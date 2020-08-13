package nexia;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class main implements BackgroundFunction<PubSubMessage> {

    @Override
    public void accept(PubSubMessage message, Context context) {
        try {
            if (FirebaseApp.getApps().size() == 0) {
                FileInputStream serviceAccount = new FileInputStream("nexiaAdmin.json");

                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://nexia-df1d0.firebaseio.com")
                        .build();

                FirebaseApp.initializeApp(options);

            }
            // make api call to MyNexia, get json response
            String responseBody = getResponseBody();
            if (responseBody != null) {
                // get compressor speed and outdoor temperature from json response body
                double speed = getCompressorSpeed(responseBody);
                double outdoorTemp = getOutdoorTemp(responseBody);
                // make sure both values are valid
                // create object to be added to list of data points stored in database
                Map<String, Object> dataToAdd = new HashMap<>();
                dataToAdd.put("percentage", speed);
                dataToAdd.put("time", Timestamp.now());
                dataToAdd.put("outdoorTemp", outdoorTemp);
                System.out.println(speed + " " + outdoorTemp);
                // add info to database
                updateDatabase(dataToAdd);

            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void updateDatabase(Map<String, Object> dataToAdd) {
        try {
            // get the document path of the correct document based on the day
            String path = getDocumentPath();
            DocumentReference docRef = FirestoreClient.getFirestore().document(path);
            DocumentSnapshot docSnap = docRef.get().get();

            List list = new ArrayList();
            // if document exists, set list to the list of data points stored in the document
            if (docSnap != null && docSnap.exists() && docSnap.getData() != null && docSnap.getData().containsKey("percentages")) {
                list = (List) docSnap.getData().get("percentages");
            }
            // add new data point to end of list
            list.add(dataToAdd);

            double averageCompressorSpeed = getAvgCompPer(list);
            double averageOutdoorTemp = getAverageOutdoorTemp(list);

            HashMap<String, Object> dataToSave = new HashMap<>();
            dataToSave.put("percentages", list);
            dataToSave.put("avgCompSpeed", averageCompressorSpeed);
            dataToSave.put("avgTemp", averageOutdoorTemp);

            // save document to database

            docRef.set(dataToSave).get();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static double getAvgCompPer(List data) {
        double sum = 0;

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> firstPoint = (Map) data.get(i);

            Object firstPerObj = firstPoint.get("percentage");
            double percentage;
            if (firstPerObj instanceof Double) {
                percentage = (double) firstPerObj;
            } else {
                percentage = (long) firstPerObj;
            }

            sum += percentage;
        }

        return sum / data.size();
    }

    private static double getAverageOutdoorTemp(List data) {
        double sum = 0;
        int size = data.size();

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> point = (Map) data.get(i);
            Object tempObj = point.get("outdoorTemp");

            double temp;
            if (tempObj instanceof Double) {
                temp = (double) tempObj;
            } else {
                temp = (long) tempObj;
            }

            if (temp != -100) {
                sum += temp;
            } else {
                size--;
            }
        }

        // make sure to not divide by 0
        if (size > 0) {
            return sum / size;
        }

        return 0;
    }

    // returns the path to the correct document based on the day. Format: data/YYYY_MM_DD
    // calculated in America/Chicago timezone
    private static String getDocumentPath() {
        LocalDateTime localTime = LocalDateTime.now(ZoneId.of("America/Chicago"));

        int year = localTime.getYear();
        int month = localTime.getMonthValue();
        int day = localTime.getDayOfMonth();

        return String.format("data/%d_%d_%d", year, month, day);
    }

    // make api call to MyNexia. return json response body as string,
    // null if error, throws error if status code is not a 200 status code
    private static String getResponseBody() {
        try {
            Connection.Response response = Jsoup.connect("https://www.mynexia.com/mobile/houses/770283")
                    .headers(getHeaders()) // store api keys
                    .ignoreContentType(true)
                    .execute();

            System.out.println("Status: " + response.statusCode());
            if (response.statusCode() > 300) {
                throw new IllegalStateException("Bad Status code: " + response.statusCode());
            }

            return response.body();
        } catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }
    }

    // returns a map that represents the request headers made in the
    // api call to MyNexia
    private static Map<String, String> getHeaders() throws Exception {
        File keyFile = new File("keys.txt");

        Scanner keyScanner = new Scanner(keyFile);
        // get api key and mobile id from file
        String mobileId = keyScanner.nextLine();
        String apiKey = keyScanner.nextLine();
        // add headers
        Map<String, String> result = new HashMap<>();
        result.put("User-Agent", "Mozilla/5.0 (Linux; Android 9; Android SDK built for x86 Build/PSR1.180720.075; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36");
        result.put("Connection", "keep-alive");
        result.put("Accept", "application/json");
        result.put("X-MobileId", mobileId);
        result.put("X-ApiKey", apiKey);
        result.put("X-AppVersion", "5.10.0");
        result.put("Accept-Encoding", "gzip, deflate");
        result.put("Accept-Language", "en-us");
        result.put("X-Requested-With", "com.schlagelink.android");

        return result;
    }

    // returns a double that represents the speed of the compressor
    // 1 is max speed, 0.0 is off
    private static double getCompressorSpeed(String body) {
        int index = body.indexOf("compressor_speed\":");
        // make sure compressor speed is included in body
        if (index != -1) {
            // instead of parsing as json, simpler to get a substring
            String snippet = body.substring(index, index + 32);
            snippet = snippet.split("}")[0];
            snippet = snippet.split(":")[1];

            return Double.parseDouble(snippet);
        }

        return -1;
    }

    // returns a double that represents the outdoor temperature
    private static double getOutdoorTemp(String body) {
        int index = body.indexOf("\"outdoor_temperature\":");
        // make sure outdoor temp is in the response body
        if (index != -1) {
            String snippet = body.substring(index, index + 32);
            snippet = snippet.split(",")[0];
            snippet = snippet.split(":")[1];
            // it is necessary to remove spaces and quotation marks
            // because the outdoor temp is stored as a string in the json response
            snippet = snippet.replaceAll(" ", "");
            snippet = snippet.replaceAll("\"", "");

            if (snippet.contains("--")) {
                return -100;
            }

            return Double.parseDouble(snippet);
        }
        // return -100 because theoretically possible that temperature
        // could get below 0.
        return -100;
    }

}
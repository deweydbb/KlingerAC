var firebaseConfig = {
    apiKey: "AIzaSyBGjWAAHiBorlpqLcpqxJVly43xET32pNY",
    authDomain: "nexia-df1d0.firebaseapp.com",
    databaseURL: "https://nexia-df1d0.firebaseio.com",
    projectId: "nexia-df1d0",
    storageBucket: "nexia-df1d0.appspot.com",
    messagingSenderId: "464865206070",
    appId: "1:464865206070:web:4dbabf29780d02d5cadd97",
    measurementId: "G-GNVQNZC3SQ"
};
// Initialize Firebase
firebase.initializeApp(firebaseConfig);
firebase.analytics();

var db = firebase.firestore();

function getDocName() {
    var url = new URL(window.location.href);

    var dateParam = url.searchParams.get("date");
    if (dateParam) {
        var split = dateParam.split("-");
        var year = parseInt(split[0]);
        var month = parseInt(split[1]);
        var day = parseInt(split[2]);

        return year + '_' + month + '_' + day;
    } else {
        var date = new Date();
        var year = date.getFullYear();
        var month = date.getMonth() + 1;
        var day = date.getDate();

        return year + '_' + month + '_' + day;
    }
}

async function getData() {
    console.log(getDocName());
    var docRef = db.collection("data").doc(getDocName());

    try {
        var snap = await docRef.get();

        if (snap.exists) {
            var result = [["Time", "Compressor Percentage", "Outdoor Temp"]];
            //console.log(snap.data());
            var percentages = snap.data().percentages;

            for (var i = 0; i < percentages.length; i++) {
                var dataPoint = percentages[i];

                var date = new Date(dataPoint.time.seconds * 1000);

                var point = [date, dataPoint.percentage * 100];
                var outdoorTemp = dataPoint.outdoorTemp;
                if (dataPoint.outdoorTemp == -100) {
                    point.push(0);
                } else {
                    point.push(dataPoint.outdoorTemp);
                }

                result.push(point);
            }

            return result;
        }
    } catch (err) {
        console.log(err);
    }

    return null;
}

function drawChart() {
    dataPromise.then(function (dataArray) {
        var data = google.visualization.arrayToDataTable(dataArray);

        document.getElementById("chart-title").innerText = 'Compressor Percentage on: ' + getDocName();

        var options = {
            legend: { position: 'bottom' },
            hAxis: { format: 'hh:mm' },
            animation: { "startup": true },
        };

        var chart = new google.visualization.LineChart(document.getElementById('curve_chart'));

        chart.draw(data, options);
    });
}

function loadPage() {
    dataPromise.then(function (dataArray) {
        var loadDiv = document.getElementById("loading_curve");

        if (dataArray) {
            loadDiv.style.display = "none";
            google.charts.load('current', { 'packages': ['corechart'] });
            google.charts.setOnLoadCallback(drawChart);
        } else {
            loadDiv.innerText = "Error: No data for specified day"
        }
    });


}

var dataPromise = getData();
loadPage();




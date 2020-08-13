# scrape-compressor-speed
This Google Cloud Function is triggered every fifteen minutes by the Google Cloud
Scheduler. It queries the current air conditioner compressor percentage and outdoor
temperature using MyNexia's http api. It calculates the current average compressor
percentage and the average outdoor temperature for the day and saves the data to
the Firestore. 

It is possible that the outdoor temperature returned by the Nexia api is "--". In
that instance, the function sets the outdoor temperature to -100 and uploads the
rest of the data regularly. The function ignores temperaturesof -100 when
calculating the average outdoor temperature for the day. 



## Deploy Function
    // switch current gcloud project to the nexia project
    gcloud config set project nexia-df1d0
    // deploy function to GCF
    gcloud functions deploy scrape-compressor-speed --runtime java11 --trigger-topic SCRAPE_SPEED --entry-point nexia.main

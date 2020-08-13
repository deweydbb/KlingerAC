# MAKE SURE YOU SWITCH PROJECTS
    gcloud config set project nexia-df1d0

    gcloud functions deploy scrape-compressor-speed --runtime java11 --trigger-topic SCRAPE_SPEED --entry-point nexia.main

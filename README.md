# Klinger AC Compressor Project
Project to store Klingler Air Conditioner compressor percentage and provide a website
to view data graphically.

[Link to website](https://nexia-df1d0.firebaseapp.com/)

I made this project in the course of a few days just as a way to keep the bordem off
during quarentine. My dad had mentioned to me that he was not happy with the graph
of the compressor usage proided by his "smart" air conditioner. Having been working
with web scraping earlier in the summer I figured I would try and build him a better
graph. I first looked to see if MyNexia had an api I could use but I could not find
one publicly avaliable. So I logged into their online web portal but for whatever
reason they did not provide the web compressor percentage on the web portal. They did
however provide the current compressor percentage on their android app. One decompiled
apk later, I figured out that it was just a web app, so they must be making http calls
to some api to get the info they are displaying. After a few failed attempts to view the
http calls made by my android phone, I found this [article](https://towardsdatascience.com/data-scraping-android-apps-b15b93aa23aa)
which worked like a charm and I was able to figure out their api. From there the rest
of the project was pretty simple. I wrote a quick java program that could run as a 
Google Cloud Function which I could have run every fifteen minutes. Then I created a 
simple little website that displayed the data graphically. The website is hosted by 
firebase and since the project is really only useful to my dad, I kept the default url.

I asked my Dad if he cared if the data was publicly accessible and he did not so this 
repo exposes a read only api key to the database. I should also mention that I really 
do not designing user interfaces and so the website is pretty simple. In addition it 
does not have a mobile view becuase my dad will primarily be accessing the website from
his desktop. 

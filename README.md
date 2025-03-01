Runtastic2FitoTrack
===

A very specific pieced together migration "script" that migrates data from Adidas Running (formerly known as Runtastic) 
to the open source app [jannis/FitoTrack](https://codeberg.org/jannis/FitoTrack).

This will very likely not work directly for you (reasons, [see bellow](#the-story)), but can at least provide a solid foundation to
adapt the code to your need.

It's pragmatic least effort code, written to be run once.
Among the worst I ever wrote.
Have fun!

# How does it work?
* It tries to load `fitotrack-backup.ftb` to merge any existing data
* It then migrates all workouts, including GPS tracks from the Sqlite file `db` and enriches it with data from `sport-activities`. 
  This includes exotic data like shoes, weather, tracking device, etc.  
  Note that exporting these from Runtastic Android App requires root access 😐️
* If a track does not contain GPS data, look in `Sport-sessions/GPS-data/*.json` files for it (the official Runtastic
  export)
* Write the result into an `out.ftb` file that you can merge into or replace your fitotrack data with

Why so complicated? It scratches a very specific itch of mine. Keep reading if you want to know the details.

# The story

A story of privacy, "the cloud", rooted phones, reverse engineering and open source.

In 2015, when I was less conscious about privacy, I started using Runtastic.  
Over the years, I became vary of being forced to accept the newest version of the terms before starting my workout.
I quickly identified [FitoTrack](https://codeberg.org/jannis/FitoTrack) as a suitable alternative, but shied away from the effort of migrating my data.  
At some point, I even requested an export of my data from Runtastic to evaluate the possibilities of a migration.
Still, my priorities lay elsewhere.

This changed suddenly when I was no longer able to log in to runtastic.
I did not change or forget the password, they just denied my login from one day to another.
They locked me out of my data and I will never know why.
The classic SaaS nightmare.

Fortunately, I still had the credentials to the email address I used for logging in to Runtastic.
So I could have requested a new password.  
But. The email provider (Starting in G, keep in mind, this whole thing started pre 2015) decided they would only let me in
if I gave them my mobile number.
I could have gotten another burner to get around it. But well. Wasn't this the final nudge I needed, to finally do the migration?

Unfortunately, requesting another export was not an option without acces to the email address.  
Fortunately, I can [root my phone on demand even though it has a locked bootloader](https://github.com/schnatterer/rooted-graphene/).
This way I was able to come into property of the 55 SQlite databases of the Runtastic app.  
My precious... data.

A quick manual analysis revealed that a lot of data was in there, the largest one being `db` at 28M.  
So I started wondering how the migration could be done in the quickest (and possibly dirtiest) way.

Thanks to FitTrack being an open source app, I was able to pick the sources that were necessary to create their export `ftb` file. 
With little effort, I could adapt the sources to run on plain Linux, as I didn't want to be slowed down by the android SDK.
With this, I could migrate the first Runtastic SQlite columns to FitoTrack class fields. 🥳

Then came the moment of shock: Where is the GPS data?  
Even though the `sqlite_sequence` for `gps` was at `423041` the table was empty 😰

I recovered quickly, finding the `gpsTrace` col among the 104 cols of the `session` table.  
Unfortunately, it is binary. Now, how to decode it?

Again, fortunately, I had my export (from 2023). There I found JSON exports of the GPS data, which turned out to have more or
less the same schema (data types and order) as the binary data.  
With that, I could migrate the GPS tracks, leading my POC to success.

Only later I found out that they must have started using the `gpsTrace` only around 2017, leaving dozens of older tracks behind. 
But of course, I wanted my data. All of it.  
Turns out there also is a column that contains a [polyline](https://developers.google.com/maps/documentation/utilities/polylinealgorithm)-encoded
version of the trace for all workouts - which would have been much eaiser to read than the binary data 🤦‍♂️.  
AFAIK polyline does not contain timestamps though, but only the plain long and lat values.  

However, thanks to reverse engineering the binary data, I already had the export JSON files open which sparked the idea to fill in the blanks using this export.
With this, I was able to complete the export of all GPS data.

In due course I had to look up other data in a different SQlite DB `sport-activities`, convert some units, (re-)calculate some values and even find a workaround to keep tracking the mileage of my shoes (not a feature of Fitotrack, yet): Each shoe is now a different custom workout type 🤯

Finally, I can go running without having to either share my data with whoever adidas sees fit or surrender my 10 years of running history. 
Free at last, thanks to free and open source software. 🚀🏃‍♂️ 

Thanks [FitoTrack contributors](https://codeberg.org/jannis/FitoTrack/activity/contributors), for your great work!

PS I also learned about [Metabolic equivalent of task (MET)](https://en.wikipedia.org/wiki/Metabolic_equivalent_of_task),
a ratio of calorie consumption per sport.
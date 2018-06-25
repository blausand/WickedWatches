README
======

Transistor - Radio App for Android
----------------------------------

**Version 3.0.x ("Oh! You Pretty Things")**

Transistor is a bare bones app for listening to radio programs over the internet. The app stores stations as files on your device's external storage. It currently understands streams encoded in MP3, AAC and Ogg/Opus(*).

Important note: This is an app of type BYOS ("bring your own station"). It does not feature any kind of built-in search option. You will have to manually add radio stations.

Transistor is free software. It is published under the [MIT open source license](https://opensource.org/licenses/MIT). Want to help? Please check out the notes in [CONTRIBUTING.md](https://github.com/y20k/transistor/blob/master/CONTRIBUTING.md) first.

Install Transistor
------------------
[<img src="https://play.google.com/intl/de_de/badges/images/generic/en_badge_web_generic.png" width="192">](https://play.google.com/store/apps/details?id=net.blausand.wickedwatch)

[<img src="https://cloud.githubusercontent.com/assets/9103935/14702535/45f6326a-07ab-11e6-9256-469c1dd51c22.png" width="192">](https://f-droid.org/repository/browse/?fdid=net.blausand.wickedwatch)

[... or get a Release APK here on GitHub](https://github.com/y20k/transistor/releases)

How to use Transistor
---------------------
### How to add a new radio station?
The easiest way to add a new station is to search for streaming links and then choose Transistor as a your default handler for those file types. You can also tap the (+) symbol in the top bar and paste in streaming links directly. Please note: Transistor does not feature any kind of built-in search option.

### How to play back a radio station?
Tap the Play button ;).

### How to stop playback?
Tap the Stop button within the app or on the notification - or just unplug your headphones.

### How to start the sleep timer?
Tapping the Clock symbol in the stations detail screen starts a 15 minute countdown after which Transistor stops playback. An additional tap adds 15 minutes to the clock. Playback must be running to be able to activate the sleep timer.

### How to place a station shortcut on the Home screen?
The option to place a shortcut for a station on the Home screen can be accessed from the station's three dots menu. A tap on a shortcut will open Transistor - playback will start immediately.

### How to rename or delete a station?
The rename and delete options can be accessed both from the station's detail screen. Just tap on the three dots symbol. You can manage the list of stations also from a file browser (see next question).

### Where does Transistor store its stations?
Transistor does not save its list of stations in a database. Instead it stores stations as M3U files on your device's external storage. Feel free to tinker with those files using the text editor of your choice. The files are stored in /Android/data/net.blausand.wickedwatch/files/Collection.

### How do I backup and transfer my radio stations?
Transistor supports Android 6's [Auto Backup](http://developer.android.com/about/versions/marshmallow/android-6.0.html#backup) feature. Radio stations are always backed up to your Google account and will be restored at reinstall. On devices running on older versions of Android you must manually save and restore the "Collection" folder.

### Why does Transistor not have any setting?
There is nothing to be set ;). Transistor is a very simple app. Depending on your point of view "simple" is either great or lame.

Which Permissions does Transistor need?
---------------------------------------
### Permission "INSTALL_SHORTCUT" and "UNINSTALL_SHORTCUT"
This permission is needed to install and uninstall radio station shortcuts on the Android Home screen.

### Permission "INTERNET"
Transistor streams radio stations over the internet.

### Permission "READ_EXTERNAL_STORAGE"
Transistor needs access to images, photos and documents to be able to customize radio station icons and to able to open locally saved playlist files.
            
### Permission "VIBRATE"
Tapping and holding a radio station will toggle a tiny vibration.

### Permission "WAKE_LOCK"
During Playback Transistor acquires a so called partial wake lock. That prevents the Android system to stop playback for power saving reasons.

(*) Opus playback is only supported on devices running Android 5.0+

Screenshots (v3.0)
---------------------
[<img src="https://user-images.githubusercontent.com/9103935/34250985-d89e79f6-e63e-11e7-9610-ff7987243841.png" width="240">](https://user-images.githubusercontent.com/9103935/34250985-d89e79f6-e63e-11e7-9610-ff7987243841.png)
[<img src="https://user-images.githubusercontent.com/9103935/34267758-5754c16c-e67e-11e7-953d-dee955850aa7.png" width="240">](https://user-images.githubusercontent.com/9103935/34267758-5754c16c-e67e-11e7-953d-dee955850aa7.png)
[<img src="https://user-images.githubusercontent.com/9103935/34267759-576f6b84-e67e-11e7-883f-3f6acfedea5b.png" width="240">](https://user-images.githubusercontent.com/9103935/34267759-576f6b84-e67e-11e7-883f-3f6acfedea5b.png)

[<img src="https://user-images.githubusercontent.com/9103935/34267760-578a3086-e67e-11e7-8cce-98ca4a238be5.png" width="240">](https://user-images.githubusercontent.com/9103935/34267760-578a3086-e67e-11e7-8cce-98ca4a238be5.png)
[<img src="https://user-images.githubusercontent.com/9103935/34250989-d9048a16-e63e-11e7-886d-419ae55de0eb.png" width="240">](https://user-images.githubusercontent.com/9103935/34250989-d9048a16-e63e-11e7-886d-419ae55de0eb.png)
[<img src="https://user-images.githubusercontent.com/9103935/34257749-68c2f270-e65b-11e7-97be-815fca8d6529.png" width="240">](https://user-images.githubusercontent.com/9103935/34257749-68c2f270-e65b-11e7-97be-815fca8d6529.png)

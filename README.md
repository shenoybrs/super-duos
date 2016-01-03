# super-duos
Both the projects alexandria and Football_Scores are in the same repo.

1) Added a bar code scanner as a 3rd party open source lib viz  , also used GLide to fetch the images of the books in alexandria. following are the references
    compile 'com.github.bumptech.glide:glide:3.6.1'
    compile 'com.journeyapps:zxing-android-embedded:3.0.2@aar'
    compile 'com.google.zxing:core:3.2.0'
2) Got one crash in geny motion in the tablet mode when the screen is rotated , when i analyzed the crash it looks to be in 
at android.support.v7.internal.widget.ToolbarWidgetWrapper.<init>(ToolbarWidgetWrapper.java:100) a null pointer exception .
3) added widgets in the football-score app 
4) please add TheAPIKey=your_api_key in gradle.property for the football_scrore to work.



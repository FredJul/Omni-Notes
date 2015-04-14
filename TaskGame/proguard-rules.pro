-renamesourcefileattribute SourceFile    
-keepattributes SourceFile,LineNumberTable
-keepnames class *

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class android.support.v7.widget.SearchView { *; }

# for picasso
-dontwarn com.squareup.okhttp.**

# for TaskGame
-keep class net.fred.taskgame.model.SyncData { *; }
-keep class * extends com.raizlabs.android.dbflow.structure.BaseModel { *; }

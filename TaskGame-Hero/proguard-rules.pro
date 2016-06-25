# This is an opensource project, let's keep the filename and line numbers
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepnames class *

#DBFlow
-keep class * extends com.raizlabs.android.dbflow.** { *; }
-keep class com.raizlabs.android.dbflow.** { *; }

# Parceler
-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }
-keep class org.parceler.Parceler$$Parcels
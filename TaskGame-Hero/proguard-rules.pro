# This is an opensource project, let's keep the filename and line numbers
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepnames class *

# Parceler
-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }
-keep class org.parceler.Parceler$$Parcels

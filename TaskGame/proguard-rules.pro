# This is an opensource project, let's keep the filename and line numbers
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable, Signature
-keepnames class *

# Support v7
# https://code.google.com/p/android/issues/detail?id=58508
-keep class android.support.v7.widget.SearchView { *; }

# Parceler
-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }
-keep class org.parceler.Parceler$$Parcels

# ZXing relies on dynamic class lookup for some writers/readers.
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlinx Serialization generated serializers.
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.juanmediahubph.qrstudio.**$$serializer { *; }
-keepclassmembers class com.juanmediahubph.qrstudio.** {
    *** Companion;
}

# Room and Hilt ship their own consumer rules. Keep schema metadata for diagnostics.
-keep class com.juanmediahubph.qrstudio.data.db.** { *; }

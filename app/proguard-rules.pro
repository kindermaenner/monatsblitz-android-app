# Project-specific ProGuard/R8 rules for Monatsblitz
#
# For more details, see:
# https://developer.android.com/studio/build/shrink-code

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses,EnclosingMethod

# Keep serializable classes and their serializers
-keepclassmembers class * {
    *** Companion;
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Retrofit & OkHttp
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Room
-dontwarn androidx.room.paging.**

# Google Play Services Code Scanner (ML Kit)
-keep class com.google.android.gms.internal.mlkit_code_scanner.** { *; }
-keep class com.google.mlkit.vision.codescanner.** { *; }
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.common.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_code_scanner.**
-dontwarn com.google.mlkit.vision.codescanner.**


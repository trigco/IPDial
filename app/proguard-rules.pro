# Keep PJSIP JNI bridge
-keep class org.pjsip.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep our SIP service
-keep class com.ipdial.service.** { *; }
-keep class com.ipdial.data.model.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Compose
-dontwarn androidx.compose.**

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for readable crash stack traces (Crashlytics)
-keepattributes SourceFile,LineNumberTable

# ---------- Gson ----------
# Keep generic type info Gson needs for TypeToken / parameterized responses
-keepattributes Signature
-keepattributes *Annotation*

# Keep every model/DTO ("...Item") field and its @SerializedName so Gson
# can still deserialize after obfuscation. Covers retrofit/model,
# transactions/model, and every per-module model package.
-keep class com.shreefintech.paytouchconsumer.**.*Item {
    <fields>;
}
-keepclassmembers class com.shreefintech.paytouchconsumer.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# ---------- Retrofit / OkHttp ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ---------- Project enums (Constant.kt / enums package) ----------
-keepclassmembers enum com.shreefintech.paytouchconsumer.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Play Core (in-app update) ----------
-keep class com.google.android.play.core.appupdate.** { *; }
-keep class com.google.android.play.core.install.** { *; }
-dontwarn com.google.android.play.core.**

# ---------- Glide ----------
-keep class com.bumptech.glide.** { *; }
-keep class * extends com.bumptech.glide.module.AppGlideModule
-dontwarn com.bumptech.glide.**

# ---------- ViewModels (constructed via reflection by ViewModelProvider) ----------
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ---------- General Kotlin metadata ----------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

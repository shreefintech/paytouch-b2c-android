# Gson — keep all fields annotated with @SerializedName (covers all API DTOs)
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all model classes — API DTOs (retrofit/model/**) and local DTOs passed between
# Activities as Gson JSON (operator/model, transactions/model, loadwallet/model, …)
-keep class com.shreefintech.paytouchconsumer.**.model.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep Retrofit service interfaces so their method signatures survive R8
-keep interface com.shreefintech.paytouchconsumer.retrofit.** { *; }

# OkHttp (platform and internal classes)
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

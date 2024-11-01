# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#Google Play Services Ads
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.ads.** { *; }
-dontwarn com.google.ads.**

# Facebook Shimmer
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

#Android SpinKit
-keep class com.github.ybq.** { *; }
-dontwarn com.github.ybq.**

#AppLovin, AdColony
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**
-keep class com.adcolony.** { *; }
-dontwarn com.adcolony.**

# AndroidX Android
-keep class androidx.** { *; }
-dontwarn androidx.**

# Lifecycle Process
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Messaging Platform
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

# ConstraintLayout
-keep class androidx.constraintlayout.** { *; }
-dontwarn androidx.constraintlayout.**

# method native (JNI)
-keepclasseswithmembers class * {
    native <methods>;
}

-keep class com.ads.ads_module.** { *; }

-keep class com.yandex.metrica.** { *; }
-dontwarn com.yandex.metrica.**

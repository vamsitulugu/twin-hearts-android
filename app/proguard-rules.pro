# Release shrinking rules. Retrofit, OkHttp, Hilt, Room and kotlinx.serialization ship their own consumer rules.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, RuntimeVisibleAnnotations
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# WebRTC talks to its native code by name
-keep class org.webrtc.** { *; }

# Razorpay checkout (payments)
-keepattributes JavascriptInterface
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-optimizations !method/inlining/*
-keepclasseswithmembers class * { public void onPayment*(...); }

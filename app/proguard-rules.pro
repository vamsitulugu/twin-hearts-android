# Release shrinking rules. Retrofit, OkHttp, Hilt, Room and kotlinx.serialization ship their own consumer rules.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, RuntimeVisibleAnnotations
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

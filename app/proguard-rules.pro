# Proguard rules for Freezing Apps
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class com.freezingapps.app.data.model.** { *; }
-keep class com.freezingapps.app.data.db.** { *; }

# Keep line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# AccessibilityService & foreground monitoring
-keep class com.example.monitoring.accessibility.AegisAccessibilityService { *; }
-keep class com.example.monitoring.accessibility.AegisAccessibilityService$Companion { *; }

# Isolated Time-Based Blocker & Companion Connector
-keep class com.example.timeblocker.** { *; }

# Activities, Services, BroadcastReceivers, DeviceAdminReceiver, AppWidgetProvider
-keep class com.example.MainActivity { *; }
-keep class com.example.ui.browser.BrowserActivity { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }

# Room Database, Entities, DAOs, and generated _Impl classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.data.local.** { *; }

# Core models and preferences
-keep class com.example.core.models.** { *; }
-keep class com.example.data.preferences.** { *; }

# Preserve enum values/valueOf for reflection/serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep WebView JavascriptInterfaces if any
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# TensorFlow Lite & OkHttp / Retrofit optional platform classes
-dontwarn org.tensorflow.lite.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okhttp3.internal.platform.**
-dontwarn okio.**

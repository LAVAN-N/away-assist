# ProGuard rules for Away Assist

# Keep Compose runtime & animations
-keepclassmembers class * extends androidx.compose.runtime.State { *; }

# Keep DataStore Preferences enum serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Android Service, BroadcastReceiver, and AppWidget entry points
-keep public class com.awayassist.app.service.RingerService
-keep public class com.awayassist.app.service.ScreenStateReceiver
-keep public class com.awayassist.app.service.SmsCommandReceiver
-keep public class com.awayassist.app.service.SimStateReceiver
-keep public class com.awayassist.app.service.ShutdownReceiver
-keep public class com.awayassist.app.service.BootReceiver
-keep public class com.awayassist.app.widget.AwayAssistAppWidgetProvider

# Keep Shizuku IPC classes
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }


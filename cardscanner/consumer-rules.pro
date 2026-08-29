############################################
# Card Scanner consumer rules
############################################

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# Public SDK API
-keep public class com.cardscanner.CardScanner {
    public *;
}

-keep public class com.cardscanner.CardScannerResult {
    public *;
}

-keep public class com.cardscanner.config.** {
    public *;
}

# Scanner Activity
-keep public class com.cardscanner.** extends android.app.Activity {
    public <init>();
}

-keep public class com.cardscanner.** extends androidx.activity.ComponentActivity {
    public <init>();
}

# Hilt ViewModels used internally
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * {
    <init>(...);
}

# Serializable scanner configuration
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
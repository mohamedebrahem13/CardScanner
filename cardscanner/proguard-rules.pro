############################################
# Card Scanner - ProGuard / R8 rules
############################################

# ==========================================
# Kotlin / annotations / generics
# ==========================================

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep runtime-visible annotations
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault


# ==========================================
# PUBLIC CARD SCANNER API
# ==========================================

# Main scanner API
-keep public class com.cardscanner.CardScanner {
    public *;
}

# Scanner result
-keep public class com.cardscanner.CardScannerResult {
    public *;
}

# Configuration API
-keep public class com.cardscanner.config.** {
    public *;
}


# ==========================================
# ANDROID COMPONENTS
# ==========================================

# If your scanner has a dedicated Activity,
# keep the Activity class.
#
# Change the class name if yours is different.

-keep public class com.cardscanner.** extends android.app.Activity {
    public <init>();
}

-keep public class com.cardscanner.** extends androidx.activity.ComponentActivity {
    public <init>();
}


# ==========================================
# HILT / DAGGER
# ==========================================

# Hilt-generated components generally ship
# their own rules, but preserve generated
# metadata and injected constructors.

-keep,allowoptimization,allowobfuscation class dagger.hilt.** { *; }
-keep,allowoptimization,allowobfuscation class javax.inject.** { *; }

# Keep Hilt generated component holders
-keep class * implements dagger.hilt.internal.GeneratedComponent {
    *;
}

-keep class * implements dagger.hilt.internal.GeneratedComponentManager {
    *;
}

# Keep generated Hilt classes
-keep class **_HiltComponents* { *; }
-keep class Hilt_* { *; }

# Keep @HiltViewModel classes
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * {
    <init>(...);
}


# ==========================================
# VIEWMODEL
# ==========================================

# Keep ViewModel constructors where needed
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}


# ==========================================
# CAMERA X
# ==========================================

# CameraX already includes consumer rules.
# These are conservative guards for reflection
# and implementation discovery.

-keep class androidx.camera.** { *; }

-dontwarn androidx.camera.**


# ==========================================
# ML KIT
# ==========================================

# ML Kit normally ships its own keep rules.
# Keep public ML Kit vision APIs used by scanner.

-keep class com.google.mlkit.vision.** { *; }

-dontwarn com.google.mlkit.**


# ==========================================
# GOOGLE PLAY SERVICES
# ==========================================

-dontwarn com.google.android.gms.**


# ==========================================
# COROUTINES
# ==========================================

# Coroutines generally work correctly with R8.
# Suppress optional debug/internal warnings.

-dontwarn kotlinx.coroutines.**


# ==========================================
# COMPOSE
# ==========================================

# Compose ships R8 rules itself.
# Preserve composable annotations/metadata.

-keepattributes *Annotation*

-dontwarn androidx.compose.**


# ==========================================
# SERIALIZABLE CONFIG
# ==========================================

# Your CardScannerConfig implements Serializable.
# Keep serialVersionUID if present.

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

# Keep scanner config fields because the config may
# cross Activity boundaries using Serializable.
-keep class com.cardscanner.config.** {
    <fields>;
    <methods>;
}


# ==========================================
# ENUMS
# ==========================================

# Preserve enum valueOf()/values()
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# ==========================================
# PARCELABLE - if added later
# ==========================================

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}


# ==========================================
# JNI - safety if a dependency uses native code
# ==========================================

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}


# ==========================================
# DO NOT KEEP INTERNAL IMPLEMENTATION
# ==========================================

# Intentionally no broad:
#
# -keep class com.cardscanner.** { *; }
#
# That would prevent R8 from optimizing your library.
#
# Classes such as:
#
# CardOcrParser
# CardScannerViewModel
# internal UI helpers
#
# may be optimized/obfuscated.
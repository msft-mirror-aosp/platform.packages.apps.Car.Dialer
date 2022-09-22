-verbose
# Use ProGuard only to get rid of unused classes
-dontobfuscate
-dontoptimize
-keepattributes *

# Preverification was introduced in Java 6 to enable faster classloading, but
# dex doesn't use the java .class format, so it has no benefit and can cause
# problems.
-dontpreverify

# Skipping analysis of some classes may make proguard strip something that's
# needed.
-dontskipnonpubliclibraryclasses

# Parcel reflectively accesses this field.
-keepclassmembers class * implements android.os.Parcelable {
  public static *** CREATOR;
}

# Don't warn about Nullable and NonNull annotations
-dontwarn org.jetbrains.annotations.*

# Unexpected reference to missing service class: META-INF/services/javax.annotation.processing.Processor.
-dontwarn javax.annotation.processing.Processor

# AbstractMethodError
-keep public interface android.car.Car$CarServiceLifecycleListener { *; }
-keep public interface android.car.CarProjectionManager$ProjectionStatusListener { *; }

# NPE where void onRestrictionsChanged(@NonNull CarUxRestrictions carUxRestrictions) passes null
-keep class android.car.drivingstate.** { *; }

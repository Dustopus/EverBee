# ============================================================
# ApisLens ProGuard Rules
# ============================================================

# ── Kotlin ──
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ── Hilt ──
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.android.AndroidEntryPoint class * { *; }

# ── Gson ──
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.apislens.data.** { *; }
-keep class com.apislens.data.model.** { *; }
-keep class com.apislens.data.local.entity.** { *; }
# Keep generic signatures for TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
# Keep @SerializedName fields
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Glide ──
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ── MPAndroidChart ──
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ── WorkManager ──
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class androidx.work.impl.** { *; }

# ── Navigation SafeArgs ──
-keep class * extends androidx.navigation.Navigator

# ── AndroidX / Material ──
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

# ── Keep entity constructors for Room + Gson ──
-keepclassmembers class com.apislens.data.local.entity.Device {
    <init>(...);
}
-keepclassmembers class com.apislens.data.local.entity.ChargeRecord {
    <init>(...);
}
-keepclassmembers class com.apislens.data.local.entity.UsageRecord {
    <init>(...);
}
-keepclassmembers class com.apislens.data.model.ExportData {
    <init>(...);
}

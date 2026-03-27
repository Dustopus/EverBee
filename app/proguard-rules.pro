# ── ApisLens ProGuard Rules ──

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ── Hilt ──
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepattributes *Annotation*

# ── Gson ──
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.apislens.data.model.** { *; }
-keep class com.apislens.data.local.entity.** { *; }
-keep class com.apislens.data.utils.ExportResult { *; }
-keep class com.apislens.data.utils.ImportResult { *; }

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

# ── Kotlin Coroutines ─>
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Navigation SafeArgs ──
-keep class * extends androidx.navigation.Navigator

# ── General Android ──
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }

# AriSam Tunes release shrinker rules.
#
# Keep metadata used by Kotlin serialization, Room, Hilt, Compose previews and
# reflection-light integrations while still allowing R8 to shrink application code.

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Kotlin serialization DTO serializers are referenced generated-code-first, but
# Ktor content negotiation can still need serializers and companion objects after
# obfuscation.
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class **$Companion { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.arisamtunes.data.**.*Dto { *; }
-keep class com.arisamtunes.data.**.*Request { *; }
-keep class com.arisamtunes.data.**.*Response { *; }

# Ktor, OkHttp and Okio are used for REST, auth refresh and WebSockets.
-keep class io.ktor.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Room database schema, entities, DAOs and type converters.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class com.arisamtunes.data.local.** { *; }

# Hilt/Dagger generated entry points.
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**

# Media3 playback service/session and cache components.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil image loading and generated/network integrations.
-keep class coil3.** { *; }
-dontwarn coil3.**

# WorkManager creates workers reflectively when restoring scheduled downloads.
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.work.impl.** { *; }
-dontwarn androidx.work.**

# Compose tooling/debug artifacts should never block release shrinking.
-dontwarn androidx.compose.ui.tooling.**

# The release build is the first time R8 runs over this code, so the classes
# something else instantiates by reflection are spelled out here rather than
# left to consumer rules alone.

# WorkManager builds a Worker by name through its default factory. Without
# this the enrichment job silently never runs in release — the snip still
# saves, so nothing looks broken, which is exactly what makes it worth
# keeping explicitly.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Room's generated implementation looks up the entity's constructor.
-keep class app.snips.data.Snip { *; }

# Jsoup reaches for its parser implementations by name.
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# OkHttp names optional platform classes it will happily do without.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep line numbers so a crash report from a friend is worth reading.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Jsoup
-keeppackagenames org.jsoup.nodes

# WorkManager instantiates the worker; keep it and its assisted-inject ctor.
-keep class com.sitewatch.app.work.SiteCheckWorker { *; }

# MonitorType is persisted to Room by its enum name; keep the constant names
# stable so stored rows still resolve after R8.
-keepclassmembers enum com.sitewatch.app.data.local.MonitorType {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    *;
}

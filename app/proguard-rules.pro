# 予你 B0 阶段混淆规则（release 当前未开启 minify，此文件为提前准备）
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
# OkHttp 平台识别相关（避免 R8 在无对应类时告警）
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Room 生成的实现类
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class *

# WorkManager 通过反射实例化 Worker
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# 数据模型（后续用 JSON 反射时保留字段名）
-keepclassmembers class com.mulin.us.** { *; }

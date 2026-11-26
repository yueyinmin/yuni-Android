// ============================================================
// 予你 · Us —— 根目录构建脚本
// 这里只声明"整个工程要用哪些插件"，不给版本号；
// 版本号统一写在 gradle/libs.versions.toml。
// 与轻记工程完全独立：不同目录、不同 applicationId（com.mulin.us）。
// ============================================================
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

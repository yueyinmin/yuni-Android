// ============================================================
// app 模块构建脚本
// 和轻记工程完全独立：applicationId 不同、数据库名不同、包名不同。
// ============================================================
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ------------------------------------------------------------
// 读取本机 local.properties 里的 CloudBase 配置（可全部留空）
// 留空 = 不连云端，App 走本地替身实现，0 网络请求、0 费用。
// 只读客户端可公开的字段，绝不读任何高权限密钥。
// ------------------------------------------------------------
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}

fun esc(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

val cloudEnvId: String = localProps.getProperty("yuni.envId").orEmpty().trim()
val cloudClientId: String = localProps.getProperty("yuni.clientId").orEmpty().trim()
val cloudClientSecret: String = localProps.getProperty("yuni.clientSecret").orEmpty().trim()
// ECS 跳板端点（三个月临时）：留空则不启用。例：http://47.114.58.55:8080
val ecsBaseUrl: String = localProps.getProperty("yuni.ecsBaseUrl").orEmpty().trim()

android {
    namespace = "com.mulin.us"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mulin.us"        // 与轻记 com.qingji.app 完全不同，可同机共存
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-b0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 云端配置：默认空字符串。App 侧 AppConfig.isCloudConfigured 判断后才启用网络层。
        buildConfigField("String", "CLOUD_ENV_ID", "\"${esc(cloudEnvId)}\"")
        buildConfigField("String", "CLOUD_CLIENT_ID", "\"${esc(cloudClientId)}\"")
        buildConfigField("String", "CLOUD_CLIENT_SECRET", "\"${esc(cloudClientSecret)}\"")
        buildConfigField("String", "CLOUD_REGION", "\"ap-shanghai\"")
        // B0 阶段硬开关：只有显式把 yuni.envId 填上才会变 true
        buildConfigField("boolean", "CLOUD_ENABLED", cloudEnvId.isNotEmpty().toString())
        buildConfigField("String", "ECS_BASE_URL", "\"${esc(ecsBaseUrl)}\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false          // B0 阶段不混淆，先把构建链路跑通
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // B0 阶段仅用于验证「release 能否构建」，用的是 debug 签名。
            // 正式发布前必须换成自己的正式密钥，并把这里改掉。
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }

    // schema json 放在 app/schemas，由 Room 导出并随 Git 提交。
    // 迁移测试直接读这个文件自己建 v1 库（AGP 8.7 不会把 test.assets 合进单测资源），
    // 这里保留 srcDir 只为让 IDE 把 schemas 识别为测试资源目录。
    sourceSets {
        getByName("test") {
            assets.srcDir(files("$projectDir/schemas"))
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    // 必须导出：v1/v2 schema 是迁移测试的基线，且随 Git 提交做版本留痕
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.mirage)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

package com.mulin.us.core

import com.mulin.us.BuildConfig

/**
 * 云端配置的唯一入口。
 *
 * 设计原则（对应工单"云端可替换"）：
 * 1. 业务代码永远不直接读 BuildConfig，只读 AppConfig；
 * 2. 没配置环境 ID 时，AppConfig.cloudEnabled = false，整个网络层不会被创建；
 * 3. 这里只有"客户端可公开"的值，service_role / 管理员密钥绝不出现。
 */
object AppConfig {

    /** 环境 ID，例如 us-free-dev-8gxxxxx；空字符串表示未配置 */
    val envId: String get() = BuildConfig.CLOUD_ENV_ID

    val clientId: String get() = BuildConfig.CLOUD_CLIENT_ID

    val clientSecret: String get() = BuildConfig.CLOUD_CLIENT_SECRET

    val region: String get() = BuildConfig.CLOUD_REGION

    /**
     * 云端总开关。
     * 空配置 → false → App 走本地替身实现：0 网络请求、0 云资源点消耗、0 费用。
     */
    val cloudEnabled: Boolean get() = BuildConfig.CLOUD_ENABLED

    /**
     * 是否具备用「用户名+密码」登录的条件。
     * 没有 Client Secret 也能登录（CloudBase 默认用环境 ID 当 client_id），
     * 但必须在控制台把该应用配置为允许客户端登录后才会成功。
     */
    val canAttemptLogin: Boolean get() = cloudEnabled

    val pgRestBaseUrl: String
        get() = "https://$envId.api.tcloudbasegateway.com/v1/rdb/rest"

    val authBaseUrl: String
        get() = "https://$envId.api.tcloudbasegateway.com/auth/v1"

    val functionBaseUrl: String
        get() = "https://$envId.api.tcloudbasegateway.com/v1/functions"

    val isDebugBuild: Boolean get() = BuildConfig.DEBUG

    /** 给人看的一句话状态，UI 直接用 */
    fun describeCloudState(): String = when {
        !cloudEnabled -> "未配置（本地模式，不联网）"
        else -> "已配置环境 $envId（未验证）"
    }
}

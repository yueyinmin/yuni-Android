package com.mulin.us.data.remote

/**
 * 远端调用结果。
 *
 * 为什么不用 Kotlin 的 Result：
 * 网络层需要区分"没网""没登录""没权限""版本冲突""服务未配置"这几种完全不同的处理方式，
 * 用一个 Throwable 包住会让调用方到处写 when(exception::class) —— 迟早漏掉一种。
 */
sealed interface RemoteResult<out T> {

    data class Success<T>(val data: T) : RemoteResult<T>

    /** 云端根本没配置（B0 默认就是这种状态）：不是错误，是"还没接" */
    data class NotConfigured(val message: String) : RemoteResult<Nothing>

    /** 网络不可达、超时 */
    data class Network(val message: String) : RemoteResult<Nothing>

    /** 401：token 无效或过期，需要重新登录/刷新 */
    data class Unauthorized(val message: String) : RemoteResult<Nothing>

    /** 403：身份没问题，但 RLS/规则不允许——通常是越权尝试，不重试 */
    data class Forbidden(val message: String) : RemoteResult<Nothing>

    /** 409：版本冲突，交给 ConflictResolver */
    data class Conflict(val message: String, val serverRow: String? = null) : RemoteResult<Nothing>

    /** 4xx 参数/请求问题 */
    data class BadRequest(val code: String?, val message: String) : RemoteResult<Nothing>

    /** 5xx 或未知 */
    data class Server(val code: String?, val message: String) : RemoteResult<Nothing>

    val isSuccess: Boolean get() = this is Success

    fun <R> map(transform: (T) -> R): RemoteResult<R> = when (this) {
        is Success -> Success(transform(data))
        is NotConfigured -> this
        is Network -> this
        is Unauthorized -> this
        is Forbidden -> this
        is Conflict -> this
        is BadRequest -> this
        is Server -> this
    }

    companion object {
        fun <T> failureOf(message: String): RemoteResult<T> = Network(message)
    }
}

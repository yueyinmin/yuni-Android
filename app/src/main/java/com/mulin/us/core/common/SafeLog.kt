package com.mulin.us.core.common

import android.util.Log

/**
 * 安全日志。
 *
 * 为什么不用 android.util.Log 直接打：
 * 予你的日记、悄悄话、信件正文都属私密内容，一旦进了 logcat，
 * 任何有 adb 权限的电脑（比如连着电脑充电）都能读出来。
 *
 * 所以这里只允许打「元信息」：谁、什么操作、多长、什么结果。
 * 想打正文？SafeLog 里没有这个方法 —— 不是忘了写，是故意不提供。
 */
object SafeLog {

    private const val TAG_PREFIX = "Yuni"

    fun d(scope: String, message: String) {
        Log.d(TAG_PREFIX + "/" + scope, message)
    }

    fun i(scope: String, message: String) {
        Log.i(TAG_PREFIX + "/" + scope, message)
    }

    fun w(scope: String, message: String) {
        Log.w(TAG_PREFIX + "/" + scope, message)
    }

    fun e(scope: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG_PREFIX + "/" + scope, message, throwable)
    }

    /** 只输出"有多少个字符"，绝不输出内容本身。 */
    fun eLength(scope: String, what: String, textLength: Int) {
        Log.d(TAG_PREFIX + "/" + scope, "$what.length=$textLength")
    }

    /**
     * 对敏感标识（userId / token / coupleId）做脱敏，只留头尾各 4 位。
     * 用于排查"是不是同一个账号"这类问题，又不至于把完整值留在日志里。
     */
    fun mask(value: String?): String {
        if (value.isNullOrEmpty()) return "<empty>"
        if (value.length <= 12) return "***"
        return value.take(4) + "***" + value.takeLast(4)
    }
}

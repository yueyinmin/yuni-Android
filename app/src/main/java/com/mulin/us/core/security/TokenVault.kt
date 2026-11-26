package com.mulin.us.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.mulin.us.core.common.SafeLog
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Token 保管箱。
 *
 * 三件事必须做到（工单硬要求）：
 * 1. Token 不写普通日志 —— 本类不给任何"打印 token"的方法；
 * 2. Token 不硬编码进源码 —— 只存在运行时生成的文件里；
 * 3. Token 落地加密 —— 用 Android Keystore 里的 AES 密钥加密后再写 SharedPreferences。
 *
 * 为什么不用 EncryptedSharedPreferences（androidx.security）：
 * 那个库在本机依赖缓存里没有，且已被官方标记为 deprecated；
 * 直接调 Keystore 只有 60 行，反而更可控、可审计。
 *
 * 密钥特性：密钥材料存在系统 Keystore（TEE/StrongBox 里，如果设备支持），
 * 应用只能"用"不能"读"，root 之后把文件拷走也解不开。
 */
class TokenVault(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("yuni_tokens", Context.MODE_PRIVATE)

    fun save(accessToken: String, refreshToken: String?, userId: String) {
        prefs.edit()
            .putString(KEY_ACCESS, encrypt(accessToken))
            .putString(KEY_REFRESH, refreshToken?.let { encrypt(it) })
            .putString(KEY_USER, userId)
            .apply()
        SafeLog.d(TAG, "token saved for user=${SafeLog.mask(userId)}")
    }

    fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)?.let { decrypt(it) }

    fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)?.let { decrypt(it) }

    fun userId(): String? = prefs.getString(KEY_USER, null)

    /** 当前双人空间 ID：持久化，App 重启后同步无需再发 /me（避免嵌套请求） */
    fun coupleId(): String? = prefs.getString(KEY_COUPLE, null)

    fun saveCoupleId(id: String?) {
        prefs.edit().putString(KEY_COUPLE, id).apply()
    }

    fun hasSession(): Boolean = accessToken() != null

    /** 退出登录只清这里，绝不动数据库（工单：退出登录不能删本地日记） */
    fun clear() {
        prefs.edit().clear().apply()
        SafeLog.d(TAG, "token cleared")
    }

    // ---------------- 加解密 ----------------

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // 不要求用户解锁才能用：后台同步要在锁屏时也能跑
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        // 格式：base64(iv) + ":" + base64(ciphertext)
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(body, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? = runCatching {
        val parts = stored.split(":")
        if (parts.size != 2) return null
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val body = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(body), Charsets.UTF_8)
    }.onFailure {
        // 密钥被清（换机、恢复出厂、清除数据）时会走到这里：当作没登录，不崩
        SafeLog.w(TAG, "decrypt failed, treat as logged out")
        prefs.edit().clear().apply()
    }.getOrNull()

    private companion object {
        const val TAG = "TokenVault"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "yuni_token_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_USER = "uid"
        const val KEY_COUPLE = "couple"
    }
}

package com.mulin.us.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 本机头像存储：相册选图 → 压缩 → 私有目录落盘。
 *
 * 为什么不直接用云端图片：
 * - 头像属于本机档案，没登录也要能换；
 * - 不申请存储权限，文件只在 App 私有目录；
 * - 当前 ECS 跳板没有/app 头像接口，先只做本机持久化，避免假装同步成功。
 */
class AvatarStore(private val context: Context) {

    /** 压缩到最长边 512px、JPEG 85，头像够用且不会占空间 */
    fun save(uri: Uri, key: String): Result<String> = runCatching {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("读不到这张图片")
        val original = input.use { BitmapFactory.decodeStream(it) }
            ?: error("图片格式不支持")
        val ratio = minOf(1f, 512f / maxOf(original.width, original.height))
        val width = (original.width * ratio).toInt().coerceAtLeast(1)
        val height = (original.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(original, width, height, true)
        val bytes = ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
        scaled.recycle()
        original.recycle()

        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val file = File(dir, "$key.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }
}

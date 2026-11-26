package com.mulin.us.data.remote.ecs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.mulin.us.data.remote.RemoteResult
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * S4b 图片：本地压缩 + 私有云存储。
 *
 * 服务端不提供公开永久链接，只返回 5 分钟签名 URL —— 客户端改时间也拿不到。
 * 本地私有目录存一份副本，断网也能看（对应"本地优先"底线）。
 */
class ImageStore(private val context: Context, private val gate: EcsGate) {

    /** 选图后压缩到最长边 1600px、JPEG 80，控制单张 < 1MB */
    fun compress(uri: Uri, maxDimension: Int = 1600, quality: Int = 80): ByteArray? {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(stream)
        stream.close()
        if (original == null) return null
        val ratio = minOf(1f, maxDimension.toFloat() / maxOf(original.width, original.height))
        val w = (original.width * ratio).toInt().coerceAtLeast(1)
        val h = (original.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(original, w, h, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        scaled.recycle(); original.recycle()
        return out.toByteArray()
    }

    /** 存本地私有目录（不申请存储权限） */
    fun saveLocal(bytes: ByteArray, name: String): String {
        val dir = java.io.File(context.filesDir, "images").apply { mkdirs() }
        val f = java.io.File(dir, name)
        f.writeBytes(bytes)
        return f.absolutePath
    }

    suspend fun upload(memoryCloudId: String, bytes: ByteArray): RemoteResult<JSONObject> {
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return gate.post("/image/upload", JSONObject()
            .put("memoryId", memoryCloudId)
            .put("fileName", "photo.jpg")
            .put("base64", b64))
    }

    suspend fun signedUrl(fileId: String): RemoteResult<String> = when (
        val r = gate.get("/image/url?fileId=$fileId")
    ) {
        is RemoteResult.Success -> RemoteResult.Success(r.data.optString("url"))
        else -> r as RemoteResult<String>
    }

    fun fullUrl(path: String): String {
        val base = gate.baseUrlForImage()
        return if (path.startsWith("http")) path else base.trimEnd('/') + path
    }
}

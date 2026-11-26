package com.mulin.us.data.repository.impl

import android.net.Uri
import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.ecs.ImageStore
import com.mulin.us.domain.model.MemoryPhoto
import com.mulin.us.domain.repository.MemoryPhotoRepository

/**
 * 图片仓储（4.8/4.9）：
 * 本地先落一份（断网可看），再上传云端拿签名 URL。
 * 上传失败不删本地副本 —— 本地优先，不能因为云端挂了就丢照片。
 */
class ImageRepositoryImpl(private val store: ImageStore) : MemoryPhotoRepository {
    override suspend fun add(memoryCloudId: String, uri: Uri): Result<MemoryPhoto> {
        val bytes = store.compress(uri)
            ?: return Result.failure(IllegalStateException("读不到这张图片"))
        val localPath = store.saveLocal(bytes, "img-${System.currentTimeMillis()}.jpg")
        val uploaded = store.upload(memoryCloudId, bytes)
        val fileId = when (uploaded) {
            is RemoteResult.Success -> uploaded.data.optString("fileId").ifBlank { null }
            else -> null
        }
        return Result.success(MemoryPhoto(
            memoryCloudId = memoryCloudId,
            localPath = localPath,
            cloudFileId = fileId,
            byteSize = bytes.size.toLong(),
            synced = fileId != null
        ))
    }
}

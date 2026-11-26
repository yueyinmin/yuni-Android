package com.mulin.us.data.repository.impl

import android.net.Uri
import com.mulin.us.data.local.dao.MemoryPhotoDao
import com.mulin.us.data.local.entity.MemoryPhotoEntity
import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.ecs.ImageStore
import com.mulin.us.domain.model.MemoryPhoto
import com.mulin.us.domain.repository.MemoryPhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 图片仓储（4.8/4.9 + 持久化）：
 * 1. 压缩 → 存本地私有目录（断网可看）
 * 2. 上传云端拿 fileId
 * 3. **写 Room memory_photo 表**：重启 App 后 UI 还能显示，fileId 不丢
 *
 * 上传失败也写 Room（cloudFileId=null），之后可补传 —— 本地优先，不丢照片。
 */
class ImageRepositoryImpl(
    private val store: ImageStore,
    private val photoDao: MemoryPhotoDao
) : MemoryPhotoRepository {

    override suspend fun add(memoryLocalId: Long, memoryCloudId: String, uri: Uri): Result<MemoryPhoto> {
        val bytes = store.compress(uri)
            ?: return Result.failure(IllegalStateException("读不到这张图片"))
        val localPath = store.saveLocal(bytes, "img-${System.currentTimeMillis()}.jpg")

        val uploaded = store.upload(memoryCloudId, bytes)
        val fileId = when (uploaded) {
            is RemoteResult.Success -> uploaded.data.optString("fileId").ifBlank { null }
            else -> null
        }

        // 写 Room：这是持久化的关键，重启后 UI 从这里读
        photoDao.insert(MemoryPhotoEntity(
            memoryLocalId = memoryLocalId,
            localPath = localPath,
            cloudFileId = fileId,
            byteSize = bytes.size.toLong(),
            createdAt = System.currentTimeMillis()
        ))

        return Result.success(MemoryPhoto(
            memoryLocalId = memoryLocalId,
            memoryCloudId = memoryCloudId,
            localPath = localPath,
            cloudFileId = fileId,
            byteSize = bytes.size.toLong(),
            synced = fileId != null
        ))
    }

    override fun observeByMemory(memoryLocalId: Long): Flow<List<MemoryPhoto>> =
        photoDao.observeByMemory(memoryLocalId).map { list -> list.toModels() }

    override fun observeAll(): Flow<List<MemoryPhoto>> =
        photoDao.observeAll().map { list -> list.toModels() }

    private fun List<MemoryPhotoEntity>.toModels(): List<MemoryPhoto> = map { e ->
        MemoryPhoto(
            memoryLocalId = e.memoryLocalId,
            memoryCloudId = "",
            localPath = e.localPath,
            cloudFileId = e.cloudFileId,
            byteSize = e.byteSize,
            synced = e.cloudFileId != null
        )
    }
}

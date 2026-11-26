package com.mulin.us.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mulin.us.data.local.dao.MemoryPhotoDao
import com.mulin.us.data.local.entity.MemoryPhotoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 4.8 照片持久化测试。
 *
 * 钉住一条不变量：照片行写进 Room 后，observeByMemory 能读回 ——
 * 这是"重启 App 后缩略图还在"的证据，不是靠内存 Map 假装。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class PhotoPersistenceTest {

    @Test
    fun `照片写入Room后能被Flow读回`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, YuniDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao: MemoryPhotoDao = db.memoryPhotoDao()
            assertEquals(0, dao.observeByMemory(42L).first().size)

            dao.insert(MemoryPhotoEntity(
                memoryLocalId = 42L,
                localPath = "/data/user/0/app/images/img-1.jpg",
                cloudFileId = "img-abc123",
                byteSize = 12345L,
                createdAt = System.currentTimeMillis()
            ))

            val rows = dao.observeByMemory(42L).first()
            assertEquals(1, rows.size)
            assertEquals("img-abc123", rows.first().cloudFileId)
            assertEquals(12345L, rows.first().byteSize)
            // 另一条回忆不该看到这张图
            assertEquals(0, dao.observeByMemory(99L).first().size)
        } finally {
            db.close()
        }
    }
}

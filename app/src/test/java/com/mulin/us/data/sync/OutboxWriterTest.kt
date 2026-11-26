package com.mulin.us.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mulin.us.data.local.dao.SyncCursorDao
import com.mulin.us.data.local.dao.SyncTaskDao
import com.mulin.us.data.local.entity.SyncCursorEntity
import com.mulin.us.data.local.entity.SyncTaskEntity
import com.mulin.us.domain.model.SyncOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Outbox 入队闸门测试（B0.1 T05）。
 *
 * 这里不用真 Room，用内存假 DAO 就够了：要验证的是"调了 enqueueDiary 之后
 * 到底有没有写进队列"，不是 DAO 的 SQL 对不对（那部分由迁移测试覆盖）。
 *
 * SafeLog 内部用 android.util.Log，所以用 Robolectric 提供 Android 环境；
 * 指定空 Application 是为了避开 YuniApplication 里 WorkManager 的初始化。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = android.app.Application::class)
class OutboxWriterTest {

    private class FakeTaskDao : SyncTaskDao {
        val tasks = mutableListOf<SyncTaskEntity>()

        override suspend fun insert(entity: SyncTaskEntity) {
            tasks += entity
        }

        override suspend fun insertOrReplace(entity: SyncTaskEntity) {
            tasks.removeAll { it.taskId == entity.taskId }
            tasks += entity
        }

        override suspend fun pending(limit: Int): List<SyncTaskEntity> =
            tasks.filter { it.syncStatus == "PENDING_UPLOAD" }.take(limit)

        override fun observePendingCount(): Flow<Int> = MutableStateFlow(tasks.size)

        override suspend fun pendingTaskForEntity(type: String, entityId: Long): SyncTaskEntity? =
            tasks.firstOrNull {
                it.entityType == type && it.entityId == entityId && it.syncStatus == "PENDING_UPLOAD"
            }

        override suspend fun delete(taskId: String) {
            tasks.removeAll { it.taskId == taskId }
        }

        override suspend fun bumpRetry(taskId: String, error: String?) = Unit

        override suspend fun setStatus(taskId: String, status: String) = Unit

        override suspend fun total(): Int = tasks.size
    }

    private class FakeCursorDao : SyncCursorDao {
        private val rows = mutableMapOf<String, SyncCursorEntity>()

        override suspend fun get(domain: String): SyncCursorEntity? = rows[domain]

        override fun observeAll(): Flow<List<SyncCursorEntity>> = MutableStateFlow(rows.values.toList())

        override suspend fun upsert(entity: SyncCursorEntity) {
            rows[entity.dataDomain] = entity
        }
    }

    private fun newWriter(): Pair<OutboxWriter, FakeTaskDao> {
        val taskDao = FakeTaskDao()
        return OutboxWriter(taskDao, FakeCursorDao()) to taskDao
    }

    /** 真实 Repository 入队时 payload 会带 client_mutation_id；测试也照此构造 */
    private fun payload(mutation: String? = null) = JSONObject()
        .put("content", "测试内容")
        .apply { if (mutation != null) put("client_mutation_id", mutation) }

    @Test
    fun `私密日记绝不入队`() = runBlocking {
        val (writer, dao) = newWriter()
        writer.enqueueDiary(1L, "PRIVATE", "couple-a", payload(), SyncOperation.CREATE, "m-1")
        assertEquals("私密日记一条任务都不许生成", 0, dao.tasks.size)
    }

    @Test
    fun `共享日记没有coupleId也不入队`() = runBlocking {
        val (writer, dao) = newWriter()
        // 迁移上来的历史数据就是这种：SHARED 但 coupleId = null（待认领）
        writer.enqueueDiary(2L, "SHARED", null, payload(), SyncOperation.CREATE, "m-2")
        writer.enqueueDiary(3L, "SHARED", "", payload(), SyncOperation.CREATE, "m-3")
        assertEquals("没认领空间的共享日记不能传，否则只会被 RLS 拒绝", 0, dao.tasks.size)
    }

    @Test
    fun `共享日记有coupleId才入队且payload带空间归属`() = runBlocking {
        val (writer, dao) = newWriter()
        writer.enqueueDiary(4L, "SHARED", "couple-a", payload(), SyncOperation.CREATE, "m-4")

        assertEquals(1, dao.tasks.size)
        val task = dao.tasks.single()
        assertEquals("diary", task.entityType)
        assertEquals("m-4", task.clientMutationId)
        assertTrue("云端要靠 couple_id 做行级权限校验", task.payload.contains("\"couple_id\":\"couple-a\""))
        assertTrue("入队后状态必须是待上传", task.syncStatus == "PENDING_UPLOAD")
    }

    @Test
    fun `同一条记录重复入队只保留最后一次`() = runBlocking {
        val (writer, dao) = newWriter()
        writer.enqueueDiary(5L, "SHARED", "couple-a", payload("m-5").put("content", "第一次"), SyncOperation.CREATE, "m-5")
        writer.enqueueDiary(5L, "SHARED", "couple-a", payload("m-6").put("content", "第二次"), SyncOperation.UPDATE, "m-6")

        assertEquals("同一条记录只留一条待上传任务", 1, dao.tasks.size)
        assertEquals(
            "路线图 2.6：覆盖时必须保留第一次的幂等键；否则首次上传其实成功但响应丢失时，" +
                "换新键会让云端重复创建一条",
            "m-5", dao.tasks.single().clientMutationId
        )
        assertTrue("覆盖后 payload 应是最后一次的内容",
            dao.tasks.single().payload.contains("第二次"))
        assertTrue("payload 内的 client_mutation_id 也要同步改成保留的键",
            dao.tasks.single().payload.contains("\"client_mutation_id\":\"m-5\""))
    }
}

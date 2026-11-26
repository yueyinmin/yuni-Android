package com.mulin.us.data.repository.impl

import com.mulin.us.data.local.YuniDatabase
import com.mulin.us.data.local.entity.LetterEntity
import com.mulin.us.data.local.toModel
import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.cloudbase.CloudSessionState
import com.mulin.us.data.remote.ecs.EcsGate
import com.mulin.us.domain.model.Letter
import com.mulin.us.domain.model.LetterStatus
import com.mulin.us.domain.repository.LetterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** S6 未来信 ECS 实现：正文服务端密文，未解锁不下发 */
class EcsLetterRepository(
    private val gate: EcsGate,
    private val db: YuniDatabase
) : LetterRepository {

    override suspend fun create(title: String, content: String, unlockAt: Long, kind: String): Result<Letter> = when (
        val r = gate.post("/letter/create", JSONObject()
            .put("title", title).put("content", content)
            .put("unlockAt", unlockAt).put("kind", kind))
    ) {
        is RemoteResult.Success -> {
            val letter = Letter(
                cloudId = r.data.optString("id"),
                title = r.data.optString("title"),
                unlockAt = r.data.optLong("unlockAt"),
                senderId = "", receiverId = "",
                status = LetterStatus.LOCKED
            )
            cache(listOf(letter))
            Result.success(letter)
        }
        else -> Result.failure(IllegalStateException("创建失败：$r"))
    }

    override suspend fun list(): Result<List<Letter>> = when (
        val r = gate.getArray("/letter/list")
    ) {
        is RemoteResult.Success -> {
            val list = (0 until r.data.length()).map { i ->
                val o = r.data.getJSONObject(i)
                Letter(
                    cloudId = o.optString("id"),
                    title = o.optString("title"),
                    unlockAt = o.optLong("unlockAt"),
                    senderId = if (o.optBoolean("mine")) "me" else "other",
                    receiverId = "",
                    status = if (o.optBoolean("unlocked")) LetterStatus.UNLOCKED else LetterStatus.LOCKED,
                    createdAt = o.optLong("createdAt")
                )
            }
            cache(list)
            Result.success(list)
        }
        else -> Result.failure(IllegalStateException("加载失败：$r"))
    }

    override suspend fun open(id: String): Result<String> = when (
        val r = gate.get("/letter/open?id=$id")
    ) {
        is RemoteResult.Success -> {
            // 服务端已经确认解锁，更新本地状态让时间线立刻出现这条
            markUnlocked(id)
            Result.success(r.data.optString("content"))
        }
        is RemoteResult.Forbidden -> Result.failure(IllegalStateException("还没到解锁时间"))
        else -> Result.failure(IllegalStateException("打开失败：$r"))
    }

    override fun observeUnlocked(): Flow<List<Letter>> =
        db.letterDao().observeUnlocked().map { list -> list.map { it.toModel() } }

    /** 本地只存壳，不存正式正文；未解锁的一律 LOCKED */
    private suspend fun cache(list: List<Letter>) {
        val coupleId = CloudSessionState.coupleId
        list.forEach { letter ->
            db.letterDao().upsert(LetterEntity(
                cloudId = letter.cloudId,
                coupleId = coupleId,
                title = letter.title,
                unlockAt = letter.unlockAt,
                senderId = letter.senderId,
                receiverId = letter.receiverId,
                status = letter.status.name,
                createdAt = letter.createdAt
            ))
        }
    }

    private suspend fun markUnlocked(cloudId: String) {
        val entity = db.letterDao().byCloudId(cloudId) ?: return
        db.letterDao().upsert(entity.copy(status = LetterStatus.UNLOCKED.name))
    }
}

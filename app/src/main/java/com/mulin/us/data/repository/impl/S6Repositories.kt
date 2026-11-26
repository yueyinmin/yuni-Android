package com.mulin.us.data.repository.impl

import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.ecs.EcsGate
import com.mulin.us.domain.model.Letter
import com.mulin.us.domain.model.LetterStatus
import com.mulin.us.domain.repository.LetterRepository
import org.json.JSONObject

/** S6 未来信 ECS 实现：正文服务端密文，未解锁不下发 */
class EcsLetterRepository(private val gate: EcsGate) : LetterRepository {
    override suspend fun create(title: String, content: String, unlockAt: Long, kind: String): Result<Letter> = when (
        val r = gate.post("/letter/create", JSONObject()
            .put("title", title).put("content", content)
            .put("unlockAt", unlockAt).put("kind", kind))
    ) {
        is RemoteResult.Success -> Result.success(Letter(
            cloudId = r.data.optString("id"),
            title = r.data.optString("title"),
            unlockAt = r.data.optLong("unlockAt"),
            senderId = "", receiverId = "",
            status = LetterStatus.LOCKED
        ))
        else -> Result.failure(IllegalStateException("创建失败：$r"))
    }

    override suspend fun list(): Result<List<Letter>> = when (
        val r = gate.getArray("/letter/list")
    ) {
        is RemoteResult.Success -> Result.success((0 until r.data.length()).map { i ->
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
        })
        else -> Result.failure(IllegalStateException("加载失败：$r"))
    }

    override suspend fun open(id: String): Result<String> = when (
        val r = gate.get("/letter/open?id=$id")
    ) {
        is RemoteResult.Success -> Result.success(r.data.optString("content"))
        is RemoteResult.Forbidden -> Result.failure(IllegalStateException("还没到解锁时间"))
        else -> Result.failure(IllegalStateException("打开失败：$r"))
    }
}

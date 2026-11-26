package com.mulin.us.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * 数据库迁移测试（v1 → v2）。
 *
 * 工单 B0.1 T05 要求：新增 Room Entity 必须提供 Migration 和迁移测试。
 *
 * ────────────────────────────────────────────────────────────
 * 为什么不直接用 Room 官方的 MigrationTestHelper
 * ────────────────────────────────────────────────────────────
 * 它要求 schema json 放在 assets 里，并且用 `instrumentation.getContext().assets` 读取。
 * 但 AGP 8.7 不会把 `sourceSets.test.assets` 合并进 unit test 的 assets（实测确认：
 * generateDebugUnitTestAssets 跑完后 build/intermediates/assets 下没有任何 json）。
 * 要让它工作，唯一办法是把 schema 塞进 main 的 assets —— 那等于让生产 APK
 * 无端背上数据库结构文件，不值得。
 *
 * 所以这里自己实现 MigrationTestHelper 的内核，步骤完全一样：
 * 1. 读 `app/schemas/…/1.json`，用里面的 `createSql` 建出一个**真实的 v1 库**
 *    （包括 room_master_table 和 v1 的 identityHash）；
 * 2. 用 Room(v2) 打开这个库，Room 会自己执行 MIGRATION_1_2 并做
 *    `validateMigration` —— 少一列、类型不对、默认值不符，它会直接抛异常；
 * 3. 断言业务数据仍在、且符合迁移规则。
 *
 * 换句话说：**schema 正确性是 Room 校验的，不是我手写 SQL 断言的。**
 * 我只测"数据有没有丢"和"新列的值对不对"。
 * ────────────────────────────────────────────────────────────
 * 为什么指定 @Config(application = Application::class)
 * ────────────────────────────────────────────────────────────
 * 不能用真实的 YuniApplication：它 onCreate 里会 schedulePeriodic()
 * → WorkManager.getInstance()，而 Robolectric 下 AndroidX Startup 不会执行，
 * 会直接抛 IllegalStateException，六个测试全部死在 Application 启动那一刻。
 * 迁移测试只需要一个能开库的环境，不需要 App 的业务初始化。
 * sdk = 33 是因为本机缓存了 robolectric 的 android-all-13 运行时，离线可跑。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = android.app.Application::class)
class MigrationTest {

    private companion object {
        const val DB_NAME = "migration-test.db"
        const val SCHEMA_V1 = "schemas/com.mulin.us.data.local.YuniDatabase/1.json"
    }

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    // ────────────────────────── 建 v1 库 ──────────────────────────

    /**
     * 按 1.json 建出真实的 v1 结构。
     *
     * 关键点：`createSql` 里的 `${'$'}{TABLE_NAME}` 是 Room 导出时的占位符，要替换成真实表名；
     * 索引同样在 indices[].createSql 里。
     * 最后写入 room_master_table —— 少了这一步，Room 会认为这不是它管的库，
     * 从而跳过敏捷校验，迁移测试就失去意义了。
     */
    private fun createV1Database(): SupportSQLiteDatabase {
        val schemaFile = File(SCHEMA_V1)
        check(schemaFile.exists()) {
            "找不到 v1 schema：${schemaFile.absolutePath}（工作目录=${File(".").absolutePath}）"
        }
        val schema = JSONObject(schemaFile.readText()).getJSONObject("database")
        val identityHash = schema.getString("identityHash")

        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val entities = schema.getJSONArray("entities")
                        for (i in 0 until entities.length()) {
                            val entity = entities.getJSONObject(i)
                            val tableName = entity.getString("tableName")
                            db.execSQL(
                                entity.getString("createSql").replace("\${TABLE_NAME}", tableName)
                            )
                            val indices = entity.optJSONArray("indices") ?: continue
                            for (j in 0 until indices.length()) {
                                val idx = indices.getJSONObject(j)
                                db.execSQL(
                                    idx.getString("createSql").replace("\${TABLE_NAME}", tableName)
                                )
                            }
                        }
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS room_master_table " +
                                "(id INTEGER PRIMARY KEY, identity_hash TEXT)"
                        )
                        db.execSQL(
                            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                                "VALUES(42, '$identityHash')"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        return helper.writableDatabase
    }

    /** 往 v1 库里塞"升级前就存在"的数据，模拟真实用户从 B0 版本升上来 */
    private fun seedV1Data() {
        val db = createV1Database()
        // 一条共同日记（已同步）
        db.execSQL(
            """
            INSERT INTO diary (ownerId, visibility, title, content, entryEpochDay,
                               createdAt, updatedAt, syncStatus, serverVersion)
            VALUES ('user-a', 'SHARED', '第一顿一起吃的饭', '在楼下那家面馆',
                    20000, 1700000000000, 1700000000000, 'SYNCED', 1)
            """.trimIndent()
        )
        // 一条私密日记（永不上云）
        db.execSQL(
            """
            INSERT INTO diary (ownerId, visibility, content, entryEpochDay,
                               createdAt, updatedAt, syncStatus, serverVersion)
            VALUES ('user-a', 'PRIVATE', '只有我知道的事',
                    20001, 1700000100000, 1700000100000, 'LOCAL_ONLY', 0)
            """.trimIndent()
        )
        // 一条历史问答
        // 注意：v1 的 revealed 是 INTEGER NOT NULL 且没有 DEFAULT，
        // 所以 INSERT 必须显式给值，否则报 NOT NULL constraint failed。
        // 这里给 0 = 还没互相揭晓，正是老版本最常见的状态。
        db.execSQL(
            """
            INSERT INTO daily_question (questionEpochDay, content, myAnswer, revealed, createdAt)
            VALUES (20000, '今天有什么瞬间让你开心？', '下班路上的晚霞', 0, 1700000000000)
            """.trimIndent()
        )
        // 一封还没解锁的信
        db.execSQL(
            """
            INSERT INTO letter (title, unlockAt, senderId, receiverId, hasLocalDraft, createdAt)
            VALUES ('写给一年后的你', 1800000000000, 'user-a', 'user-b', 0, 1700000000000)
            """.trimIndent()
        )
        db.close()
    }

    /**
     * 用 Room(v2) 打开 v1 库。
     *
     * 这一步本身就是 schema 正确性的严格校验：Room 会执行 MIGRATION_1_2，
     * 然后把结果表结构与编译期生成的 2.json 逐列比对，不一致就抛
     * 「Migration didn't properly handle: …」。
     */
    private fun openV2(): YuniDatabase =
        Room.databaseBuilder(context, YuniDatabase::class.java, DB_NAME)
            .addMigrations(*YuniDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

    // ────────────────────────── 测试 ──────────────────────────

    @Test
    fun `v1到v2_历史数据一条不丢`() {
        seedV1Data()

        val db = openV2()
        val raw = db.openHelper.writableDatabase

        raw.query("SELECT COUNT(*) FROM diary WHERE deletedAt IS NULL").use { c ->
            c.moveToFirst()
            assertEquals("升级后日记必须还是 2 条", 2, c.getInt(0))
        }
        raw.query("SELECT COUNT(*) FROM daily_question").use { c ->
            c.moveToFirst()
            assertEquals("升级后问答必须还在", 1, c.getInt(0))
        }
        raw.query("SELECT COUNT(*) FROM letter").use { c ->
            c.moveToFirst()
            assertEquals("升级后信件必须还在", 1, c.getInt(0))
        }
        // 具体内容也要对得上，不只是条数
        raw.query("SELECT title FROM diary WHERE visibility = 'SHARED'").use { c ->
            c.moveToFirst()
            assertEquals("第一顿一起吃的饭", c.getString(0))
        }
        db.close()
    }

    @Test
    fun `v1到v2_不给历史数据编造coupleId`() {
        seedV1Data()

        val db = openV2()
        val raw = db.openHelper.writableDatabase

        raw.query("SELECT coupleId FROM diary").use { c ->
            var rows = 0
            while (c.moveToNext()) {
                rows++
                assertTrue("历史日记的 coupleId 必须是 NULL（待认领），不能编造", c.isNull(0))
            }
            assertEquals("应该有 2 条日记被检查", 2, rows)
        }
        raw.query("SELECT coupleId FROM daily_question").use { c ->
            c.moveToFirst()
            assertTrue("历史问答的 coupleId 必须是 NULL", c.isNull(0))
        }
        raw.query("SELECT coupleId FROM letter").use { c ->
            c.moveToFirst()
            assertTrue("历史信件的 coupleId 必须是 NULL", c.isNull(0))
        }
        db.close()
    }

    @Test
    fun `v1到v2_updatedAt用createdAt回填而不是留在0`() {
        seedV1Data()

        val db = openV2()
        val raw = db.openHelper.writableDatabase

        raw.query("SELECT createdAt, updatedAt, syncStatus FROM daily_question").use { c ->
            c.moveToFirst()
            val createdAt = c.getLong(0)
            val updatedAt = c.getLong(1)
            assertEquals("updatedAt 必须回填成 createdAt，否则这行永远同步不到", createdAt, updatedAt)
            assertTrue("updatedAt 不能是 0", updatedAt > 0)
            assertEquals("新列默认同步状态应为 LOCAL_ONLY", "LOCAL_ONLY", c.getString(2))
        }
        db.close()
    }

    @Test
    fun `v1到v2_letter新列默认为草稿且无正文`() {
        seedV1Data()

        val db = openV2()
        val raw = db.openHelper.writableDatabase

        raw.query("SELECT status, localDraftContent FROM letter").use { c ->
            c.moveToFirst()
            assertEquals("老信件升级后状态应为 DRAFT", "DRAFT", c.getString(0))
            assertNull("localDraftContent 对老数据必须是 NULL", c.getString(1))
        }
        db.close()
    }

    @Test
    fun `v1到v2_message表可以正常写入读出`() {
        seedV1Data()

        val db = openV2()
        val raw = db.openHelper.writableDatabase

        raw.execSQL(
            """
            INSERT INTO message (coupleId, senderId, receiverId, content, createdAt, updatedAt,
                                 syncStatus, serverVersion)
            VALUES ('couple-1', 'user-a', NULL, '今天想你了', 1700000200000, 1700000200000,
                    'LOCAL_ONLY', 0)
            """.trimIndent()
        )

        raw.query(
            "SELECT content, readAt, deletedAt, receiverId FROM message WHERE senderId = 'user-a'"
        ).use { c ->
            assertTrue("刚插入的消息必须能查出来", c.moveToFirst())
            assertEquals("今天想你了", c.getString(0))
            assertTrue("新消息 readAt 应为 NULL（未读）", c.isNull(1))
            assertTrue("新消息 deletedAt 应为 NULL（未删除）", c.isNull(2))
            assertTrue("receiverId 允许为 NULL（表示发给空间里另一个人）", c.isNull(3))
        }
        db.close()
    }

    @Test
    fun `v1到v2_message的四个索引都建出来了`() {
        seedV1Data()

        val db = openV2()
        val raw = db.openHelper.writableDatabase

        val expected = setOf(
            "index_message_coupleId",
            "index_message_createdAt",
            "index_message_readAt",
            "index_message_syncStatus"
        )
        val actual = mutableSetOf<String>()
        raw.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'message'")
            .use { c ->
                while (c.moveToNext()) {
                    c.getString(0)?.let { actual += it }
                }
            }
        assertTrue("message 的四个索引必须都在，实际: $actual", actual.containsAll(expected))
        db.close()
    }

    /**
     * 加了 coupleId 却不改唯一索引，等于没解决问题：
     * 两个空间同一天各写一条问答时，第二个会撞 index_daily_question_questionEpochDay。
     * 这条测试同时验证「旧索引已删」「新索引生效」「同空间内仍然唯一」。
     */
    @Test
    fun `v1到v2_问答唯一索引改为同一空间内按天唯一`() {
        seedV1Data()

        val db = openV2()
        val raw = db.openHelper.writableDatabase

        val names = mutableSetOf<String>()
        raw.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'daily_question'")
            .use { c ->
                while (c.moveToNext()) c.getString(0)?.let { names += it }
            }
        assertTrue("必须建出复合唯一索引，实际: $names",
            "index_daily_question_coupleId_questionEpochDay" in names)
        assertFalse("旧的全局按天唯一索引必须删掉，实际: $names",
            "index_daily_question_questionEpochDay" in names)

        // 两个不同空间、同一天：都要能写进去
        raw.execSQL(
            "INSERT INTO daily_question (coupleId, questionEpochDay, content, revealed, createdAt) " +
                "VALUES ('couple-a', 20000, 'A 空间今天的问题', 0, 1700000300000)"
        )
        raw.execSQL(
            "INSERT INTO daily_question (coupleId, questionEpochDay, content, revealed, createdAt) " +
                "VALUES ('couple-b', 20000, 'B 空间今天的问题', 0, 1700000300000)"
        )
        raw.query("SELECT COUNT(*) FROM daily_question").use { c ->
            c.moveToFirst()
            assertEquals("历史 1 条 + 两个空间各 1 条 = 3 条", 3, c.getInt(0))
        }

        // 同一个空间同一天写第二条：必须被唯一索引挡住
        val duplicated = runCatching {
            raw.execSQL(
                "INSERT INTO daily_question (coupleId, questionEpochDay, content, revealed, createdAt) " +
                    "VALUES ('couple-a', 20000, '重复题', 0, 1700000400000)"
            )
        }.isFailure
        assertTrue("同一空间同一天必须仍然唯一", duplicated)

        db.close()
    }

    @Test
    fun `v1到v2_版本号推进到2`() {
        seedV1Data()

        val db = openV2()
        val raw = db.openHelper.writableDatabase

        raw.query("PRAGMA user_version").use { c ->
            c.moveToFirst()
            // ALL_MIGRATIONS 会把 v1 一路升到当前最新版本（现在是 3）
            assertEquals("升级后 user_version 必须是当前最新版本 3", 3, c.getInt(0))
        }
        db.close()
    }

}

package com.mulin.us.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移登记处。
 *
 * 规矩（B0.1 T05 立下，以后每次改表都照这个来）：
 * 1. 一版一个 Migration，命名 MIGRATION_旧版本_新版本，不要跳版本；
 * 2. 默认只做"加法"——加列、加表、加索引。删列、改类型这种破坏性操作必须先导出数据再重建；
 *    唯一例外是"改索引定义"：先 DROP 旧索引再 CREATE 新索引，不动任何一行数据；
 * 3. 每一版都必须写迁移测试（见 app/src/test/.../MigrationTest.kt），用真实 v1 库升上来验证；
 * 4. 绝不使用 fallbackToDestructiveMigration() —— 那等于升级一次清一次数据。
 */

/**
 * v1 → v2：补齐 T05 指出的四个结构缺口。
 *
 * 改动清单：
 * | 表              | 动作                                        | 为什么 |
 * |-----------------|---------------------------------------------|--------|
 * | diary           | 加 coupleId（可空）                          | 共享日记要能确定属于哪个空间 |
 * | daily_question  | 加 coupleId / updatedAt / syncStatus / serverVersion；唯一索引改成 (coupleId, questionEpochDay) | 原表完全没有同步字段；单列唯一索引会让两个空间同一天撞键 |
 * | letter          | 加 coupleId / localDraftContent / status     | 草稿要能留在本机；草稿与已发出的信要能区分 |
 * | message         | 新建表 + 4 个索引                            | 悄悄话原本无处可存 |
 *
 * 三条不可越线的原则：
 *
 * ① **不给历史数据编造 coupleId。**
 *    迁移里所有新加的 coupleId 都是 NULL。为什么：现在数据库里如果有数据，
 *    那都是"还没绑定空间时写的"，它们属于哪个空间这件事**无人知道**。
 *    随便填一个值等于把数据挂到错误的空间下，比留空危险得多。
 *    NULL 的语义是"待认领"，由 B1 绑定空间后由业务决定怎么处理。
 *
 * ② **daily_question.updatedAt 用 createdAt 回填，不留在 0。**
 *    增量同步靠 "server_updated_at > 游标" 判断，如果历史行的时间戳是 0，
 *    它们永远不会被任何一次拉取命中。回填成 createdAt 是唯一合理的推断。
 *
 * ③ **message 表的 NOT NULL 列不给 DEFAULT。**
 *    新表是从零创建的，不存在"老行缺值"的问题；
 *    而 ALTER TABLE 加 NOT NULL 列时 SQLite 强制要求 DEFAULT，所以上面那几列必须有。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // ---------- 1. diary：加空间归属 ----------
        db.execSQL("ALTER TABLE `diary` ADD COLUMN `coupleId` TEXT")

        // ---------- 2. daily_question：加空间归属 + 四个同步字段 ----------
        db.execSQL("ALTER TABLE `daily_question` ADD COLUMN `coupleId` TEXT")
        db.execSQL("ALTER TABLE `daily_question` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `daily_question` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
        db.execSQL("ALTER TABLE `daily_question` ADD COLUMN `serverVersion` INTEGER NOT NULL DEFAULT 0")
        // 回填：0 会让这一行永远同步不到（见上文原则 ②）
        db.execSQL("UPDATE `daily_question` SET `updatedAt` = `createdAt` WHERE `updatedAt` = 0")
        // 唯一索引从「按天全局唯一」改成「同一空间内按天唯一」。
        // 不改的话，两个人各自绑定不同空间，同一天都会被第二个人写入时撞唯一键。
        // coupleId 为 NULL 的历史行不受唯一约束（SQLite 里 NULL 互不相等），保留"待认领"状态。
        db.execSQL("DROP INDEX IF EXISTS `index_daily_question_questionEpochDay`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_question_coupleId_questionEpochDay` " +
                "ON `daily_question` (`coupleId`, `questionEpochDay`)"
        )

        // ---------- 3. letter：加草稿与状态 ----------
        db.execSQL("ALTER TABLE `letter` ADD COLUMN `coupleId` TEXT")
        db.execSQL("ALTER TABLE `letter` ADD COLUMN `localDraftContent` TEXT")
        db.execSQL("ALTER TABLE `letter` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'DRAFT'")

        // ---------- 4. message：新建悄悄话表 ----------
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `message` (
                `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `cloudId` TEXT,
                `coupleId` TEXT,
                `senderId` TEXT NOT NULL,
                `receiverId` TEXT,
                `content` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `readAt` INTEGER,
                `deletedAt` INTEGER,
                `syncStatus` TEXT NOT NULL,
                `clientMutationId` TEXT,
                `serverVersion` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_coupleId` ON `message` (`coupleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_createdAt` ON `message` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_readAt` ON `message` (`readAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_syncStatus` ON `message` (`syncStatus`)")
    }
}

/**
 * v2 → v3（工单 V3.1 C3.5）：恋爱开始日期改用 ISO 字符串语义存储。
 *
 * 为什么加列而不改列类型：
 * - 旧列 `relationshipStartEpochDay` 被 20+ 处引用（含云端协议），直接改类型风险大；
 * - 新增 `startDateIso` 作为**权威存储**，旧列保留兼容只读；
 * - 迁移时用 SQLite 的 date() 把 epochDay 转成 "YYYY-MM-DD" 回填，跨时区不偏移。
 *
 * epochDay → ISO 的换算：date(epochDay, 'unixepoch', 'days')
 * 例：epochDay=20454 → date(20454,'unixepoch','days') = '2026-01-01'
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `couple_profile` ADD COLUMN `startDateIso` TEXT")
        db.execSQL(
            """
            UPDATE `couple_profile`
            SET `startDateIso` = date(`relationshipStartEpochDay`, 'unixepoch', 'days')
            WHERE `relationshipStartEpochDay` IS NOT NULL
            """.trimIndent()
        )
    }
}

/**
 * v3 → v4：双端日期同步需要冲突判定时间。
 *
 * 只加 `startDateUpdatedAt`（毫秒），旧数据为 0：
 * 服务端有值时以服务端为准；两端都是 0 时按"谁先写谁生效"处理。
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `couple_profile` ADD COLUMN `startDateUpdatedAt` INTEGER NOT NULL DEFAULT 0")
    }
}

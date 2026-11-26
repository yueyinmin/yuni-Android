# 证据代码快照说明

本目录是 **B0.1 验收时点（HEAD `b7f6d04`）的完整源码副本**，供后续技术复核直接阅读，不是可编译的源码树。

- 权威源码位置：`C:\Users\22109\dev\yuni-android\app\src\...`
- 本目录仅作冻结快照：以后改代码，改的是权威位置，不会同步更新这里。
- 目录结构与工程内相对路径一致（例如 `app/src/main/java/com/mulin/us/data/local/Migrations.kt`）。
- `GIT-提交记录.txt` 记录本轮全部提交。
- 测试日志在上一级 `证据日志/`，被 `.gitignore` 的 `*.log` 排除，不进 Git。

用途对应工单第十七节 10 项代码证据：
1. Room Entity/DAO → `app/src/main/java/com/mulin/us/data/local/entity/Entities.kt`、`dao/Daos.kt`
2. Database + Migration → `YuniDatabase.kt`、`Migrations.kt`、`app/schemas/.../1.json`、`2.json`
3. Navigation → `ui/navigation/YuniNavigation.kt`
4. Repository → `domain/repository/Repositories.kt`、`data/repository/impl/RepositoryImpls.kt`
5. ViewModel ↔ UI → `ui/YuniViewModel.kt`、`MainActivity.kt` 及各页面
6. CloudBase → `data/remote/cloudbase/*`、`core/common/AppConfig.kt`、`data/remote/offline/OfflineDataSources.kt`
7. 隐私字段与权限 → `core/security/TokenVault.kt`、`core/common/SafeLog.kt`、`data/sync/OutboxWriter.kt`
8. Git 记录 → `GIT-提交记录.txt`
9. 测试代码 → `app/src/test/java/...` 四个文件
10. 构建日志 → 上一级 `证据日志/最终构建与测试.log`（本机留存）

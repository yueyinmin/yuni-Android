# 02 — Git 仓库与基线提交报告

- 工单：US-B0.1-ARCH-ALIGNMENT / T02
- 仓库名：Us-Android
- 仓库路径：`C:\Users\22109\dev\yuni-android`
- 当前分支：`main`

---

## 1. 实际检查内容

按顺序做了 4 件事：

1. 检查目标目录：存在完整 Android 工程，无 `.git` 目录。
2. 检查是否有既有 Git 元数据：`git rev-parse` 无结果 → 可以安全初始化，没有覆盖历史的风险。
3. 检查敏感信息：`local.properties` 当时只有 `sdk.dir`，**没有 envId / clientId / clientSecret 的实际值**。
4. 写 `.gitignore` → `git init` → 先提交基线，**再**开始改代码。

### 密钥扫描（最终复验）

```
$ git check-ignore -v local.properties
.gitignore:24:local.properties    local.properties

$ python 遍历 git ls-files，逐文件比对 local.properties 里的值
tracked secret hits: []
```

结论：仓库里没有密钥、密码、Token、数据库凭据。`local.properties` 被明确忽略；`.gitignore` 同时排除 keystore、`.env*`、`secrets.properties`、`*.secret`、`*.log`。

---

## 2. `.gitignore` 关键规则

| 规则 | 目的 |
|---|---|
| `.gradle/`、`build/`、`**/build/` | 构建产物不入库 |
| `!gradle/wrapper/gradle-wrapper.jar` | 唯一例外：别人 clone 后要能构建 |
| `local.properties` | Android SDK 路径 + 未来可能填的云端凭证 |
| `*.jks` `*.keystore` `*.p12` `*.pfx` `*.bks` `keystore.properties` `signing.properties` | 签名密钥 |
| `.env` `.env.*` `secrets.properties` `*.secret` | 环境变量与密钥文件 |
| `*.log` `logs/` | 本地日志（本轮验收日志留在工作区，不入库） |
| `.idea/` `*.iml` `.vscode/` | IDE 临时文件 |

---

## 3. 提交记录（本轮全部）

```
b7f6d04 test(navigation): 为五Tab与底栏显隐规则补充单测
d2782b8 feat(data): 对齐 v2 数据模型并补齐迁移与闸门测试
bb2dcd9 chore(db): 导出 v1 schema 作为迁移基线
8d94cfc feat(navigation): 五 Tab 结构调整为 首页/回忆/互动/信箱/我们
2fbbe63 refactor: 收口单模块包分层与依赖边界
d3cf7d4 chore: establish B0 baseline
```

- B0 基线：`d3cf7d4`，48 个文件，其中 33 个 Kotlin 文件 / 4261 行。
- 最终 HEAD：`b7f6d04`。
- 每个逻辑任务一个独立提交；未为凑数拆无意义提交。
- 所有提交都在测试通过后形成；未把未通过测试的版本标为稳定。
- 远程仓库：**未创建、未推送、未改变公开状态**（工单 5.4 / 13.6）。

---

## 4. 修改的文件
- 新增 `.gitignore`。
- 创建 `.git` 仓库元数据（非业务文件）。

## 5. 修改原因
建立可追溯基线；先固定 B0 状态，再允许后续改动，便于逐步 `git diff`。

## 6. 修改前后差异
从"无版本管理"变为"本地 main 分支 + 6 个提交 + 明确忽略规则"。

## 7. 测试结果
- `git status --short`：最终为空（工作区干净）。
- 密钥扫描：0 命中。
- 无远程操作。

## 8. 尚未解决的问题
- 是否创建 GitHub 远程仓库、仓库公开还是私有：**待用户决策**。本轮保持仅本地。
- `.gitignore` 忽略了 `*.log`，因此验收日志只留在本机交付目录，不进 Git。若以后要把测试日志纳入版本管理，需要单独调整规则（不建议，日志会持续变大）。

## 9. 证据
完整提交记录副本：`证据代码/GIT-提交记录.txt`；`.gitignore` 全文：`证据代码/.gitignore`。

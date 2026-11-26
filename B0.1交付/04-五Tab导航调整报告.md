# 04 — 五 Tab 导航结构调整报告

- 工单：US-B0.1-ARCH-ALIGNMENT / T04
- 对应提交：`8d94cfc feat(navigation): 五 Tab 结构调整为 首页/回忆/互动/信箱/我们`、`b7f6d04 test(navigation)`
- 决策依据：K3

---

## 1. 实际检查内容

| 项 | 修改前（基线 `d3cf7d4`） | 修改后（当前） |
|---|---|---|
| Tab 1 | 首页（home） | 首页（home） |
| Tab 2 | 回忆（memory） | 回忆（memories） |
| Tab 3 | 日记（diary，一级） | 互动（interaction，新建） |
| Tab 4 | 愿望（wish，一级） | 信箱（mailbox，新建） |
| Tab 5 | 我的（mine，自检页） | 我们（us，由 MineScreen 改名） |
| 日记入口 | 一级 Tab | 「回忆」里的按钮 → `memories/diary` 二级页 |
| 愿望入口 | 一级 Tab | 「我们」里的按钮 → `us/wishes` 二级页 |
| 底栏显隐 | 所有页面都显示 | 只在 5 个顶级 Tab 显示，二级页隐藏 |
| 导航文件 | `ui/YuniApp.kt` | `ui/navigation/YuniNavigation.kt`（YuniApp 删除） |

---

## 2. 修改的文件

| 文件 | 动作 | 原因 |
|---|---|---|
| `ui/navigation/YuniNavigation.kt` | 新建 | 路由表 + 五 Tab + 二级页；抽出 `YuniNavSpec` 供单测 |
| `ui/YuniApp.kt` | 删除 | 被 YuniNavigation 取代 |
| `ui/home/HomeScreen.kt` | 从 `ui/screen/` 移入 | 按功能分包 |
| `ui/memories/MemoryListScreen.kt` | 由 MemoryScreen 改名并移入 | 增加「写日记」入口 |
| `ui/memories/DiaryScreen.kt` | 移入 | 增加 `onBack` 和自带返回栏 |
| `ui/couple/UsScreen.kt` | 由 MineScreen 改名并移入 | 增加「愿望清单」入口 |
| `ui/couple/WishScreen.kt` | 移入 | 增加 `onBack` 和自带返回栏 |
| `ui/interaction/InteractionScreen.kt` | 新建占位页 | 补 K3 的 Tab；不放假按钮 |
| `ui/mailbox/MailboxScreen.kt` | 新建占位页 | 补 K3 的 Tab；页面里写明解锁规则属服务端控制 |
| `ui/components/Formatters.kt` | 新建 | 时间 / 同步状态格式化统一 |
| `ui/components/CommonComponents.kt` | 新建 | SyncBadge / EmptyHint / LabeledRow |
| `ui/components/SubPageScaffold.kt` | 新建 | 二级页统一「← 返回」顶栏 |
| `MainActivity.kt` | 改调用 | 从 `YuniApp(container)` 改为 `YuniNavigation(container)` |
| `ui/navigation/YuniNavigationTest.kt` | 新建测试 | 4 例：Tab 顺序、路由唯一、二级页归属、底栏显隐 |

---

## 3. 修改前后差异（关键逻辑）

### 底栏显隐
```kotlin
// 修改前：任何页面都显示底栏
NavigationBar { tabs.forEach { ... } }

// 修改后：纯函数，可单测
object YuniNavSpec {
    val topLevelRoutes: Set<String> = tabs.map { it.route }.toSet()
    fun shouldShowBottomBar(route: String?) = route != null && route in topLevelRoutes
}
```

### Tab 切换
保留 `popUpTo(startDestination) { saveState = true } + launchSingleTop + restoreState`，避免堆栈累积并保留滚动位置；不设 `inclusive`，保证返回键不会直接退出 App。

### 二级页
`memories/diary` 和 `us/wishes` 自带 `Scaffold`，**不吃外层 innerPadding**，避免两层状态栏留白；返回统一 `navController.popBackStack()`。

---

## 4. 功能是否丢失

| 原功能 | 去向 | 状态 |
|---|---|---|
| 首页关系卡片 / 自检 | `ui/home/HomeScreen.kt` | 保留 |
| 回忆列表 + 新增回忆 | `ui/memories/MemoryListScreen.kt` | 保留，额外加日记入口 |
| 私密/共同日记的写入与列表 | `ui/memories/DiaryScreen.kt` | 保留，未删任何字段或按钮 |
| 愿望清单的添加 / 勾选完成 | `ui/couple/WishScreen.kt` | 保留 |
| 本地模式自检信息 | `ui/couple/UsScreen.kt` | 保留 |
| 互动 / 信箱 | 新占位页 | 新增，明确标注待实施，不放假功能 |

数据库未做任何删除操作；日记和愿望的数据表、DAO、Repository 全部保留。

---

## 5. 测试结果

`YuniNavigationTest` 4 例全部通过：

```
com.mulin.us.ui.navigation.YuniNavigationTest.html — 4 tests, 0 failures
  ✓ 五个Tab顺序和名称完全符合K3
  ✓ 路由不能重复
  ✓ 日记挂在回忆下_愿望挂在我们下
  ✓ 底栏只在顶级Tab显示
```

**边界说明**：这是路由表和显隐规则的逻辑测试，不是真机点击测试。真机上点击五个 Tab、返回键、二级页进出，仍属 **未验证**（本机模拟器此前长时间 offline，见 `01` 和 `11`）。

## 6. 尚未解决的问题
- 互动页和信箱页是占位页，实际业务属 B5 / B6，本轮按工单禁止提前开发。
- 图标仍是汉字占位（省约 3MB material-icons 依赖），正式图标留到 B7。
- 真机交互验证待做；不阻塞 B1 开工，但阻塞"体验验收"。

## 7. 证据
`证据代码/app/src/main/java/com/mulin/us/ui/navigation/YuniNavigation.kt`（完整文件）、`证据代码/app/src/test/java/com/mulin/us/ui/navigation/YuniNavigationTest.kt`。

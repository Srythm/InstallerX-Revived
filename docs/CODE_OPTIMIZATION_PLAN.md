\# InstallerX Revived 代码优化建议



> Version: 1.0  

> Review Scope: 全项目（Data / Domain / UI / DI / Compose / Room / Koin）  

> 更新时间：2026-06-27



\---



\# 总览



项目整体代码质量较高，具有以下优点：



\- 清晰的分层（Data / Domain / UI）

\- Koin 使用规范

\- Version Catalog 管理统一

\- Material3 与 Miuix UI 家族分离合理

\- Room、Flow、Compose 架构设计规范



本次代码审查共发现 \*\*32 项优化建议\*\*，按照：



> \*\*正确性 → 性能 → 可维护性 → UI 一致性 → 技术债\*\*



排序。



\## 优先级统计



| 优先级 | 数量 | 建议 |

|---------|------|------|

| 🔴 P0 | 6 | 必须优先修复 |

| 🟠 P1 | 10 | 建议近期完成 |

| 🟡 P2 | 11 | 维护性优化 |

| 🟢 P3 | 5 | 技术债清理 |



建议开发顺序：



```

P0

&#x20;↓

P1

&#x20;↓

P2

&#x20;↓

P3

```



\---



\# P0 正确性（必须优先修复）



\---



\## 1. CoroutineScope 泄漏



\### 文件



```

IBinderAppInstallerRepoImpl.kt:62

AppInstallerRepositoryImpl.kt:124-138

```



\### 问题



Repository 初始化：



```kotlin

CoroutineScope(Dispatchers.IO)

```



但生命周期未管理。



resolveRepo()



每次安装：



```

创建 Repo

↓



创建 CoroutineScope

↓



永不释放

```



批量安装多个 APK 将累计泄漏多个 Job。



\### 建议



方案一（推荐）



Repository 注册为：



```kotlin

single<IBinderAppInstallerRepo> { ... }

```



共享生命周期。



方案二



通过 Koin 注入：



```kotlin

@Named("TaskScope")

CoroutineScope(...)

```



统一管理。



\### 收益



\- 消除 Scope 泄漏

\- 生命周期正确

\- 减少后台线程



\---



\## 2. BackupRepository Room 操作缺少事务



\### 文件



```

BackupRepositoryImpl.kt

applyImportPlan()

rollback()

```



\### 问题



当前流程：



```

deleteAll()



↓



insert()



↓



insert()



↓



insert()

```



未使用：



```kotlin

withWriteTransaction

```



崩溃将导致数据库被清空。



\### 建议



注入：



```

InstallerRoom

```



统一：



```kotlin

room.withWriteTransaction {



}

```



将：



\- restore

\- rollback

\- export



全部事务化。



\### 收益



\- 保证数据库一致性

\- 崩溃可恢复



\---



\## 3. Process.join() 无超时



\### 文件



```

DefaultPrivilegedService.kt

243-309

```



\### 问题



```

stdoutThread.join()



stderrThread.join()

```



无限等待。



若 su 卡死：



整个安装线程永久阻塞。



\### 建议



```

waitFor(timeout)



join(timeout)



destroyForcibly()

```



\### 收益



Root 服务不会永久卡死。



\---



\## 4. CancellationException 被吞



\### 文件



```

AnalyserRepositoryImpl.kt

157-173

```



\### 问题



```

catch(Exception)

```



将：



```

CancellationException

```



转换：



```

emptyList()

```



破坏结构化并发。



\### 建议



```kotlin

catch (e: CancellationException) {

&#x20;   throw e

}



catch (e: Exception) {



}

```



\---



\## 5. InputStream 泄漏



\### 文件



```

IBinderAppInstallerRepoImpl.kt

379-397

```



\### 问题



InputStream 未：



```kotlin

use {



}

```



异常时资源泄漏。



\### 建议



参考：



```

NoneAppInstallerRepoImpl

```



统一：



```kotlin

inputStream.use {



}

```



\---



\## 6. Broadcast Receiver Scope 未取消



\### 文件



```

BroadcastHandler.kt

84

```



\### 问题



```

CoroutineScope(SupervisorJob())

```



仅注销 Receiver。



未取消 Scope。



\### 建议



```

receiverScope.cancel()

```



或共享父 Scope。



\---



\# P1 性能优化



\---



\## 7. Module Log O(n²)



\### 文件



```

ProcessInstallationUseCase.kt

```



\### 问题



每收到一行：



```

output.toList()

```



复制整个列表。



日志越长性能越差。



\### 建议



采用：



\- 每 200ms emit

\- 每 50 行 emit

\- append 增量更新



\---



\## 8. DataStore 重复 first()



\### 重灾区



\- SessionNotifierImpl

\- ActionHandler

\- ProcessInstallationUseCase

\- GetResolvedConfigUseCase



\### 建议



新增：



```kotlin

suspend fun snapshot(): AppPreferences

```



统一读取一次：



```kotlin

preferencesFlow.first()

```



减少重复 Flow 收集。



\---



\## 9. isInstallingModule 重复遍历



建议：



放入：



```

InstallerState

```



或：



```

derivedStateOf

```



\---



\## 10. uiState 使用 SharingStarted.Eagerly



建议：



```kotlin

SharingStarted.WhileSubscribed(5000)

```



后台停止收集。



\---



\## 11. DAO 查询补 suspend



涉及：



```

find()



findByPackageName()



findByNullPackageName()

```



统一：



```kotlin

suspend

```



\---



\# P2 UI 一致性



\---



\## 12. Material3 与 Miuix 漂移



\### 已发现



\#### 图标



AllowSigMismatch



AllowSigUnknown



Miuix 使用同一图标。



\---



\#### 文案



SDK 描述：



Material3



```

config\_display\_sdk\_version\_desc

```



Miuix：



手动字符串。



\---



\#### 功能



Installer Package 自动补全：



仅 Material3。



\### 建议



统一共享 UI Model。



\---



\## 13. EditItemWidget 重复约 150 行



包括：



\- enum map

\- displayName

\- authorizer list



建议抽：



```

EditOptionMaps.kt

```



\---



\# P2 Compose



\---



\## 14. remember() 缺少 key



当前：



```kotlin

remember {



}

```



建议：



```kotlin

remember(rootMode, isSupported) {



}

```



避免 Root Mode 切换 UI 不刷新。



\---



\## 15. collectAsState



统一：



```

collectAsStateWithLifecycle()

```



\---



\## 16. Pair 不稳定



建议：



```kotlin

@Immutable

data class

```



替代：



```

Pair<Boolean, Boolean>

```



\---



\## 17. LazyColumn items 缺 key



统一：



```kotlin

items(

&#x20;   key = {}

)

```



\---



\## 18. seedColor 协程竞争



建议维护：



```

seedColorJob

```



进入新任务：



```

cancel()



launch()

```



\---



\# P2 代码重复



\---



\## 19. installResultVerify / uninstallResultVerify



约 95% 相同。



建议抽：



```

verifyResult()

```



\---



\## 20. doFinishWork



两个 Installer Repo 基本一致。



建议抽：



```

BaseInstallerRepo

```



并确认：



```

Authorizer.None

```



是否存在逻辑问题。



\---



\## 21. Drawable → Bitmap



统一调用：



```

toSafeBitmap()

```



\---



\## 22. Magic Number



当前：



```

100000

```



建议：



```kotlin

const val PER\_USER\_UID\_RANGE

```



\---



\## 23. PackageManager Compat



建议：



```

PackageManagerCompat.kt

```



统一：



```

getPackageInfo()



getApplicationInfo()

```



\---



\## 24. Wallpaper Flow



两个 Provider 完全重复。



建议：



```

ThemeStateProvider



↓



依赖



↓



SystemEnvProvider

```



\---



\## 25. Enum Converter



统一：



```

Enum.fromValue()

```



避免：



```

entries.find

```



\---



\# P3 启动优化



\---



\## 26. createdAtStart



建议：



DatabaseInitializer



Lazy 初始化。



后台加载。



\---



\## 27. GlobalContext



升级：



Koin 4.x API。



\---



\# P3 错误处理



\---



\## 28. printStackTrace()



统一：



```

Timber.e()

```



\---



\## 29. message.contains()



当前：



```

contains("binder...")

```



建议：



```

InstallerException

```



携带：



```

errorType

```



\---



\## 30. RequiresApi



确认：



```

PackageSourceConverter

```



是否误标：



```

@RequiresApi(TIRAMISU)

```



\---



\# P3 无障碍



\---



\## 31. clickable()



增加：



```

Role.Switch



onClickLabel

```



\---



\## 32. 硬编码字符串



包括：



\- UID

\- Android

\- to



统一迁移：



```

strings.xml

```



\---



\# Miuix UI 移除计划（保留 Blur / Shader）



\## 删除源码



\### Miuix UI



\- \[ ] 删除 56 个 `miuix` 包源码

\- \[ ] 删除 4 个独立 Miuix 文件

\- \[ ] 清理无引用资源



\---



\## Gradle



\- \[ ] 删除 5 个 Miuix UI 依赖

\- \[ ] 保留 `miuix-blur`

\- \[ ] 保留 `miuix-shader`

\- \[ ] 清理 Version Catalog



\---



\## Manifest / Theme



\- \[ ] InstallerTheme

\- \[ ] Backdrop

\- \[ ] Color

\- \[ ] InstallerNavContainer

\- \[ ] InstallerActivityContent

\- \[ ] SettingsActivity



\---



\## Domain / Data



\- \[ ] ThemeState

\- \[ ] PredictiveBackAnimation

\- \[ ] AppPreferences

\- \[ ] AppSettingsRepository

\- \[ ] AppDataStore

\- \[ ] ThemeStateProviderImpl



\---



\## UI



\- \[ ] InstallerState

\- \[ ] InstallerViewAction

\- \[ ] InstallerViewModel

\- \[ ] Theme Settings 四件套



\---



\## 字符串资源



涉及：



23 个 locale。



删除：



\- \[ ] theme\_miuix

\- \[ ] use\_miuix

\- \[ ] theme\_miuix\_\*

\- \[ ] miuix\_\*



\---



\# 推荐实施顺序



| 阶段 | 内容 | 目标 |

|------|------|------|

| Phase 1 | 修复 P0 | 消除崩溃、资源泄漏、事务问题 |

| Phase 2 | 修复 P1 | 提升安装性能与响应速度 |

| Phase 3 | 修复 P2 | 减少重复代码，统一 UI |

| Phase 4 | 移除 Miuix | 精简架构 |

| Phase 5 | 技术债清理 | 冷启动优化、Koin 升级 |



\---



\# 总结



本次审查共发现 \*\*32 项优化建议\*\*。



\- \*\*P0\*\*：协程生命周期、Room 事务、IO 资源释放、Cancellation 传播等问题，建议立即修复。

\- \*\*P1\*\*：性能优化，重点包括 DataStore、Flow、Compose 与日志处理。

\- \*\*P2\*\*：UI 一致性与代码复用，可显著降低维护成本。

\- \*\*P3\*\*：启动优化、错误处理统一及技术债清理，可作为后续版本持续改进内容。



建议采用 \*\*P0 → P1 → P2 → P3\*\* 的渐进式实施策略；若计划彻底移除 Miuix UI 家族，建议在完成 P0/P1 后单独进行一次架构重构，以避免功能修改与架构迁移交叉，提高迁移效率与可控性。


# 更新日志 —— 上游同步（2026-09-18）

## 同步概览

| 项 | 值 |
|---|---|
| 合并提交 | `5dacb838` (Merge remote-tracking branch 'origin/main' into code-optimization-p0) |
| 父节点 | `eb9b2a8f`（本地） + `8ede2725`（上游） |
| 合并时间 | 2026-09-18 16:13:10 +0800 |
| 上游合并点时间 | 2026-09-16 13:50:14 +0200 |
| 分叉点 (merge-base) | `11f15e47` |
| 并入上游提交 | **21 个**（`21135797` .. `8ede2725`） |
| 本地独有提交 | 41 个（合并前）→ **42 个**（合并后） |
| 同步后状态 | 领先上游 42 / 落后 0 |
| 备份分支 | `backup/code-optimization-p0-pre-sync-20260918` |
| 改动规模 | 62 文件 / +2618 / −732 |
| 冲突数 | 12（4 个 Miuix modify/delete + 8 个内容冲突） |

改动分布：`app/src/main/java` 29 · `app/src/main/res` 21 · `app/src/test` 5 · `gradle/libs.versions.toml` 1 · `app/build.gradle.kts` 1 · `.idea/codeStyles` 2 · `docs/README(.CN).md` 2

---

## 一、依赖与技术栈升级

| 依赖 | 旧 → 新 |
|---|---|
| AGP | 9.3.2 → **9.4.0** |
| Kotlin | 2.4.10 → **2.4.20** |
| KSP | 2.3.11 → 2.3.12 |
| Compose BOM | 2026.08.00 → **2026.09.00** |
| Compose | 1.12.0 → 1.12.1 |
| Material 3 | 1.5.0-alpha26 → 1.5.0-alpha28 |
| Room | 3.0.1 → 3.0.3 |
| AboutLibraries | 15.1.1 → 15.2.0 |
| MaterialKolor | 5.0.0 → 5.0.1 |
| Benchmark MacroJUnit4 | 1.4.1 → 1.5.0 |
| Baseline Profile | 1.5.0-rc01 → 1.5.0（正式版） |
| Spotless | 8.10.0 → 8.10.2 |
| composeKtlintRules | 0.6.4 → 0.6.6 |

### ⚠️ 破坏性变更：apksig 依赖改为复用本机 SDK

`com.android.tools.build:apksig`（原 9.3.2）已**从版本目录中移除**，改为直接挂载本机 Android SDK 的
`build-tools/<ver>/lib/apksigner.jar`：

```kotlin
private val minApksignerBuildTools = "37.0.0"
// 自动挑选 >= 37.0.0 且含 lib/apksigner.jar 的最低版本；找不到则 error() 硬失败
implementation(files(apksignerJar))
```

**影响**：构建环境必须存在 **Build Tools >= 37.0.0 且带 `lib/apksigner.jar`**，否则配置阶段直接失败。
本机已确认满足（Android SDK 的 `build-tools/` 下有 34.0.0 / 36.0.0 / 36.1.0 / 37.0.0，
其中 37.0.0 带 apksigner.jar）。

**副作用**：原先的 `apksig` 版本号由"9.5.0-alpha02"（提交标题所写）实际并未落地 —— 该版本随着依赖整体移除而作废，
真实形态是"改用 SDK 内置 jar"。提交标题与最终效果不一致，勿据标题判断依赖版本。

---

## 二、新功能

### 1. 支持列出已安装的 Magisk 模块（`7c712ea5`）

原先 Magisk 模式**完全不支持**模块枚举（`moduleListCommand` 直接 `return null`）。现改为：

```
sh -c 'for prop in /data/adb/modules/*/module.prop; do [ -f "$prop" ] || continue;
       printf "\036"; cat "$prop"; done'
```

用 `\u001e` 作记录分隔符，`java.util.Properties` 解析每个 `module.prop`
（id / name / version / versionCode / author / description / updateJson）。
KernelSU（`ksud module list`）与 APatch（`apd module list`）维持原有 JSON 解析路径。
新增 `parseMagiskModuleList` 及单测。

### 2. 未知安装来源作用域（`f2884ee5`，closes #802）

来源不明确时**不再猜测**为共享 UID 的包，而是归类为 `Unknown`。
新增可空的 Unknown scope 与持久化的显示开关，设置页分组展示，菜单支持多选，并纳入备份校验。
涉及 `PlatformInstallPolicyChecker` / `ConfigResolver` / `ApplyItemWidget` / `ApplyPage` /
`ApplyViewModel` / `ApplyViewState` / `ApplyViewAction` / `GroupedDropdownMenuPopup` 等 21 个文件。
新增 `ToggleAppTargetConfigUseCaseTest`。

### 3. 保留安装器选择与恢复的应用详情（`7aa6db96`）

- 批量选择改为**单次统一更新**，并忽略过期的选择事件（避免 stale UI 事件回滚当前选择）。
- 重新选中某包时，恢复此前的 split 与 dex 元数据选择；文档说明保存的选择何时被取代。
- 从通知重新打开安装结果时恢复选中的应用，包含"APK 里同时含模块"的混合情形。
- 新增 27 个测试覆盖选择、过期事件与会话恢复。

新增 ViewAction：
```kotlin
data class TogglePackageSelection(val packageName: String, val entity: SelectInstallEntity)
data class SetApkSelection(val selected: Boolean)
```

> **上游遗留待验证项**（原文）：
> "Device notification reopening still needs retesting."
> —— 通知重开路径上游尚未在真机复测。

---

## 三、缺陷修复

### 1. 并发 ZIP 读取数据串扰（`86355c51`）

commons-compress 在"延迟本地头解析"与"payload 读取"之间**未共用同一把锁**：打开第二个 entry 时会 seek
到活跃 payload 流所用的同一 channel，导致并发读取互相破坏。
修复方式为包裹 `LockedEntryInputStream`，将 `read / skip / available / mark / reset / close`
全部收敛到 `synchronized(zipFile)`。新增 171 行测试。

> 这是真实的数据损坏缺陷，非单纯重构。

### 2. APK 签名方案 v3.2 / ML-DSA 支持（`cf368f64`）

- 新增 v3.2 方案：block ID `0x70e1c89f`，`hasSdkRange = true`。
- `MIN_SDK_WITH_V32_SUPPORT = Build.VERSION_CODES.CINNAMON_BUN`。
- 证书选取优先级提升为 v3.2 → v3.1 → v3。
- 常量由魔法数字改为 `Build.VERSION_CODES.*`（P / TIRAMISU / CINNAMON_BUN）。
- `readSdkRange` 的校验由 `minSdk < 0 || minSdk > maxSdk` 改为 `minSdk !in 0..maxSdk`。
- 新增 40 行测试。

### 3. Android 8.x 上的生物识别认证（`739ee6c3`）

在 `app/src/main/keepRules/android-components.keep` 补 3 行保留规则。

### 4. 设置项高度适配字体缩放（`07360f51`）

Material 3 `ListItem` 的固定 56dp/72dp 最小高度不随 `fontScale` 收缩，小字体下留白过多。
改为按 `fontScale` 缩放最小高度并 `coerceAtLeast(48.dp)`，同时移除原先按密度计算的
`dynamicInternalPadding` 内边距。代码中已注明：升级 Material 3 时需复核此 workaround。

### 5. ScaleNavTransition 退出动画（`e2b0e143`）

修正预测返回转场的 `exitAnimation`。

### 6. Xposed 模块设置文案（`cfdd2627`）

修正 22 个文件中的设置项标签（含 21 个语言资源）。

---

## 四、国际化

三批 Weblate 翻译更新：

| 提交 | 内容 |
|---|---|
| `f6ffcd8e` (#804) | **新增波兰语完整翻译（values-pl，+502 行）**；ja / pt-rBR / ru / uk / zh-rTW / es 更新 |
| `da05598f` (#814) | de / es / pl / pt-rBR / ru / uk |
| `8ede2725` (#829) | **日语大规模修订（388 行变更）**；es / pt-rBR / ru / uk |

---

## 五、合并冲突与语义冲突处置

### 已解决的 12 个冲突

| 类型 | 文件 | 处置 |
|---|---|---|
| modify/delete ×4 | `MiuixInstallerPage.kt` / `InstallChoiceContent.kt` / `MiuixApplyPage.kt` / `MiuixDefaultInstallerPage.kt` | `git rm` 保留删除（上游对 Miuix 与 Material3 两侧做了等价改动） |
| 内容 ×5 | `ApplyPage.kt` / `ApplyViewModel.kt` / `AppSettingsRepositoryImpl.kt` / `InstallerViewModel.kt` / `InstallChoiceDialog.kt` | 逐块合并，保住本地重构 |
| 翻译 ×3 | `values-ja` / `values-pt-rBR` / `values-uk` | 保留上游翻译，删除本地已移除功能的键 |

### 合并后才暴露的 3 处「语义冲突」（git 不报）

1. **上游新测试引用本地已删参数**
   `InstallerSelectionTest.kt` 的 `testPreferences()` 传入 `expandDialogTemporarySettingsByDefault` /
   `showMiuixUI` / `useMiuixMonet`，三者本地均已删除 → Kotlin 编译报
   `No parameter with name '...' found`。处置：删除这 3 行。

2. **`uiState` 的 SharingStarted 语义分歧（最重要）**
   本地提交 `9cc0028a` 将 11 个 ViewModel 由 `SharingStarted.Eagerly` 改为
   `SharingStarted.WhileSubscribed(5000)`：

   | | Eagerly | WhileSubscribed |
   |---|---|---|
   | 上游 `origin/main` | 16 | 1 |
   | 本地（合并前） | 3 | 14 |

   上游新测试**不订阅**即直接读 `vm.uiState.value`，在 `WhileSubscribed` 下 `stateIn` 永不启动，
   `.value` 停在构造时的 `initialValue` → 该类 27/27 全部失败，表现为
   `expected:<[...]> but was:<[]>` / `stage` 恒为 `Ready` / `currentPackageName` 恒为 `null`，
   极易误判为业务回归。
   处置：**保住本地重构，改测试夹具**（保持订阅；并在 `finally` 中先取消两个 job 再 `resetMain()`，
   否则 `stateIn` 收尾协程访问主线程会抛 `CompletionHandlerException`）。

3. **翻译资源漏删**
   本地移除了 `expand_temporary_settings_by_default` 的默认值资源，但 `values-pl/strings.xml` 未同步删除
   → AAPT 警告 `removing resource ... without required default value`。处置：删除该 2 行。
   附带排查确认：本仓另有既有孤儿键 `config_authorizer_msg`（values-es / values-zh-rTW），非本次引入。

---

## 六、构建与验证

### 环境变更

- **`--offline` 已不可用**：AGP/Kotlin/Compose BOM 均升级且依赖集变动 → 必须联网构建，
  否则报 `Plugin [id: 'com.android.library', version: '9.4.0'] was not found`。
- `JAVA_HOME` 必须为 `C:/Program Files/Java/jdk-25.0.2`（正斜杠；MSYS 风格 `/c/...` 会被错转）。

### 验证结果（全部实测）

| 命令 | 结果 |
|---|---|
| `./gradlew testUnstableDebugUnitTest --no-build-cache` | **BUILD SUCCESSFUL** —— 30 个测试类 / 149 用例 / 0 失败 0 错误 |
| `./gradlew assembleUnstableDebug --no-build-cache` | **BUILD SUCCESSFUL** —— `app-Unstable-debug.apk` 30,758,294 B，versionName `26.09.eb9b2a8`，versionCode 143 |
| `./gradlew :app:checkUnstableDebugDuplicateClasses --rerun-tasks --no-build-cache` | **BUILD SUCCESSFUL**（任务真实执行 2m59s） |
| `./gradlew :app:mergeUnstableDebugNativeLibs --rerun` | **BUILD SUCCESSFUL**（任务真实执行） |

### 本机构建已知问题（与本次同步无关）

Gradle build cache 写入 `.part` 报「拒绝访问」：
`Failed to store cache entry ... \.gradle\caches\build-cache-1\<hash>.part (拒绝访问。)`

- 该问题会把症状**伪装**成 `compileXxxUnitTestKotlin FAILED` / `checkXxxDuplicateClasses FAILED` /
  `mergeXxxNativeLibs FAILED`，真实原因在 `Failed to store cache entry` 一行。
- 已排除目录 ACL（本用户对该目录有完全控制）与残留 daemon。
- 可靠绕过：`--no-build-cache`。
- 依赖解析阶段失败的任务，重跑显示 `UP-TO-DATE` **不算已验证**，需 `--rerun-tasks` 强制实跑。

---

## 七、待办与风险

| 项 | 状态 |
|---|---|
| 通知重开路径的真机复测 | **未验证** —— 上游原文 "Device notification reopening still needs retesting." |
| 推送远端 | **未执行**（用户未要求；本地分支） |
| `AGENTS.md` 是否需补充 apksig / Build Tools 要求 | 未处理 —— 当前 `AGENTS.md` 的构建前置章节未提及 Build Tools >= 37.0.0 这一新增硬要求，建议后续补上 |
| `docs/CODE_OPTIMIZATION_PLAN.md` L31 | 仍有过时的「Material3 与 Miuix UI 家族分离合理」表述（L905 已标注该节失效），超出本次同步范围 |

---

## 附：并入的上游提交清单（21）

```
21135797 chore(deps): update apksig from 9.3.2 to 9.5.0-alpha02
8bb65883 fix(deps): update all non-major dependencies (#803)
36924a71 fix(deps): update all non-major dependencies (#805)
30ceea6e fix(deps): update aboutlibraries to v15.2.0 (#806)
3cf778d5 chore: update IDE code style configuration
cf368f64 fix: support ML-DSA and APK signature v3.2
f6ffcd8e i18n: Translations update from Hosted Weblate (#804)
f2884ee5 feat: add unknown install source scope, closes #802
739ee6c3 fix: biometric auth on Android 8.x (#812)
07360f51 fix: adapt setting item height to font scale
5d4b1ac8 fix(deps): update all non-major dependencies (#813)
cfdd2627 fix: correct Xposed module setting labels
72d98fc4 fix(deps): update dependency com.diffplug.spotless to v8.10.2 (#819)
e2b0e143 fix: fix ScaleNavTransition 's exitAnimation (#818)
03fe1145 fix(deps): update Kotlin to v2.4.20 (#822)
7c712ea5 feat: support listing installed Magisk modules
54acebe0 fix(deps): update all non-major dependencies (#826)
da05598f i18n: Translations update from Hosted Weblate (#814)
86355c51 fix: synchronize concurrent ZIP entry reads
7aa6db96 fix: preserve installer choices and restored app details
8ede2725 i18n: Translations update from Hosted Weblate (#829)
```

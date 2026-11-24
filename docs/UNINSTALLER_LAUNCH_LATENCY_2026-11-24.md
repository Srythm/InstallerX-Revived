# 卸载器唤起延迟分析（2026-11-24）

本文记录“偶现唤起卸载面板时界面延迟出现”的取证过程、实测数据、代码层成因，以及与设备上模块版 release 的对照结论。
**本轮只做分析与测量，未修改任何代码**；优化项按性价比列在第 8 节，尚未实施。

- 设备：RMX5090 / Android 17
- 被测对象
  - 新版（本分支 debug）：`com.rosan.installer.x.revived`，`versionName 26.11.223e095`、`versionCode 1755`、`flags=[DEBUGGABLE]`
  - 模块版（Stable release 装入 `/system/priv-app`）：`com.android.packageinstaller`，`versionName 26.11.1.20261122.1`、`versionCode 1721`、`flags=[SYSTEM]`
- 测量方式：`adb shell am start -W`（`TotalTime` / `Displayed`）+ `adb logcat -v time` 打点，冷启动用 `am force-stop` 前置

---

## 1. 结论

1. **主导因素是冷启动，不是渲染管线，也与 COUI 全屏/模糊这轮改动无关。** 同一套代码热启动 0.14s 出帧，冷启动 0.87–1.44s，差 6–10 倍。
2. **冷启动最大的单项是 dex 加载**：debug 包没有可用 oat，日志为 `Odex status:io-error-no-oat filter:run-from-apk ... cost:530~829ms`，每次冷启动都要从 `base.apk` 重新加载校验。
3. **观感被放大，是因为“没有任何视觉反馈”**：两个 Activity 都用 `Theme.Installer.Translucent`，系统因此不为它们创建启动窗口（`doesn't want a starting window`）。这段几百毫秒到一秒多的时间里，屏幕上一直是调用方的界面。
4. **面板出现后还要等一次解析**：`UninstallResolving → UninstallReady` 在本机是 1.9–2.1s，期间面板已经可见但应用名/版本/图标色未就绪。
5. **“偶现”的三个真实来源**：
   - 冷/热进程差异（0.87–1.44s vs 0.14s）；
   - 冷启动本身的双峰（不同次采样落在 ~0.88s 与 ~1.4s 两簇，见第 3 节）；
   - 上一次卸载会话未结束时再次唤起会被**排队**（第 6.3 节）。
6. **模块版 release 更快，但快在构建与部署方式**（AOT + R8 + 特权 + 常驻），代码路径与新版一致；它**同样没有启动窗口**，同样有 `Ready` 空白帧，也同样会排队。

---

## 2. 测量环境与方法

```bash
adb shell am force-stop <pkg>            # 冷启动前置
adb logcat -c
adb shell am start -W -a android.intent.action.DELETE \
  -d package:<目标包> -n <pkg>/com.rosan.installer.ui.activity.UninstallerActivity
adb logcat -d -v time | grep -E "Start proc|Displayed|onCreate|collected progress|Odex status"
```

`am start -W` 的 `TotalTime` 与 `Displayed ... +Nms` 是同一口径（首帧），本文统一用首帧。热启动样本不 force-stop，直接再次 `am start`。

---

## 3. 实测数据

### 3.1 首帧（首帧 = `Displayed`，单位 ms）

| 构建 | 冷启动样本 | 热启动样本 |
| --- | --- | --- |
| 新版 debug（n=7） | 873 / 876 / 889 / 1386 / 1399 / 1408 / 1437 | 141 / 151 / 482 |
| 模块版 release（n=5） | 534 / 637 / 960 / 984 / 1017 / 1092 | 105 / 121 / 122 |

两点必须如实说明：

- **冷启动呈双峰**：新版 debug 明显分成 ~0.88s 与 ~1.4s 两簇，模块版也有 ~0.55s 与 ~1.0s 两簇。双峰与 `Odex status` 里 `cost:` 的波动同步（实测出现 530ms 与 829ms 两种），推测与设备当时的 IO/CPU 状态有关，本次未做控制变量验证。
- **两版区间在尾部有重叠**（1.0s 附近），所以“release 一定比 debug 快”不成立；成立的是“release 的中位更靠前，且不承担 no-oat 成本”。

### 3.2 一次完整的冷启动时间线（新版 debug，同一次会话内打点）

```
19:32:04.559  Start proc 30357:com.rosan.installer.x.revived  ← 进程 fork
19:32:05.506  Odex status: io-error-no-oat / run-from-apk / cost:829
19:32:05.741  UninstallerSessionActivity onCreate            ← 跳板已转发，会话 Activity 起来
19:32:05.915  Displayed  +1s408ms                             ← 首帧
19:32:06.244  UninstallResolving                              ← 面板开始有骨架内容
19:32:08.321  UninstallReady                                  ← 应用名/版本/图标色就绪
```

拆段：

| 段 | 耗时 | 说明 |
| --- | --- | --- |
| 进程 fork → 会话 `onCreate` | ≈ 1,182ms | 含运行时初始化、Koin、以及上面 829ms 的 no-oat dex 加载 |
| `onCreate` → 首帧 | ≈ 174ms | Compose 首次组合 + 主题状态 |
| 首帧 → `UninstallResolving` | ≈ 329ms | 会话建立、ViewModel 接到首个进度 |
| `UninstallResolving` → `UninstallReady` | ≈ 2,077ms | 卸载信息解析（DataStore + PackageManager + 可选取色） |
| **进程 fork → 可用内容** | **≈ 3.76s** | |

模块版同法采集：`Start proc 19:32:13.201 → Displayed 19:32:14.189`，即 ≈ 988ms，且**没有** `Odex status` 行（存在 AOT 产物）：

```
/data/dalvik-cache/arm64/...PackageInstaller.apk@classes.art   1.68 MB（另有 .vdex）
```

---

## 4. 代码路径

外部 `ACTION_DELETE` 进来后经过两个 Activity：

1. **`UninstallerActivity`（跳板）** —— `app/src/main/java/com/rosan/installer/ui/activity/UninstallerActivity.kt:28-43`
   不绘制任何内容，只把 intent 改写 flag 后 `startActivity` 到会话 Activity 再 `finish()`。
   `computeDetachedUninstallFlags()` 会清掉 task 关联与 result 转发，让卸载面板独立成任务，避免调用方在包被删后连带关闭它。**代价是多一次 Activity 启动（实测 40–50ms）。**
2. **`UninstallerSessionActivity`（会话）** —— `UninstallerSessionActivity.kt:50-73`
   `onCreate` → `startUninstallIntent()` → 权限检查 → `resolveUninstall()` → 收集 `progress` → `showContent()`。

两处“空白”就出现在这一层与它下游：

- `CouiInstallerPage.kt:285-287`：`if (stage is InstallerStage.Ready && !isClosingFullscreen) return@InstallerCOUITheme`
- `InstallerActivityContent.kt:29-30`：`if (!uiState.isLoaded) return`
- `ThemeStateProviderImpl.kt:24-70`：`themeStateFlow = combine(preferencesFlow, wallpaperColors) { ... }.stateIn(Eagerly, initialValue = ThemeState())`，`isLoaded` 只在首个组合结果里才为 `true`

也就是说：窗口已经创建、首帧已经画出来，但这三处依次把内容挡在门外，**首帧到 `UninstallResolving` 之间的 300ms 量级里，屏幕上是空的**（透明窗口，所以看到的是调用方）。

---

## 5. 为什么“偶现”

### 5.1 冷 / 热进程

同一份代码，热启动 141ms、冷启动 1400ms。系统是否回收过进程完全不受应用控制，所以用户感知就是“有时很快、有时明显卡一下”。

### 5.2 冷启动自身双峰

`Odex status ... cost:` 在样本间从 530ms 波动到 829ms，冷启动因此分成两簇。设备负载、IO 抖动都会影响这一段。

### 5.3 上一次会话未结束 → 新唤起被排队

`UninstallerSessionActivity.kt:79-83`：

```kotlin
if (shouldDeferUninstallIntent()) {                      // :206
    sessionManager.enqueueForegroundUninstall(Intent(intent).apply { removeExtra(KEY_UNINSTALLER_ID) })
    Timber.d("UninstallerSessionActivity deferred foreground uninstall intent.")
    return
}
```

只要旧会话还停在 `UninstallResolving / UninstallReady / Uninstalling / UninstallSuccess / UninstallFailed / UninstallResolveFailed`（`isForegroundUninstallProgress()`，:232-237），新唤起就只会入队，等旧会话发出 `Finish` 才启动。

实测命中过一次：

```
19:16:43.153  UninstallerSessionActivity onNewIntent.
19:16:43.154  UninstallerSessionActivity deferred foreground uninstall intent.
```

**这一类无论 debug 还是 release 都不会改善**，是最容易被误判成“卡死”的场景。

### 5.4 首次未授权时多一次权限弹窗

模块版所有运行时权限是 `SYSTEM_FIXED | GRANTED_BY_DEFAULT`，debug 包的 `POST_NOTIFICATIONS` 是用户授权（`USER_SET`）。全新安装后首次唤起会先走 `requestEssentialPermissions`，期间还会 `isRequestingPermission = true` 抑制 `onStop` 关会话。

---

## 6. 与模块版 release 的差异

| 维度 | 新版（branch，debug） | 模块版 release |
| --- | --- | --- |
| 安装位置 | `/data/app/...` | `/system/priv-app/PackageInstaller` |
| 包名 | `com.rosan.installer.x.revived` | `com.android.packageinstaller` |
| flags | `DEBUGGABLE` | `SYSTEM` |
| AOT | **无** → `io-error-no-oat`、`run-from-apk`、`cost:530~829ms` | **有** `classes.art` 1.68MB + `.vdex` |
| R8 / 资源收缩 | `optimization.enable = false` | `true` |
| 权限 | 用户授权 | `SYSTEM_FIXED \| GRANTED_BY_DEFAULT` |
| 被回收概率 | 高 | 低（更常落在热启动） |
| 启动窗口 | 无 | **同样无** |
| `Ready` 空白帧 | 有 | **同样有** |
| deferred 排队 | 有 | **同样有** |

结论：差异来自**构建类型与部署方式**，不是代码路径。本分支这轮改动集中在 COUI 全屏、展开分组与设置菜单，未触及 `UninstallerActivity` / `UninstallerSessionActivity` / `UninstallResolver`。

---

## 7. 优化选项（尚未实施，按性价比排序）

| # | 措施 | 针对 | 预期 | 代价 / 风险 |
| --- | --- | --- | --- | --- |
| 1 | 给会话 Activity 一个极简启动窗口（或同帧骨架屏） | “点了没反应” | 立竿见影：把空白期换成可见反馈 | 与 `windowIsTranslucent` 的视觉风格冲突，需要设计一版与面板同色的 `windowBackground` |
| 2 | `Ready` 阶段也渲染面板骨架，而不是 `return` | 首帧后 300ms 空白 | 首帧即有内容 | 需要骨架与真实内容同几何，否则会跳一下 |
| 3 | 合并跳板 Activity（flag 改写移进会话 Activity） | 每次 40–50ms | 小幅但稳定 | 需保留独立任务语义，回归面比 1、2 大 |
| 4 | 主题状态用内存快照预热，避免 `isLoaded` 门 | 首帧偶发空帧 | 小 | 冷启动首次仍要读 DataStore |
| 5 | 发 release / 非 debuggable 包 | no-oat 的 530–829ms | 冷启动中位明显前移 | 只治“慢”，不治“没有反馈”与“排队” |
| 6 | `UninstallResolving → Ready` 的解析瘦身 | 1.9–2.1s 的二次等待 | 面板更快可用 | 与包数量、取色逻辑相关，需要单独测量 |

排序理由：1 与 2 直接消除用户感知的“无响应”，且不改变业务逻辑；5 改善的是时长，前两者改善的是“有没有反馈”。

---

## 8. 边界与未验证项

- 本文数值全部来自上述单台设备（RMX5090 / Android 17），不同 SoC、不同系统版本的 `dexopt` 策略会显著改变冷启动分布。
- **未控制变量**：采样期间未固定屏幕亮度、未清后台、未做多次开机冷态对照，因此第 3.1 节的双峰只能描述现象，不能归因到具体子系统。
- **未测 release 版本的本分支构建**：模块版是 `main`（`26.11.1`）的 release，不是本分支的 release。两者代码路径一致，但若要精确对比本分支 release，需要单独构建并在设备上复测（会覆盖当前 debug 安装）。
- **未做 Macrobenchmark / 系统 `LaunchTime2` 轨迹采集**：本轮的 `TotalTime` 是 `am start -W` 口径，足够定位结构性瓶颈，不足以做逐帧归因。
- 第 7 节所有措施均未实施，也没有对应的 CI 或真机验证。

---

## 9. 复现命令

```bash
# 冷启动
adb shell am force-stop com.rosan.installer.x.revived
adb logcat -c
adb shell am start -W -a android.intent.action.DELETE -d package:<目标包> \
  -n com.rosan.installer.x.revived/com.rosan.installer.ui.activity.UninstallerActivity
adb logcat -d -v time | grep -E "Start proc|Displayed|onCreate|collected progress|Odex status"

# 热启动：去掉 force-stop，直接再次 am start
# 排队场景：不返回、不关闭面板，连续两次 am start
```

模块版把包名换成 `com.android.packageinstaller` 即可，其余一致。

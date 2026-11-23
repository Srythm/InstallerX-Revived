# COUI 全屏渐进式模糊：当前问题与待决事项

- 日期：2026-11-23
- 源码基线：`main` @ `e3e0ce4b`（tag `26.11.1`，versionCode 1721）
- 工作分支：`feat/coui-fullscreen-progressive-blur`，未合 main；本轮动画修复提交 `8df262f3`（取代失效的 `c7271f0b`），后续文档更新不改变该基线。
- 文档性质：问题清单与待决事项；不是功能说明，也不替代 `COUI_FULLSCREEN_PROGRESSIVE_BLUR_REPORT_2026-11-23.md` 的适配方案。
- 验证状态：`./gradlew spotlessApply` 通过；CI `manual-build.yml`（Preview/debug）多次通过，动画修复最终 run `45184368115` 通过；**未跑单元测试、无真机自动化验收**。本机无 Android SDK，编译验证只能走 CI。

## 1. 已落地且编译验证通过的部分

| 能力 | 位置 | 状态 |
| --- | --- | --- |
| 内容采样源 + 覆盖式外壳 | `CouiPositionFullScreen.kt` | 已落地；静态源供材质/弹出层，内容源供上下渐进带 |
| 顶部渐进带（状态栏＋顶栏＋头部整段，带主题遮罩） | `CouiPositionFullScreen.kt`、`Backdrop.kt CouiProgressiveEdgeBand` | 已落地；观感以 2dc8a1a7 为基准恢复 |
| 底部渐进带（仅有操作栏的页面） | `CouiPositionFullScreen.kt` | 已落地；无按钮页面不绘制 |
| 留白下沉（准备/权限/临时设置/批量结果） | 各 content 文件 | 已落地；居中状态页保留预留布局 |
| 文案「最低 SDK」、准备页黑色行标题 `headlineXS` | `strings.xml` ×2、`CouiFullScreenPrepareContent.kt` | 已落地 |
| miuix pin `5ff41ba9 → d82b40fa` | `gradle/libs.versions.toml` | 仅在本分支（`f82db3d4`） |

## 2. 未解决的视觉/交互问题

### 2.1 子页推送动画与发行版不一致（已实现修复，待真机复核）

- `d38d9ba0`：头部与正文开始解耦；头部仍在外壳覆盖层，不进入内容采样源。
- 方向修正 `8df262f3`（取代 `c7271f0b`）：`c7271f0b` 只改了空目标槽上的 `AnimatedContent` 位移符号，头部退出仍不可见。现改回 `AnimatedVisibility` 作为真正的 enter/exit 宿主：进入子页时头部向左退出，返回主页时从左侧进入。正文仍使用 `subPageTransition()` 的标准推送（旧主页向左退、子页从右进）。
- 旧主页与新子页分别持有稳定的顶部布局：开启模糊时各自保留固定 top bleed，关闭模糊时各自保留固定 viewport top inset；转场期间不再由 chrome 高度驱动正文顶边连续变化。共享采样源与 `CouiProgressiveEdgeBand` 的实现未改。
- `d7f1cced`：同一主页内仅阶段导致头部出现／消失时，保留 220/180ms 淡入淡出；该内层转场继续保留，只有子页变化才让整个头部左出左回。
- 验证：`spotlessApply` 通过；CI run `45168880991`、`45169673533`、`45182311088` 与最终 run `45184368115` 均通过 Preview/debug 组装。**尚无真机录屏，进入与返回子页、关模糊降级仍需复核。**

### 2.2 顶部带遮罩与页面 wash 的轻微色差（理论存在，未真机复核）

顶部带的不透明端为平色 `backgroundWithCard`，而页面背景是 base + 两团径向 tint。带覆盖头部间隙处会压掉 tint，理论上与周围 wash 存在轻微色差。2dc8a1a7 观感已被认可，但深色 / AMOLED / 动态色 / 安装包临时主题色下未复核。

### 2.3 部分页面顶部仍为静态材质（设计内限制）

- 安装选项页：卡片整体从顶栏下缘开始，列表视口不伸到栏后，顶部带只采样背景 → 无真实内容渐进；卡片圆角保留。
- 模块日志、确认页、分包选择页、进度/完成/错误页、卸载/归档恢复：保留预留布局与居中语义（`StageBodyContent` 的 `else -> padding(top = topBleed, bottom = bottomBleed)`），顶部静态材质。
- 准备页顶部为真实渐进（正文滚过顶栏+头部后方）；权限页、临时设置页顶部为真实渐进；准备页与批量结果底部为真实渐进。

### 2.4 底部带在按钮区不透明度偏高（观感待复核）

底部带遮罩沿用顶部带的镜像 stops（浅色 `0 → 0.7@0.4 → 1`，深色 `0 → 0.4`，见 `Backdrop.kt` `CouiProgressiveEdgeBand`）。操作按钮位于带的不透明端附近，视觉上接近一条实色底栏。用户曾要求「无按钮页面去掉遮罩」（已实现：底部带以 `bottomBar != null` 为门），但有按钮页面的遮罩强度是否合适未确认。

## 3. 未验证项（验收缺口）

- 真机矩阵：浅色/深色/AMOLED/动态色/临时主题色；手势/三键导航；横屏与平板；大字体与显示缩放；API 26–32 与关模糊降级；Compose 预览。
- 行为回归：全屏关闭 220ms 时序与 ViewModel 1000ms 兜底、退出/过渡冻结快照、顶栏按钮 `SizeTransform(clip=false)`、装饰层不吞点击、半屏与 Material 3 家族、原 COUI 设置页、卸载与归档恢复、连续新安装与通知重入。
- 性能：内容源进入滚动/日志录制链路后的帧耗时分位数、卡顿帧、图层内存；长权限列表、高频模块日志、批量结果、页面交叉淡入淡出；60Hz 与高刷设备。**没有任何实测数字，不应引用改善幅度。**
- 单元测试：`testUnstableDebugUnitTest` 未在本分支跑过（CI manual-build 只组装）。既有 `InstallerFullscreenCloseTest`、`CouiFullScreenStateTest` 可作为回归入口。
- 无 `app/src/androidTest`；不存在设备截图/录屏验收。

## 4. 仓库级待决事项

### 4.1 main 的 miuix pin 已失效（阻塞 main 编译）

上游已从 GitHub Packages 删除 `0.9.5-5ff41ba9-SNAPSHOT`（实测 `miuix-blur-android` 全部版本列表中不存在）。main 仍 pin 该版本，**任何分支含 main 都解析不到 miuix-blur/nav/material**。修复 `f82db3d4`（→ `0.9.5-d82b40fa-SNAPSHOT`）只在本分支。

- 待决：是否将 `f82db3d4` 单独合回 main（与模糊功能解耦）。
- 已核对：`d82b40fa` 上 `miuix-blur` 仍导出 `LayerBackdrop` / `ProgressiveBlur` / `progressiveTextureBlur`（签名一致），`miuix-material` 仍有 `RuntimeShader`。

### 4.2 分支历史含被取代的方案

12 个提交中，`2dc8a1a7` 的 glide 曾被 `5818183b` 取代、又在 `0b897e10` 恢复；`90782412`–`55c660cb` 的正文小带/每槽实验已整体撤销。是否 squash 成「功能 + 修复 + deps」三条左右待决；未擅自改写历史。

### 4.3 文档同步

- 本报告与 `COUI_FULLSCREEN_PROGRESSIVE_BLUR_REPORT_2026-11-23.md` 均为未跟踪/新增文档；后者行号对应基线 `e3e0ce4b`，实施后已部分过时（外壳结构、头部归属、带几何），引用时需重新核对。
- `docs/README.md` 未描述全屏模糊视觉；若要把新视觉写成产品行为，需按 AGENTS.md 同步英文与简中说明。

## 5. 建议的下一步顺序

1. 真机验收 run 45184368115 的 APK：准备页几何/渐进、子页进入与返回的纯横向推送、头部向左退出/左侧返回、选项页卡片、权限页字重、深色与关模糊降级。
2. 就 §2.4 的底部遮罩强度给出结论；如需改动，只动遮罩 stops，不动采样/带几何。
3. 决定 §4.1 pin 合回 main 的时机（独立提交，不携带模糊功能）。
4. 补跑 `testUnstableDebugUnitTest` 与性能测量，再决定 squash 与合并 main。
# COUI 全屏渐进式模糊：当前问题与待决事项

- 日期：2026-11-23（2026-11-24 更新同页展开、COUI 可展开分组与设置下拉菜单；2026-11-25 记录审查修复与分割线规范修正）
- 源码基线：`main` @ `e3e0ce4b`（tag `26.11.1`，versionCode 1721），本篇所述内容均以该基线为参照。
- 工作分支：`feat/coui-fullscreen-progressive-blur`；本轮动画修复提交 `8df262f3`（取代失效的 `c7271f0b`）。2026-11-25 按 §4.2 的决定以 `--no-ff` 合入 main，不 squash。
- 文档性质：问题清单与待决事项；不是功能说明，也不替代 `COUI_FULLSCREEN_PROGRESSIVE_BLUR_REPORT_2026-11-23.md` 的适配方案。
- 验证状态：`./gradlew spotlessApply` 通过；CI `manual-build.yml`（Preview/debug）多次通过，动画修复最终 run `45184368115`、同页展开 run `45361904550`、分组与下拉菜单 run `45366699121`、菜单定宽与条目过渡 run `45370120203`、菜单右对齐 run `45373399590`、菜单宽度由内容固定 run `45378598067`、行撑满面板 run `45380923974` 通过；2026-11-25 Branch CI run `45615000396`（attempt 2）的单元测试与 Unstable Release 构建通过，Preview/debug run `45616845562` 通过，分割线修复后 Branch CI run `45625765879` 全绿；**无真机自动化验收**。本机无 Android SDK，编译验证只能走 CI。

## 1. 已落地且编译验证通过的部分

| 能力 | 位置 | 状态 |
| --- | --- | --- |
| 内容采样源 + 覆盖式外壳 | `CouiPositionFullScreen.kt` | 已落地；静态源供材质/弹出层，内容源供上下渐进带 |
| 顶部渐进带（状态栏＋顶栏＋头部整段，带主题遮罩） | `CouiPositionFullScreen.kt`、`Backdrop.kt CouiProgressiveEdgeBand` | 已落地；观感以 2dc8a1a7 为基准恢复 |
| 底部渐进带（仅有操作栏的页面） | `CouiPositionFullScreen.kt` | 已落地；无按钮页面不绘制 |
| 留白下沉（准备/批量结果） | 各 content 文件 | 已落地；居中状态页保留预留布局 |
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

- 模块日志、确认页、分包选择页、进度/完成/错误页、卸载/归档恢复：保留预留布局与居中语义（`StageBodyContent` 的 `else -> padding(top = topBleed, bottom = bottomBleed)`），顶部静态材质。
- 准备页顶部为真实渐进（正文滚过顶栏+头部后方）；准备页与批量结果底部为真实渐进。
- 权限列表与安装选项已不再作为独立子页；其内容并入准备页滚动后，顶部渐进改由准备页采样路径覆盖，详见 §2.5。

### 2.4 底部带在按钮区不透明度偏高（观感待复核）

底部带遮罩沿用顶部带的镜像 stops（浅色 `0 → 0.7@0.4 → 1`，深色 `0 → 0.4`，见 `Backdrop.kt` `CouiProgressiveEdgeBand`）。操作按钮位于带的不透明端附近，视觉上接近一条实色底栏。用户曾要求「无按钮页面去掉遮罩」（已实现：底部带以 `bottomBar != null` 为门），但有按钮页面的遮罩强度是否合适未确认。

### 2.5 权限列表与安装选项改为同页展开（已实现，待真机复核）

- `33668ff4`：准备页的“权限列表”和“安装选项”不再派发子页动作，改为在当前 `CouiSettingsCard` 内展开。
- `cebf582e`：展开区改用 COUI 自带的可展开分组 —— `ExpandableGroupHeader` + `ExpandableGroupContent`。头部是 COUI 的 `BasicComponent` 行（标题、摘要、可翻转箭头），展开区以 400ms 高度+透明度动画生长（曲线为 COUI `COUIMoveEaseInterpolator`），不再使用自定义 `AnimatedVisibility`。为与示例一致，三个入口不再带前置图标。
- 分包选择改用 COUI `ArrowPreference`，保持同一张卡片内的行高、字号与箭头样式一致；行为仍是跳转 `FullScreenSubPage.ExtendedMenu` 之外的原有子页路径。
- 权限内容新增 `inline = true`：复用原有分组与权限行，但不创建第二套垂直滚动，也不嵌套 `Card`。安装选项同样新增 `inline = true`，内嵌使用普通 `Column`，独立页仍保留原 `LazyColumn` 与卡片样式。
- 展开时按系统返回只收起展开区；`TransitionSlotGuard` 会把 `LocalFullScreenActionsEnabled` 设为非激活，避免旧主页在转场期间抢返回键。
- 验证：`spotlessApply` 通过；CI run `45361904550`（同页展开）与 `45366699121`（可展开分组 + 设置下拉菜单）Preview/debug 组装成功。**展开高度、长权限列表、深色与大字体下的观感未真机复核。**

### 2.6 右上角“安装器设置”改为 COUI 下拉菜单（已实现，待真机复核）

- `cebf582e`：准备页右上角不再跳转 `FullScreenSubPage.Settings`。图标按钮改为弹出 COUI 下拉菜单（`OverlayDropdownMenu` 家族），菜单锚定在该按钮上，选项即原设置页的开关：
  - 第一组：显示 SDK 版本、显示大小、安装后删除；OPPO/OnePlus 设备追加“显示 OEM 特殊信息”。
  - 第二组：显示文件路径、显示安装发起者。
- 每项以 `DropdownItem.selected` 表示开启状态，点击原地切换；菜单不因选中而关闭，允许连续调多项，点外部或再点图标关闭。
- `cebf582e` 先用官方 `OverlayIconDropdownMenu`；`31313145` 改为用同族底座自组：`OverlayListPopup(minWidth = 240.dp)` + `ListPopupColumn` + `DropdownImpl` + `DropdownGroupDivider`。原因是官方图标菜单不暴露宽度，而选中行才会画勾选列，会导致切换时菜单宽度跳动。现在每行都带 `stateIcon = COUIIcons.Basic.Check`，未选中行的 `submenuIndicatorColor` 设为透明——槽位仍占 24dp，勾不显示，最宽行不再随开关变化；面板由 `minWidth` 固定在 240dp（内容更宽的长文本语言下仍保持每语言恒定）。
- 切换开关后，信息卡片的新增/移除条目改为带过渡：新增行从 0 高度展开并把下方行推开，移除行收缩淡出后才停止绘制（`AnimatedDetailRows`，`MutableTransitionState` 逐行持有）。分隔线改为每行顶部一条，避免最后一行退出后留下尾部横线。
- 原设置页顶部的提示卡已移除，改为呼出菜单时弹一次 Toast，文案新增 `installer_temp_settings_toast`（中文“仅在本次会话中生效”，英文 “Only takes effect in this session”）。
- `FullScreenSubPage.Settings` 枚举与 `settingsContent` 参数一并删除；半屏 COUI 的 `PrepareSettingsContent` 与 `installer_temp_settings_tip` 保持原样。
- 弹窗宿主沿用外壳已有的 `CouiSheetPopupHost()`（`CouiPositionFullScreen` 根部），未新增宿主。
- `d0c83a7d` 把宽度从弹窗的 `minWidth` 约束改为由内容自身声明（`Box(Modifier.width(240.dp))` 包住 `ListPopupColumn`），避免宿主对最小约束的处理影响面板宽度；同时复核了截图实测：旧截图面板约 613px、页面左右留白 54px（=16dp，密度约 3.4），折合面板约 181dp，正是 COUI 默认 `PopupMinWidth = 168.dp` 的量级，说明那张截图来自定宽提交之前的构建。
- `264e0c4e` 修正菜单水平位置：COUI 默认的 `ListPopupDefaults.DropdownPositionProvider` 不读取 `alignment`，始终把菜单按锚点水平居中，240dp 面板挂在 40dp 按钮上时右缘会短约一个面板宽的四分之一。现在改用 `FullScreenSettingsMenuPositionProvider`（右缘对齐按钮右缘，保留 8dp 锚点间距、下方放不下则上方、最后夹进窗口）。窗口矩形被响应式边距内缩 16dp，而顶栏按钮位于 8dp 尾部内边距内，因此夹取上限同时取 `anchorBounds.right`，否则右缘仍会差 8dp。
- `223e0956` 修正“面板右侧无内容”：`ListPopupColumn` 用 `constraints.minWidth` 决定列宽，而 `Box` 默认不向下传递最小约束，于是面板被撑到 240dp、列却只有文字宽度，勾选与选中高亮停在面板中间。改为 `Box(width = 240.dp, propagateMinConstraints = true)` 后行宽 = 面板宽，勾选落到行尾。
- 同轮实测（用户 2026-11-24 截图，1264×2780，密度≈3.4）：面板右缘 ≈1219px，按“顶栏 8dp + 动作槽 8dp”推算的按钮右缘 ≈1210px，差值在软边误差内，说明右对齐与 240dp 定宽均已生效；问题只剩行未撑满。

### 2.7 2026-11-25 代码审查的三项缺陷（已修复，CI 通过，待真机复核）

审查对象为 `2de00733`，三项问题均由源码直接证实：

- `abd9f727`：`cebf582e` 删除 `FullScreenSubPage.Settings` 后，`CouiFullScreenStateTest` 仍引用该枚举值，`testUnstableDebugUnitTest` 会在 Kotlin 编译阶段失败并阻断 PR 检查。测试改为验证 Main、Permissions、ExtendedMenu 与不同安装阶段之间的 key 区分。此前本分支三次 Branch CI（`45373396014`、`45378591890`、`45380923301`）均因该问题在单元测试作业失败；修复后 run `45615000396`（attempt 2）单元测试作业 `BUILD SUCCESSFUL`，Gradle 日志未给出用例数。
- `5e7b2cd3`：外壳无条件预留「70dp + 导航栏高度」，但 `InstallChoice`、`Permissions`、`ExtendedMenu` 没有外壳操作栏，这三页会被额外压缩一段，关模糊时还与页面自身的导航栏留白叠加。预留值改为以 `bottomBar` 是否存在为准。这三页原本就自行处理导航栏（`navigationBarsPadding()` 或 `bottomInset`），不会因此压到导航栏上。
  - 已知取舍：预留值随 `bottomBar` 立即切换。进入无操作栏页面时，新页面从第一帧起就是正确几何；正在退出的准备页底部内边距会在退场过程中变小。若列表已滚到底，可能在退场瞬间位移。未做“等底栏淡出后再释放预留”，因为那会让已经就位的新页面在淡出结束时跳动。**真机未复核。**
- `9b2a30b3`：`AnimatedDetailRows` 用可见 `entries` 的下标插入仍含退场行的 `rows`，前面有隐藏行时新行位置错误（关「显示大小」「显示 SDK」后开「显示文件路径」，路径行排到「架构」之前）。新行改为插在其可见前驱之后。未补充顺序单元测试，列表状态仍封装在 composable 内。
- 验证：三次修改前后 `./gradlew spotlessCheck --no-configuration-cache` 均为 `BUILD SUCCESSFUL`，`git diff --check` 无输出。推送后 CI（`ec5a6a10`）：Branch CI run `45615000396` 的格式检查、单元测试、Unstable Release 构建通过（attempt 1 的单元测试作业因 runner 解析不到 KSP 插件 `2.3.13` 在配置阶段失败，重跑后通过，非代码问题）；Preview/debug run `45616845562` 通过。**三项修复的真机表现均未复核。**

### 2.8 2026-11-25 分割线规范的两处修正（已修复，CI 通过，待真机复核）

- `da2e0f3e` 准备页分割线右侧缺留白：详情卡内三处 `HorizontalDivider` 只写了 `padding(start = 16.dp)`，右端直接贴到卡片边缘，与条目文字自身 16dp 的内边距不对称。改为 `fillMaxWidth().padding(horizontal = 16.dp)`，沿用 `InstallChoiceContent` 里同一卡片族的既有写法。三处分别是详情行之间（`CouiFullScreenPrepareContent.kt:529`）、权限与安装选项两个可展开分组之间（`:387`）、「选择分包」上方（`:402`）。
- `da2e0f3e` 权限列表不该有行间分割线：`PermissionRows` 原本在每条权限后画一条 `HorizontalDivider`，与 COUI 权限列表的规范不符。删除行间分割线，仅保留 14dp 垂直间距，敏感与普通两组仍由分区标题区分。独立子页与准备页内联展开共用同一段 `PermissionRows`，两种模式一并生效。
- 已知边界（未在本轮处理）：详情行的分割线挂在 `AnimatedVisibility` 内随行收缩，且按 `rows` 下标判断 `index > 0`；首行离场时第二行会临时变成首行而多画一条线。该行为改动前即存在。
- 验证：`./gradlew spotlessCheck --no-configuration-cache` 通过（`BUILD SUCCESSFUL in 26s`）；Branch CI run `45625765879` 的 Code Style、Unit Tests、Unstable Release 构建通过；产出的 Unstable APK 经 `keytool` 验签为 `CN=InstallerX Revived`（RSA 4096，SHA384withRSA）。**两处改动的真机观感均未复核。**

## 3. 未验证项（验收缺口）

- 真机矩阵：浅色/深色/AMOLED/动态色/临时主题色；手势/三键导航；横屏与平板；大字体与显示缩放；API 26–32 与关模糊降级；Compose 预览。
- 行为回归：全屏关闭 220ms 时序与 ViewModel 1000ms 兜底、退出/过渡冻结快照、顶栏按钮 `SizeTransform(clip=false)`、装饰层不吞点击、半屏与 Material 3 家族、半屏 COUI 设置页（全屏侧已改为顶栏下拉菜单）、卸载与归档恢复、连续新安装与通知重入。
- 性能：内容源进入滚动/日志录制链路后的帧耗时分位数、卡顿帧、图层内存；长权限列表、高频模块日志、批量结果、页面交叉淡入淡出；60Hz 与高刷设备。**没有任何实测数字，不应引用改善幅度。**
- 单元测试：`testUnstableDebugUnitTest` 未在本分支跑过（CI manual-build 只组装）。既有 `InstallerFullscreenCloseTest`、`CouiFullScreenStateTest` 可作为回归入口；后者引用已删除枚举值的问题已在 §2.7 修正，并通过 Branch CI run `45615000396`。性能测量仍未做。
- 无 `app/src/androidTest`；不存在设备截图/录屏验收。

## 4. 仓库级待决事项

### 4.1 main 的 miuix pin 已失效（阻塞 main 编译）

上游已从 GitHub Packages 删除 `0.9.5-5ff41ba9-SNAPSHOT`（实测 `miuix-blur-android` 全部版本列表中不存在）。main 仍 pin 该版本，**任何分支含 main 都解析不到 miuix-blur/nav/material**。修复 `f82db3d4`（→ `0.9.5-d82b40fa-SNAPSHOT`）只在本分支。

- 原待决（2026-11-25 随合并消解）：是否将 `f82db3d4` 单独合回 main（与模糊功能解耦）。
  - 2026-11-25 更新：未单独拆出，随整条分支以 `--no-ff` 合入 main（§4.2）。main 因此恢复可解析 miuix，但这也意味着 pin 修复与全屏功能无法再分别回滚。
- 已核对：`d82b40fa` 上 `miuix-blur` 仍导出 `LayerBackdrop` / `ProgressiveBlur` / `progressiveTextureBlur`（签名一致），`miuix-material` 仍有 `RuntimeShader`。

### 4.2 分支历史含被取代的方案

12 个提交中，`2dc8a1a7` 的 glide 曾被 `5818183b` 取代、又在 `0b897e10` 恢复；`90782412`–`55c660cb` 的正文小带/每槽实验已整体撤销。

- 2026-11-25 已决：不 squash。分支相对 `e3e0ce4b` 共 42 个提交，保留完整历史并以 `--no-ff` 合并，与 main 上 `c24e91ae`（merge code-optimization-p0 into main）的先例一致。代价是 main 的 `versionCode`（取 `git rev-list --count HEAD`）会因合并提交再 +1，属可接受偏差。

### 4.3 文档同步

- 本报告与 `COUI_FULLSCREEN_PROGRESSIVE_BLUR_REPORT_2026-11-23.md` 均为未跟踪/新增文档；后者行号对应基线 `e3e0ce4b`，实施后已部分过时（外壳结构、头部归属、带几何），引用时需重新核对。
- `docs/README.md` 未描述全屏模糊视觉；若要把新视觉写成产品行为，需按 AGENTS.md 同步英文与简中说明。

## 5. 建议的下一步顺序

1. 真机验收（以 stable draft 的 APK 或 run `45625765879` 的 Unstable 包为准）：准备页几何/渐进、子页进入与返回的纯横向推送、头部向左退出/左侧返回、权限与安装选项按 COUI 可展开分组展开/收起、分包子页、右上角设置下拉菜单的定宽、右缘与按钮对齐、呼出 Toast、切换开关时信息条目的展开/收起过渡、深色与关模糊降级。
   - 本轮新增两项：详情卡分割线两端各 16dp 是否与条目文字两端对齐，连续开关「显示大小」「显示 SDK」「显示文件路径」后行序与分割线数量是否正确；权限详情与准备页内联展开的条目之间应无分割线，敏感/普通两组仍可辨。
2. 就 §2.4 的底部遮罩强度给出结论；如需改动，只动遮罩 stops，不动采样/带几何。
3. 确认合并后 main 的 miuix 解析与 stable 变体构建（由 stable release 流水线本身验证，见 §4.1）。
4. 性能测量仍未做；§2.7 与 §2.8 的改动均已通过 CI 单元测试与构建，真机表现待复核。

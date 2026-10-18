# 发布流程与签名密钥

本文说明如何在不泄露的前提下配置发布签名，以及如何产出/发布正式版本。
适用于 `.github/workflows/stable-apk-and-magisk.yml` 与
`.github/workflows/manual-stable-release.yml`。

---

## 1. 签名密钥只存在于两处

| 位置 | 用途 | 是否入库 |
| --- | --- | --- |
| 你的本机（`~/keys/…jks`） | 本地构建、备份 | 否 |
| GitHub 仓库 Secrets（4 个） | CI 构建发布版 | 否，加密存储、保存后不可读回 |

仓库里**永远不放** `.jks`、`keystore.properties`、base64 文本。`.gitignore` 已忽略
`*.jks`、`*.keystore`、`keystore.properties`、`keystore/`。

`app/build.gradle.kts` 的取值顺序是：`keystore.properties` → 环境变量
（`KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`）→ 都没有则回退
debug keystore。CI 走的是写 `keystore.properties` 那条路径。

---

## 2. 生成密钥（一次，之后长期复用）

不要用 `-storepass` / `-keypass` 传口令：那样口令会留在 shell 历史里。
省略这两个参数，`keytool` 会以无回显方式提示输入。

bash / zsh：

```bash
mkdir -p ~/keys
keytool -genkeypair -v \
  -keystore ~/keys/installerx-release.jks \
  -alias installerx \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12 \
  -dname "CN=InstallerX Revived, O=InstallerX, C=CN"
```

Windows PowerShell（`\` 是 bash 的续行符，PowerShell 必须改用反引号 `` ` ``，
且反引号必须是该行最后一个字符，后面不能有空格）：

```powershell
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'   # 未设 JAVA_HOME 时写 keytool 的绝对路径
New-Item -ItemType Directory -Force -Path "$HOME\keys" | Out-Null
& $keytool -genkeypair -v -keystore "$HOME\keys\installerx-release.jks" `
  -alias installerx -keyalg RSA -keysize 4096 -validity 10000 `
  -storetype PKCS12 `
  -dname "CN=InstallerX Revived, O=InstallerX, C=CN"
```

`keytool` 未加入 `PATH` 时会提示「不会被识别为 cmdlet」，此时用上面的绝对路径调用，
或把 `%JAVA_HOME%\bin` 加进用户环境变量。

PKCS12 密钥库不支持独立 key 口令，`keytool` 会忽略 `-keypass`，
所以 `SIGNING_KEY_PASSWORD` 与 `SIGNING_STORE_PASSWORD` 填同一个值。

**务必离线备份这个 `.jks` 和口令。** 丢了就无法覆盖升级已安装的 APK / 系统模块，
只能让用户卸载重装。

> 如果你要延续已有的发布（例如以前的 `com-android-packageinstaller` 模块），
> 必须使用**同一个** keystore，否则签名不一致，模块无法覆盖安装。

---

## 3. 转成 base64 并存为 Secrets

Linux / macOS：

```bash
base64 -w0 ~/keys/installerx-release.jks > keystore.b64   # macOS 用: base64 -i <file> -o keystore.b64
```

Windows PowerShell：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\keys\installerx-release.jks")) |
  Set-Content -Encoding ascii -NoNewline keystore.b64
```

写入仓库 Secrets（值不会被回显，保存后也无法再查看明文）：

```bash
gh secret set SIGNING_KEY_STORE_BASE64  < keystore.b64
gh secret set SIGNING_STORE_PASSWORD
gh secret set SIGNING_KEY_ALIAS
gh secret set SIGNING_KEY_PASSWORD
```

`gh` 未安装时：Windows 用 `winget install --id GitHub.cli -e`，然后 `gh auth login`。
已安装但提示「无法识别」，说明它不在 PATH：用绝对路径 `"$env:ProgramFiles\GitHub CLI\gh.exe"`
或重开终端。

本仓库有两个 remote（`origin` / `upstream`），`gh` 不带 `-R` 可能选中上游仓库，
所有写操作都要显式指定：`-R Srythm/InstallerX-Revived`。

或在网页端：`Settings → Secrets and variables → Actions → New repository secret`，
四个名字必须完全一致：

| Secret | 内容 |
| --- | --- |
| `SIGNING_KEY_STORE_BASE64` | 整个 `.jks` 的 base64（单行、无换行） |
| `SIGNING_STORE_PASSWORD` | keystore 口令 |
| `SIGNING_KEY_ALIAS` | 例如 `installerx` |
| `SIGNING_KEY_PASSWORD` | key 口令 |

写入完成后**立刻删除本地的 `keystore.b64`**（`shred -u keystore.b64` /
`Remove-Item keystore.b64`）。

---

## 4. 本地构建时怎么用（可选）

在仓库根目录建 `keystore.properties`（已被忽略，不会提交），指向仓库外的密钥：

```properties
storeFile=/home/you/keys/installerx-release.jks
storePassword=...
keyAlias=installerx
keyPassword=...
```

或用环境变量（CI 友好，不落盘）：`KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、
`KEY_ALIAS`、`KEY_PASSWORD`。

Windows 下 `storeFile` 用正斜杠或双反斜杠，例如 `C:/Users/you/keys/installerx-release.jks`。

---

## 5. 泄露边界与加固

- Secrets 只在 workflow 运行时注入，**fork 提交的 PR 拿不到**；但 `pull_request_target`
  这类工作流会把 secrets 暴露给不可信代码，仓库目前没有这种用法，不要引入。
- 建议把发布相关 secrets 放进 **Environment**（`Settings → Environments`），设置
  「仅允许 `main` 分支」+「需要审批」，这样其它分支的构建无法用它们签名。
- 不要在 workflow 里 `echo` 密钥；GitHub 会自动掩码，但日志仍可能间接泄露
  （例如 `base64` 片段、文件大小比较）。
- PAT（推送用）与 keystore 分开管理。当前本机 PAT 的 scope 是
  `gist, repo, workflow`，**没有 `read:packages`**，所以本地 Gradle 构建拉不到 miuix；
  如果要跑本地构建，另建一个带 `read:packages` 的 PAT 写进 `~/.gradle/gradle.properties`
  的 `gpr.user` / `gpr.key`。

---

## 6. 两种产出方式

### A. 只构建，不发布（日常自测）

`Actions → Stable APK & Magisk Module → Run workflow`

- `version_name` 留空 → 版本取最后提交日期（如 `26.10`）
- `build_sequence` 当天第一次填 `1`，同日再构建填 `2`
- `publish_release` 保持 `false`

产出两个 artifact：

```
InstallerX-Revived-<yy.MM>.apk
com-android-packageinstaller-<yy.MM>.<yyyyMMdd>.<seq>.zip
```

### B. 发布正式版（草稿）

同一个 workflow，把 `publish_release` 设为 `true`：

1. `publish` job 先校验 4 个签名 secrets 是否齐全，缺任何一个直接失败（避免把 debug
   签名的包发成正式版）。
2. 下载两个 artifact，确认都存在。
3. 建一个 **draft** release：tag = `<yy.MM>`（与上游 stable 一致），附件是 APK + 模块 zip。
4. 到 `Releases` 页面补 changelog，确认无误后点 **Publish**。

注意：

- 该 tag 已存在时发布会失败——正式版本号必须递增（用 `version_name` 输入覆盖）。
- 只有 `publish_release=true` 才写 `contents: write`；日常构建权限仍是只读。
- 上游的 `Manual Stable Release` 仍然可用，但它只产出 APK，不包含模块。

---

## 7. 版本号规则

| 产物 | 格式 | 示例 |
| --- | --- | --- |
| Stable APK | `<yy.MM>` | `26.10` |
| Magisk 模块 | `<yy.MM>.<yyyyMMdd>.<seq>` | `26.10.20261017.1` |
| versionCode | git 提交数 | `1622` |

模块内部 APK 用 `-PVERSION_NAME=<模块版本>` 构建，所以 `module.prop` 与 APK 的
versionName 永远一致；workflow 里有一致性断言，不一致会直接失败。

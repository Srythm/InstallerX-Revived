import org.gradle.api.Project
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BuildConfig {
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 0
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    const val JDK_VERSION = 25

    const val VERSION_CODE = 520
}

// Get git commit count safely, compatible with configuration cache
fun Project.getGitCommitCount(): Int {
    return try {
        providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim().toInt()
    } catch (_: Exception) {
        BuildConfig.VERSION_CODE
    }
}

// Get git commit hash safely, compatible with configuration cache
fun Project.getGitHash(): String {
    return try {
        providers.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        "unknown"
    }
}

// Get the date of the latest commit directly formatted as yy.MM
fun Project.getGitDate(): String {
    return try {
        providers.exec {
            commandLine("git", "log", "-1", "--format=%cd", "--date=format:%y.%m")
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        // Fallback to current date if git command fails
        LocalDate.now().format(DateTimeFormatter.ofPattern("yy.MM"))
    }
}

// Combine the manual version name or dynamic git date, optionally with a
// patch suffix. Upstream stable releases use a `yy.MM[.PATCH]` scheme
// (e.g. `26.05.01` is the first patch of the 26.05 stable), so we let
// build invocations append an optional PATCH component to the auto-derived
// date-based version name.
//
// Resolution order:
//   1. `-PVERSION_NAME=...` wins outright and is returned as-is.
//   2. `-PVERSION_PATCH=...` is appended to the date-based version
//      (`yy.MM.PATCH`).
//   3. Otherwise we fall back to the date-based `yy.MM`.
//
// Build-level flavors (`Unstable` / `Preview`) still append
// `.<short-githash>` to whatever base we return here, so a Stable
// `-PVERSION_PATCH=01` build resolves to `26.06.01` and an Unstable build
// at the same commit resolves to `26.06.01.abc1234`.
fun Project.getBaseVersionName(): String {
    val manualVersionName = findProperty("VERSION_NAME") as String?
    if (!manualVersionName.isNullOrBlank()) return manualVersionName
    val patch = findProperty("VERSION_PATCH") as String?
    val baseDate = getGitDate()
    return if (!patch.isNullOrBlank()) "$baseDate.$patch" else baseDate
}

package app.beetle.patches.slowly.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

object Constants {
    val COMPATIBILITY_SLOWLY = Compatibility(
        name = "Slowly",
        packageName = "com.slowlyapp",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFF5A5F,
        targets = listOf(
            AppTarget(
                version = "9.5.6",
                versionCodes = mapOf(SupportedAbi.ARM64_V8A to 905062),
            ),
        ),
    )
}

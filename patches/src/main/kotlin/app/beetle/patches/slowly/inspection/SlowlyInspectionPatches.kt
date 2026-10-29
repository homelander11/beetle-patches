package app.beetle.patches.slowly.inspection

import app.beetle.patches.slowly.shared.Constants.COMPATIBILITY_SLOWLY_INSPECTION
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val NETWORK_CONFIG_NAME = "beetle_network_security_config"

@Suppress("unused")
val slowlyTrustUserCertificatesPatch = resourcePatch(
    name = "Slowly trust user certificates",
    description = "Allows HTTPS inspection using an installed user CA certificate while retaining certificate and hostname validation and blocking cleartext traffic. Does not bypass native Pairip protection.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SLOWLY_INSPECTION)

    execute {
        val config = get("res/xml/$NETWORK_CONFIG_NAME.xml")
        if (config.exists()) {
            throw PatchException("The Slowly inspection network configuration already exists.")
        }

        document("AndroidManifest.xml").use { manifest ->
            val applications = manifest.getElementsByTagName("application")
            if (applications.length != 1) {
                throw PatchException("Expected one Slowly application element.")
            }
            val application = applications.item(0) as Element
            if (application.hasAttributeNS(ANDROID_NAMESPACE, "networkSecurityConfig")) {
                throw PatchException("Slowly already defines a network security configuration; refusing to replace it.")
            }
            application.setAttributeNS(
                ANDROID_NAMESPACE,
                "android:networkSecurityConfig",
                "@xml/$NETWORK_CONFIG_NAME",
            )
            application.setAttributeNS(ANDROID_NAMESPACE, "android:usesCleartextTraffic", "false")
        }

        config.parentFile.mkdirs()
        config.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <network-security-config>
                <base-config cleartextTrafficPermitted="false">
                    <trust-anchors>
                        <certificates src="system" />
                        <certificates src="user" />
                    </trust-anchors>
                </base-config>
            </network-security-config>
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val slowlyDisableDeviceInspectionChecksPatch = bytecodePatch(
    name = "Slowly disable device inspection checks",
    description = "Suppresses JailMonkey root, hook, and ADB detection results for inspection. Does not disable Pairip, VPN checks, or TLS certificate validation.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SLOWLY_INSPECTION)

    execute {
        // Resolve all three targets before modifying any method.
        val methods = listOf(
            JailMonkeyRootCheckFingerprint.method,
            JailMonkeyHookCheckFingerprint.method,
            JailMonkeyAdbCheckFingerprint.method,
        )
        methods.forEach { method ->
            method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        }
    }
}

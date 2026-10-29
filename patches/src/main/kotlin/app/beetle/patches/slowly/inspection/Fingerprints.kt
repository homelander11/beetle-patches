package app.beetle.patches.slowly.inspection

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// JailMonkey's public SDK API names are retained in Slowly 10.0.5.
// Match those APIs rather than their obfuscated internal helpers.
object JailMonkeyRootCheckFingerprint : Fingerprint(
    definingClass = "Lcom/gantix/JailMonkey/JailMonkeyModule;",
    name = "isJailBroken",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

object JailMonkeyHookCheckFingerprint : Fingerprint(
    definingClass = "Lcom/gantix/JailMonkey/JailMonkeyModule;",
    name = "hookDetected",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

object JailMonkeyAdbCheckFingerprint : Fingerprint(
    definingClass = "Lcom/gantix/JailMonkey/JailMonkeyModule;",
    name = "AdbEnabled",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

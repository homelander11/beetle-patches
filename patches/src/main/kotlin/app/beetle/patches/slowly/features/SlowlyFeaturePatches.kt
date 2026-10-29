package app.beetle.patches.slowly.features

import app.beetle.patches.slowly.shared.Constants.COMPATIBILITY_SLOWLY
import app.beetle.patches.slowly.shared.Constants.COMPATIBILITY_SLOWLY_AVATAR
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import java.security.MessageDigest

private const val BUNDLE_PATH = "assets/index.android.bundle"
private const val BUNDLE_SIZE_9_5_6 = 8_471_052
private const val BUNDLE_SIZE_9_5_8 = 8_446_148
private const val BUNDLE_SIZE_10_0_5 = 9_620_668
private const val HBC_VERSION_OFFSET = 8
private const val HBC_FILE_LENGTH_OFFSET = 32
private const val HBC_FOOTER_SIZE = 20

private val HBC_MAGIC = byteArrayOf(
    0xC6.toByte(), 0x1F, 0xBC.toByte(), 0x03,
    0xC1.toByte(), 0x03, 0x19, 0x1F,
)
private val HBC_VERSION_96 = byteArrayOf(0x60, 0x00, 0x00, 0x00)

private data class HermesEdit(
    val name: String,
    val offset: Int,
    val expected: ByteArray,
    val replacement: ByteArray,
)

private data class HermesBundleSpec(
    val size: Int,
    val edits: List<HermesEdit>,
)

private fun roleThresholdEdit(name: String, offset: Int, register: Int) = HermesEdit(
    name = name,
    offset = offset,
    expected = byteArrayOf(0x6E, register.toByte(), 0x14),
    replacement = byteArrayOf(0x6E, register.toByte(), 0x00),
)

private fun ByteArray.hex() = joinToString(" ") {
    it.toInt().and(0xFF).toString(16).uppercase().padStart(2, '0')
}

private fun requireBytes(bytes: ByteArray, offset: Int, expected: ByteArray, name: String) {
    val endOffset = offset + expected.size
    if (offset !in bytes.indices || endOffset > bytes.size) {
        throw PatchException(
            "$name at offset 0x${offset.toString(16).uppercase()} is outside the Slowly bundle.",
        )
    }

    val actual = bytes.copyOfRange(offset, endOffset)
    if (!actual.contentEquals(expected)) {
        throw PatchException(
            "$name precondition failed at offset 0x${offset.toString(16).uppercase()}: " +
                "expected ${expected.hex()}, found ${actual.hex()}.",
        )
    }
}

private fun verifyHermesBundle(bytes: ByteArray, expectedSize: Int) {
    if (bytes.size != expectedSize) {
        throw PatchException(
            "Slowly bundle size mismatch: expected $expectedSize bytes, found ${bytes.size}.",
        )
    }

    requireBytes(bytes, 0, HBC_MAGIC, "Hermes magic")
    requireBytes(bytes, HBC_VERSION_OFFSET, HBC_VERSION_96, "Hermes bytecode version")
    requireBytes(
        bytes,
        HBC_FILE_LENGTH_OFFSET,
        ByteArray(4) { index -> (expectedSize ushr (index * 8)).toByte() },
        "Hermes file length",
    )
    requireBytes(bytes, bytes.size - HBC_FOOTER_SIZE, hermesFileHash(bytes), "Hermes SHA-1 footer")
}

private fun hermesFileHash(bytes: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-1").run {
        update(bytes, 0, bytes.size - HBC_FOOTER_SIZE)
        digest()
    }

private fun updateHermesFooter(bytes: ByteArray) {
    // Hermes hashes all bytes preceding its 20-byte footer. Refresh after each
    // patch so both feature patches can be applied to the same valid bundle.
    hermesFileHash(bytes).copyInto(bytes, bytes.size - HBC_FOOTER_SIZE)
}

private fun applyHermesEdits(bytes: ByteArray, edits: List<HermesEdit>) {
    // Validate every site before changing any byte so an incompatible bundle fails atomically.
    edits.forEach { edit ->
        requireBytes(bytes, edit.offset, edit.expected, edit.name)
    }
    edits.forEach { edit ->
        edit.replacement.copyInto(bytes, edit.offset)
    }
}

private val EXPLORE_FILTER_EDITS_9_5_6 = listOf(
    roleThresholdEdit("zodiac filter gate", 0x755324, 5),
    roleThresholdEdit("gender filter gate", 0x7553B7, 4),
    roleThresholdEdit("city filter gate", 0x75623D, 1),
    roleThresholdEdit("saved New-user restoration gate", 0x7579AA, 8),
    roleThresholdEdit("saved city coordinates filter gate", 0x7579E8, 6),
    roleThresholdEdit("shared Last-online-within-48-hours/New-Members-Only render gate", 0x757EEB, 0),
    roleThresholdEdit("excluded-country limit gate", 0x75D261, 5),
    roleThresholdEdit("excluded-topic limit gate", 0x7C6D4E, 2),
)

private val EXPLORE_FILTER_EDITS_9_5_8 = listOf(
    roleThresholdEdit("zodiac filter gate", 0x74FE3E, 5),
    roleThresholdEdit("gender filter gate", 0x74FED1, 4),
    roleThresholdEdit("city filter gate", 0x750D53, 1),
    roleThresholdEdit("saved New-user restoration gate", 0x7524C0, 8),
    roleThresholdEdit("saved city coordinates filter gate", 0x7524FE, 6),
    roleThresholdEdit("shared Last-online-within-48-hours/New-Members-Only render gate", 0x752A01, 0),
    roleThresholdEdit("excluded-country limit gate", 0x757CF8, 5),
    roleThresholdEdit("excluded-topic limit gate", 0x7C157D, 2),
)

// Hermes v96 instruction boundaries verified in the supplied 10.0.5 bundle.
// Keep the render, saved-state, and selection checks in sync with each other.
private val EXPLORE_FILTER_EDITS_10_0_5 = listOf(
    roleThresholdEdit("zodiac filter gate", 0x8612C5, 5),
    roleThresholdEdit("gender filter gate", 0x861358, 3),
    roleThresholdEdit("required-language/topic selection gate", 0x86188E, 1),
    roleThresholdEdit("city filter gate", 0x86213E, 1),
    roleThresholdEdit("saved required-language restoration gate", 0x863877, 9),
    roleThresholdEdit("saved required-topic restoration gate", 0x8638BF, 9),
    roleThresholdEdit("saved New-user restoration gate", 0x86390E, 9),
    roleThresholdEdit("saved city coordinates filter gate", 0x86394C, 7),
    roleThresholdEdit("updated zodiac state gate", 0x863D18, 5),
    roleThresholdEdit("updated gender state gate", 0x863D68, 4),
    roleThresholdEdit("shared Last-online-within-48-hours/New-Members-Only render gate", 0x863F15, 0),
    roleThresholdEdit("excluded-country selection limit gate", 0x867722, 5),
    roleThresholdEdit("excluded-country displayed limit gate", 0x868403, 0),
    roleThresholdEdit("excluded-topic limit gate", 0x8D5F74, 2),
)

private val EXPLORE_FILTER_BUNDLE_SPECS = listOf(
    HermesBundleSpec(BUNDLE_SIZE_9_5_6, EXPLORE_FILTER_EDITS_9_5_6),
    HermesBundleSpec(BUNDLE_SIZE_9_5_8, EXPLORE_FILTER_EDITS_9_5_8),
    HermesBundleSpec(BUNDLE_SIZE_10_0_5, EXPLORE_FILTER_EDITS_10_0_5),
)

private fun exploreFilterBundleSpec(bytes: ByteArray): HermesBundleSpec =
    EXPLORE_FILTER_BUNDLE_SPECS.firstOrNull { it.size == bytes.size }
        ?: throw PatchException(
            "Slowly Explore filters bundle size mismatch: expected " +
                EXPLORE_FILTER_BUNDLE_SPECS.joinToString(" or ") { "${it.size}" } +
                " bytes, found ${bytes.size}.",
        )

private val AVATAR_BUILDER_EDITS_9_5_6 = listOf(
    roleThresholdEdit("avatar item selection gate", 0x5FE3D0, 6),
    roleThresholdEdit("avatar tab selection gate", 0x5FE8EC, 2),
    roleThresholdEdit("avatar item lock-rendering gate", 0x5FF21E, 3),
    roleThresholdEdit("avatar paywall-button gate", 0x600A94, 33),
)

private val AVATAR_BUILDER_EDITS_9_5_8 = listOf(
    roleThresholdEdit("avatar item selection gate", 0x5FBBE8, 6),
    roleThresholdEdit("avatar tab selection gate", 0x5FC104, 2),
    roleThresholdEdit("avatar item lock-rendering gate", 0x5FCA4C, 3),
    roleThresholdEdit("avatar paywall-button gate", 0x5FE2C2, 33),
)

private val AVATAR_BUILDER_EDITS_10_0_5 = listOf(
    roleThresholdEdit("avatar local paywall gate", 0x6B5DF9, 13),
    roleThresholdEdit("avatar item selection gate", 0x6B76E4, 7),
    roleThresholdEdit("avatar tab selection gate", 0x6B7B4A, 4),
    roleThresholdEdit("avatar item lock-rendering gate", 0x6B8213, 2),
    roleThresholdEdit("avatar item renderer subscription flag", 0x6B8351, 8),
    roleThresholdEdit("avatar saved-tab restoration gate", 0x6B89B8, 3),
    roleThresholdEdit("avatar paywall-button gate", 0x6B9CD3, 37),
)

private val AVATAR_BUILDER_BUNDLE_SPECS = listOf(
    HermesBundleSpec(BUNDLE_SIZE_9_5_6, AVATAR_BUILDER_EDITS_9_5_6),
    HermesBundleSpec(BUNDLE_SIZE_9_5_8, AVATAR_BUILDER_EDITS_9_5_8),
    HermesBundleSpec(BUNDLE_SIZE_10_0_5, AVATAR_BUILDER_EDITS_10_0_5),
)

private fun avatarBuilderBundleSpec(bytes: ByteArray): HermesBundleSpec =
    AVATAR_BUILDER_BUNDLE_SPECS.firstOrNull { it.size == bytes.size }
        ?: throw PatchException(
            "Slowly Avatar Builder bundle size mismatch: expected " +
                AVATAR_BUILDER_BUNDLE_SPECS.joinToString(" or ") { "${it.size}" } +
                " bytes, found ${bytes.size}.",
        )

@Suppress("unused")
val slowlyExtendedExploreFiltersPatch = rawResourcePatch(
    name = "Slowly extended Explore filters",
    description = "Enables verified client-side Explore filters, including Last online within 48 hours and New Members Only, and raises excluded-country and excluded-topic selection limits.",
) {
    compatibleWith(COMPATIBILITY_SLOWLY)

    execute {
        val bundle = get(BUNDLE_PATH)
        val bytes = bundle.readBytes()
        val originalSize = bytes.size
        val bundleSpec = exploreFilterBundleSpec(bytes)

        verifyHermesBundle(bytes, bundleSpec.size)
        applyHermesEdits(bytes, bundleSpec.edits)
        updateHermesFooter(bytes)
        bundle.writeBytes(bytes)

        if (bytes.size != originalSize || bundle.length() != originalSize.toLong()) {
            throw PatchException("Slowly Explore-filter patch changed the Hermes bundle size.")
        }
    }
}

@Suppress("unused")
val slowlyAvatarBuilderItemsPatch = rawResourcePatch(
    name = "Slowly avatar builder items",
    description = "Enables Avatar Builder items marked as subscription-only without changing coin-priced item checks.",
) {
    compatibleWith(COMPATIBILITY_SLOWLY_AVATAR)

    execute {
        val bundle = get(BUNDLE_PATH)
        val bytes = bundle.readBytes()
        val originalSize = bytes.size
        val bundleSpec = avatarBuilderBundleSpec(bytes)

        verifyHermesBundle(bytes, bundleSpec.size)
        applyHermesEdits(bytes, bundleSpec.edits)
        updateHermesFooter(bytes)
        bundle.writeBytes(bytes)

        if (bytes.size != originalSize || bundle.length() != originalSize.toLong()) {
            throw PatchException("Slowly Avatar Builder patch changed the Hermes bundle size.")
        }
    }
}

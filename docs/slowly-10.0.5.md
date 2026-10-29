# Slowly 10.0.5

The 10.0.5 targets were verified against the supplied ARM64 antisplit APK,
`com.slowlyapp`, version code `1000051`. That APK is debug-signed, rather than an
untouched store-signed download. Its SHA-256 is
`4009c09057241c3e667c06afa4d363a0111e260ef75873a56dc1f7c120a73627`.
The Hermes v96 bundle is 9,620,668 bytes. Other packages with the same version
label can contain different bytecode and may fail the patch preconditions.
All 10.0.5 targets are marked **experimental**: patch application and output
integrity are verified, but startup is blocked on both tested devices. The
existing 9.5.6/9.5.8 target flags are unchanged.

## Feature patches

The Avatar Builder and Explore patches retain their existing 9.5.6/9.5.8 target
maps. Their new 10.0.5 maps use instruction boundaries verified in the Hermes
disassembly, with expected opcode/register bytes checked before any edit.
Both patches preserve the bundle length, validate its header and SHA-1 footer,
and refresh the footer after editing so they can be applied together. The
footer layout is defined by [Hermes's bytecode format](https://github.com/meta/hermes/blob/main/include/hermes/BCGen/HBC/BytecodeFileFormat.h).

| Patch | Verified 10.0.5 changes |
|---|---|
| Slowly avatar builder items | Seven local role checks covering selection, lock display, renderer state, saved tabs, and the subscription paywall button. Coin prices, purchase status, and owned-item checks remain in place. |
| Slowly extended Explore filters | Fourteen local role checks covering filter selection, required languages/topics, saved state, prop updates, and exclusion limits/display. |
| Slowly Pairip license check bypass | The stable `LicenseClient.checkLicense(Context)` entry point is still present. This patch skips that entry point only. |

These are client-side changes. They do not alter an account's server-side role,
billing records, coin balance, or server acceptance of filter requests.

## Optional inspection patches

Both inspection patches are **off by default** and currently support only the
verified 10.0.5 ARM64 target.

- **Slowly trust user certificates** adds an Android Network Security
  Configuration that trusts system CAs and CAs installed in Android's user
  credential store. Install the inspection proxy's CA as a user CA before using
  it. Certificate-chain and hostname validation remain enabled, and cleartext
  traffic remains blocked. This follows Android's documented
  [custom trust-anchor configuration](https://developer.android.com/privacy-and-security/security-config#TrustingAdditionalCas).
- **Slowly disable device inspection checks** changes the JailMonkey bridge's
  `isJailBroken()`, `hookDetected()`, and `AdbEnabled()` results to false. It does
  not change mock-location results, VPN handling, or Pairip's native code.

The examined DEX contains OkHttp's certificate-pinner class, but no active pin
set was established. No speculative certificate-pinner bypass or trust-all
manager is included. The user-CA patch addresses platform CA trust; it does not
promise to bypass custom native TLS implementations or all traffic defenses.

## Apply all five patches

The validation run used the following command from the Morphe workspace root.
The original three patches are enabled by default; `-e` explicitly enables the
two inspection patches. Replace the MPP filename with your built bundle's
version when needed. The local signing keystore is not part of the repository.

```powershell
java -jar morphe-cli.jar patch `
  -p beetle-patches/patches/build/libs/patches-1.0.3.mpp `
  --keystore Morphe.keystore `
  -e "Slowly trust user certificates" `
  -e "Slowly disable device inspection checks" `
  -r analysis/slowly/builds/slowly_10.0.5-inspection-report.json `
  -o analysis/slowly/builds/slowly_10.0.5-inspection.apk `
  analysis/slowly/apk/slowly_10.0.5.apk
```

To apply just the three feature/license patches, omit both `-e` arguments.

## Validation and known limitation

The updated bundle builds successfully. All five patches applied to the exact
10.0.5 APK without forcing compatibility or continuing after errors. The patch
catalog registers both inspection patches as `default: false`.

Output verification confirmed:

- Exactly 21 changed Hermes code bytes and a refreshed 20-byte SHA-1 footer;
  the 9,620,668-byte length and all other bundle bytes are unchanged.
- A valid footer and successful Hermes parse of 39,473 functions and 77,969
  strings, including all 21 edited instruction boundaries.
- Early returns in the Pairip license entry point and the three JailMonkey
  bridge methods.
- A compiled manifest referencing the new system/user-CA configuration, with
  cleartext traffic disabled.
- All 39 native `.so` libraries are unchanged.
- Successful APK v2/v3 signature verification and installation on both tested
  devices.

Both the supplied APK and the five-patch output fail during startup on both
authorized test devices:

| Device | Matching baseline and patched behavior |
|---|---|
| Android 15 x86_64 emulator with Houdini ARM64 translation | `SIGSEGV` in `libpairipcore.so` at relative PC `0x43b90`, followed by a Houdini crash. |
| Samsung SM-A156E, Android 16, native ARM64 | Redirects to the Play Store and exits after about one second, with native status 11 in `libpairipcore.so` at relative PC `0x43c0c`. |

The native startup failure is therefore not limited to ARM translation.
Pairip virtualizes app startup; the license-entry patch does not bypass that
native protection. The underlying startup cause remains unresolved. Neither
feature interaction nor HTTPS proxy capture has been verified in a running
app. No account, messages, purchases, or paid server features were tested.

A narrow experiment that forced Pairip's Java signature-match helper to return
true was also built, applied, and checked in the output DEX. It produced the
same Samsung native crash without a Java signature-rejection error. That
experiment was reverted and is not included in the five published patches;
native VM and application initialization remain intact.

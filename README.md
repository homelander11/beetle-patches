# Beetle Patches

Custom Morphe patches for multiple Android apps.

## Add to Morphe Manager

[Add Beetle Patches to Morphe Manager](https://morphe.software/add-source?github=homelander11/beetle-patches)

## Patches

<!-- PATCHES_START EXPANDED -->
> **[v1.0.3](https://github.com/homelander11/beetle-patches/releases/tag/v1.0.3)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;5 patches total
<details open>
<summary>📦 Slowly&nbsp;&nbsp;•&nbsp;&nbsp;5 patches</summary>
<br>

**🎯 Supported versions:**

| 9.5.6 | 9.5.8 | 🧪&nbsp;10.0.5 |
| :---: | :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Slowly Pairip license check bypass](#slowly-pairip-license-check-bypass) | Disables Pairip's client-side installer and license enforcement. |  |
| [Slowly avatar builder items](#slowly-avatar-builder-items) | Enables Avatar Builder items marked as subscription-only without changing coin-priced item checks. |  |
| [Slowly disable device inspection checks](#slowly-disable-device-inspection-checks) | Suppresses JailMonkey root, hook, and ADB detection results for inspection. Does not disable Pairip, VPN checks, or TLS certificate validation. |  |
| [Slowly extended Explore filters](#slowly-extended-explore-filters) | Enables verified client-side Explore filters, including Last online within 48 hours and New Members Only, and raises excluded-country and excluded-topic selection limits. |  |
| [Slowly trust user certificates](#slowly-trust-user-certificates) | Allows HTTPS inspection using an installed user CA certificate while retaining certificate and hostname validation and blocking cleartext traffic. Does not bypass native Pairip protection. |  |

</details>

<!-- PATCHES_END -->

## Development

Build the patch bundle with:

```shell
./gradlew buildAndroid
```

The generated MPP is written to `patches/build/libs/`.

Development happens on `dev`; verified changes are merged to `main` for stable releases.

See [Slowly 10.0.5 support and inspection notes](docs/slowly-10.0.5.md) for the
verified target, optional traffic-inspection patches, and device-test limitations.

## License

Beetle Patches is licensed under the [GNU General Public License v3.0](LICENSE).

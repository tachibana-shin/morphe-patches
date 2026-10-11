package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

object Constants {
    /**
     * APKPure app store.
     *
     * The icon background is plain white (#FEFEFE); the declared colour is the
     * green brand accent that dominates the glyph so entries stay recognisable
     * in Morphe Manager.
     */
    val COMPATIBILITY_APKPURE = Compatibility(
        name = "APKPure",
        packageName = "com.apkpure.aegon",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x00C862,
        targets = listOf(
            // Fully analysed target. Every fingerprint in the ad patches below
            // was written against this build.
            AppTarget(
                version = "3.20.7902"
            )
        )
    )

    /**
     * Zepp (formerly Amazfit), the companion app for Huami/Xiaomi wearables.
     *
     * Unlike APKPure this app ships no third party advertising at all: the only
     * ad related code is Huami's own dormant splash/launch scaffold
     * (`com.huami.ad`, seven classes) behind an AdMob app id that is declared
     * in the manifest without the AdMob SDK ever being bundled. There is
     * therefore nothing for an ad patch to remove.
     *
     * What the app does ship instead is an in house telemetry pipeline plus
     * native anti tamper probes, which is what the telemetry patch targets.
     *
     * The icon background is the plain blue of the adaptive icon layer
     * (#0154D0); the foreground glyph is white.
     *
     * Declared as XAPK because the release is split: the base APK
     * (com.huami.watch.hmwatchmanager.apk, code only) is paired with an ABI
     * split (config.armeabi_v7a.apk) that carries all 132 native libraries.
     * Only armeabi-v7a is published, so there is no arm64 variant to declare.
     */
    val COMPATIBILITY_ZEPP = Compatibility(
        name = "Zepp",
        packageName = "com.huami.watch.hmwatchmanager",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x0154D0,
        targets = listOf(
            // Fully analysed target. Every fingerprint in the telemetry patch
            // was written against this build.
            AppTarget(
                version = "10.9.0-play",
                versionCodes = mapOf(
                    SupportedAbi.ARMEABI_V7A to 151983
                )
            )
        )
    )

    val COMPATIBILITY_EXAMPLE = Compatibility(
        name = "XYZ app", // App name as it appears in the Android launcher.
        packageName = "com.example.app",
        //
        // IMPORTANT: This declaration must match the file type on APKMirror/UpToDown.
        //
        // If this is declared as one type such as APK, but the file type on APKMirror/UpToDown
        // is different such as APKM or XAPK, then Morphe Manager will _not_ send the user
        // to APKMirror/UpToDown.
        apkFileType = ApkFileType.APK,
        // Icon color in Morphe Manager. Should be the background or primary color of the app icon.
        appIconColor = 0xFF0045,
        targets = listOf(
            // "version = null" means the patch works with the latest app target
            // and is expected to work with all future app targets.
            //
            // It is highly recommended to always include the exact app version you used to develop
            // your patches so users have a version 100% confirmed as working.
            //
            // It is highly preferred to use app versions that are
            // available on apkmirror.com or uptodown.com, as Morphe web-search will
            // redirect users to these sites. If an app version is not available
            // on ApkMirror or UpToDown, then the user is sent to Google search.
            AppTarget(
                version = "2.0.0"
            ),
            AppTarget(
                version = "1.0.2"
            )
        )
    )

    val COMPATIBILITY_EXAMPLE_2 = Compatibility(
        name = "XYZ app",
        packageName = "com.example.app",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x00FF45, // Icon color in Morphe Manager. Usually the same color as the icon background.
        targets = listOf(
            // 'any' version supported experimentally.
            AppTarget(
                version = null,
                isExperimental = true
            ),
            // App version confirmed 100% working.
            AppTarget(
                version = "1.0.2"
            )
        )
    )

    // Version code restriction.
    // Required for certain apps that can have multiple architecture releases with the same
    // version name (1.0.1) but different version codes (584009457).
    val COMPATIBILITY_EXAMPLE_3 = Compatibility(
        name = "XYZ app",
        packageName = "com.example.app",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x00FF45,
        targets = listOf(
            AppTarget(
                version = "1.0.5",
                // Required version code. If the user tries to patch a different version code
                // then Manager will warn they have the wrong original file.
                //
                // This declaration is only required when multiple architecture releases have the
                // same version code but only 1 specific version code works or has been well tested.
                // If there is only 1 release for each architecture file type, then it's best
                // not to declare this.
                //
                // If you are unsure, then do not include a version code declaration.
                versionCodes = mapOf(
                    SupportedAbi.ARM64_V8A to 584009457,
                    SupportedAbi.ARMEABI_V7A to 584119423
                )
            )
        )
    )
}

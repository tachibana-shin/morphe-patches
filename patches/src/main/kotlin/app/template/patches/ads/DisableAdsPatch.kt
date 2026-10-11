package app.template.patches.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_APKPURE

/**
 * Disable ads in APKPure.
 *
 * The app stacks its advertising behind three layers, and this patch short
 * circuits every one of them:
 *
 *  1. **Ad bootstrap.** `AdSdkInitFingerprint` marks the method that loads the
 *     cached `adConfig`, wires the splash helper, registers the ad lifecycle
 *     callbacks and instantiates the TopOn service. Returning right after the
 *     app-wide activity registration keeps unrelated behaviour intact while
 *     nothing ad related is ever set up.
 *
 *  2. **Downloaded mediation plugin.** The AdMob, Pangle, Vungle, Bigo, Unity,
 *     ironSource, InMobi, AppLovin, Mbridge, AdColony and Fyber adapters are
 *     *not* in the APK. They arrive in `plugin_topon2.zip` from
 *     `wj-static-eo.winudf.com` and are loaded through Tencent Shadow. Skipping
 *     that request keeps those SDKs out of the process altogether; the
 *     continuation callback is still invoked so the caller resumes normally.
 *
 *  3. **ApicFast native SDK.** Blocked before `SDK.init`, so the packed
 *     `libapi_cfas.so` (ijiami VMP) and its encrypted `api_cfas.VData` payload
 *     are never loaded.
 *
 * Splash, native card and server side (RTB) ad loading are disabled on top of
 * that, so a stale `adConfig` cannot resurrect an ad through another path.
 */
@Suppress("unused")
val disableAdsPatch = bytecodePatch(
    name = "Disable ads",
    description = "Disables every advertising SDK bundled with the app.",
    default = true
) {
    compatibleWith(COMPATIBILITY_APKPURE)

    execute {
        // Ad bootstrap: bail out right before the mediation service is looked up,
        // so everything ad related is skipped while the earlier side effects
        // (notably the app-wide activity registration) still run.
        AdSdkInitFingerprint.let {
            it.method.addInstructions(
                it.instructionMatches[2].index,
                """
                    return-void
                """
            )
        }

        // TopOn plugin: run the continuation callback instead of downloading
        // and starting the mediation plugin.
        TopOnPluginLoaderFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual {p1}, Lcom/apkpure/aegon/reshub/l;->run()V

                return-void
            """
        )

        // ApicFast: never call SDK.init, so no native ad VM is loaded.
        AppicFastInitFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Splash ad gate: report "disabled", SplashAdActivity finishes itself.
        SplashAdGateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0

                return v0
            """
        )

        // Splash ad renderer: never populate the container either.
        SplashAdLoaderFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // TopOn native card placement.
        NativeAdPlacementInitFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Server side (RTB) placements.
        OnlineAdInitFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}

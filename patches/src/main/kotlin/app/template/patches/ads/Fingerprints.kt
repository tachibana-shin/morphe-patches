package app.template.patches.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * Fingerprints for the advertising entry points of APKPure (`com.apkpure.aegon`).
 *
 * Written against version 3.20.7902 (version code 3207927).
 *
 * APKPure is only partly obfuscated: package names that come from the app's own
 * Kotlin sources keep readable names (`com/apkpure/aegon/ads/...`), while the
 * classes shared with the rest of the build are renamed to two letter packages
 * (`s4/c`, `h5/n`, `h5/k`, `u4/e`). Those are therefore declared by signature
 * plus instruction filters that are part of the advertising feature itself, so
 * they keep matching across an app update even if the renaming changes.
 *
 * Obfuscated parameters are declared as the bare object type `L`, as required
 * for [app.morphe.patcher.StringComparisonType] to match them.
 */

/**
 * The single bootstrap method of the app's ad stack. It reads the cached
 * `adConfig`, wires the splash ad helper and the native ad placements, then
 * instantiates `plugin_topon2.TopOnService`, which is the TopOn mediation
 * entry point that loads every bundled adapter.
 *
 * The method is also the only place where the app registers its app-wide
 * activity tracker (`v5/a`), so the patch re-issues that registration before
 * returning instead of dropping it.
 */
object AdSdkInitFingerprint : Fingerprint(
    definingClass = "Ls4/c;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        // The cached ad configuration is read first, so it anchors the start of
        // the method.
        string("adConfig"),
        // The app-wide activity tracker is registered next; keeping that
        // registration intact is why the patch does not simply return at the
        // very top of the method.
        //
        // The `$` of the inner class name is escaped, because Kotlin treats it
        // as the start of a string template inside a raw string.
        methodCall(
            smali = "Landroid/app/Application;->registerActivityLifecycleCallbacks(Landroid/app/Application" + '$' + "ActivityLifecycleCallbacks;)V"
        ),
        // Everything ad related is set up after the mediation service is looked
        // up by name. The filter is deliberately the last one so the inserted
        // return sits above the `Class.forName` call.
        string("com.apkpure.aegon.plugin.topon2.TopOnService")
    )
)

/**
 * Requests the `plugin_topon2` Tencent Shadow plugin and starts its
 * `InterstitialAdService`.
 *
 * That plugin is not part of the APK: it is downloaded from
 * `wj-static-eo.winudf.com` at runtime and carries the AdMob, Pangle, Vungle,
 * Bigo, Unity, ironSource, InMobi, AppLovin, Mbridge, AdColony and Fyber
 * adapters. Blocking the download keeps those SDKs out of the process
 * entirely, so the continuation callback (`l`) must still run.
 */
object TopOnPluginLoaderFingerprint : Fingerprint(
    definingClass = "Lh5/n;",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "L"),
    filters = listOf(
        string("plugin_topon2"),
        string("startADSdkService")
    )
)

/**
 * Initialises the ApicFast ad SDK. The call loads the packed native library
 * `libapi_cfas.so` (SONAME `libSdkVMP.so`, ijiami VMP) and its encrypted
 * `assets/api_cfas.VData` payload, so skipping it keeps the whole native ad
 * VM out of the process.
 */
object AppicFastInitFingerprint : Fingerprint(
    definingClass = "Lu4/e;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            smali = "Lcom/apicfast/sdk/core/SDK;->init(Landroid/content/Context;Ljava/lang/String;Lcom/apicfast/sdk/core/SDKListener;)V"
        )
    )
)

/**
 * Answers whether the launch screen may show a splash ad. Returning false here
 * makes `SplashAdActivity` close itself immediately through its own
 * `if (!SplashAdHelper.a()) finish()` guard.
 */
object SplashAdGateFingerprint : Fingerprint(
    definingClass = "Lh5/k;",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            smali = "Lcom/apkpure/aegon/ads/SplashConfig;->isOpen()Z"
        )
    )
)

/**
 * Renders the splash ad into the container supplied by the caller. Patched as
 * well so that a caller which ignores the gate never gets an ad either.
 */
object SplashAdLoaderFingerprint : Fingerprint(
    definingClass = "Lh5/k;",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Landroid/view/ViewGroup;", "Z"),
    filters = listOf(
        string("SplashAdHelper")
    )
)

/**
 * Registers the TopOn native ad placement used by the list cards and binds it
 * to the remote `hasOpenTopOnAds` / `hasGp` feature switches.
 */
object NativeAdPlacementInitFingerprint : Fingerprint(
    definingClass = "Lcom/apkpure/aegon/ads/topon/nativead/b;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        string("hasOpenTopOnAds"),
        string("hasGp")
    )
)

/**
 * Boots the server side (RTB) ad placements owned by the app itself, which are
 * used when TopOn reports no fill.
 */
object OnlineAdInitFingerprint : Fingerprint(
    definingClass = "Lcom/apkpure/aegon/ads/online/m;",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/apkpure/aegon/ads/online/m;",
            name = "e"
        ),
        fieldAccess(smali = "Ls4/c;->b:Ls4/c;")
    )
)

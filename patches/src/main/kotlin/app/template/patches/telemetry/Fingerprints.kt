package app.template.patches.telemetry

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * Fingerprints for the telemetry and anti tamper entry points of Zepp
 * (`com.huami.watch.hmwatchmanager`).
 *
 * Written against version 10.9.0-play (version code 151983).
 *
 * Zepp obfuscates far more aggressively than APKPure: R8 runs in full mode and
 * flattens the package structure, so thousands of application classes end up at
 * the dex root under two or three character names (`c21`, `s5a`, `tfe`, `yye0`).
 * Two mitigations are used here.
 *
 * First, anything with a readable name is declared by that exact name, because
 * Huami keeps its own module package names (`com/xiaomi/hm/health/...`,
 * `com/huami/datavaults/...`) and the bundled Facebook SDK keeps
 * `com/facebook/...` unobfuscated.
 *
 * Second, obfuscated parameter types are declared as the bare object type `L`,
 * as required for [app.morphe.patcher.StringComparisonType] to match them.
 *
 * The fingerprints that do have to name an obfuscated class (`Lc21;`) are
 * pinned to this target version, and each one is additionally anchored on a
 * non obfuscated string or call so a rename still fails loudly instead of
 * silently patching the wrong method.
 */

/**
 * The central dispatcher of Huami's in house analytics pipeline.
 *
 * Every event in the app is funneled into the single method
 * `c21.a(Lrvf;Lzt9;)V`: the usage event wrapper `c21.k` alone is called from
 * 418 sites, and the error event path `yye0` calls the dispatcher directly.
 * The method builds the event, optionally logs it under the `Analytics` tag and
 * then hands it to the `zt9` sink that performs the upload.
 *
 * Returning here therefore silences the whole in house pipeline from one place.
 *
 * Note the dispatcher also carries error/crash style events (the `g1g` payload
 * carries a `Throwable`), so blocking it stops crash reporting too. That is the
 * intended trade: nothing about the device leaves the process.
 */
object AnalyticsDispatchFingerprint : Fingerprint(
    definingClass = "Lc21;",
    name = "a",
    returnType = "V",
    // Both parameters are obfuscated classes: the event being dispatched and
    // the sink it is handed to.
    parameters = listOf("L", "L"),
    filters = listOf(
        // The dispatcher logs every event it handles under this tag before
        // handing it to the sink, which makes it a stable anchor.
        string("Analytics")
    )
)

/**
 * The cold launch task that brings the analytics pipeline up.
 *
 * `AnalyticsInitTask` is a `Li640` cold launch task (whose abstract entry point
 * is `a()V`). Its body is what assigns the master enable flag `c21.a`, then
 * calls `c21.i()` to build the pipeline and `c21.n()` / `c21.b()` to wire the
 * underlying loggers.
 *
 * Skipping the task keeps the pipeline uninitialised in the first place, which
 * is a cleaner state than initialising it and then blocking the dispatcher. The
 * dispatcher fingerprint above is still applied so that the remote config path
 * in `lw10`, which assigns the same flag from a server response, cannot bring
 * telemetry back after launch.
 */
object AnalyticsInitFingerprint : Fingerprint(
    definingClass = "Lcom/xiaomi/hm/health/init/coldlaunch/AnalyticsInitTask;",
    name = "a",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        // The master enable flag of the dispatcher is the anchor, and is also
        // the single assignment the patch exists to prevent.
        fieldAccess(smali = "Lc21;->a:Z")
    )
)

/**
 * The native anti tamper probes, run from the startup activity.
 *
 * `StartUpActivity` is the only caller of either probe in the whole app. On
 * every window focus gain it invokes `AntiDebug.method01()` and then
 * `AntiRepack.method01()`.
 *
 * Both are `native` methods backed by `libantidebug-lib.so` and
 * `libantirepack-lib.so` (149 KB and 569 KB, NDK r27d, stripped, with
 * encrypted strings). The repack probe reaches back into Java through
 * `AntiRepack.method02()`, which reports the flag as a `DATA_VAULTS`/`repack`
 * analytics event.
 *
 * In this release the two boolean results are discarded by the caller, so the
 * probes appear to be telemetry only. They are still disabled because the
 * native libraries are opaque, and because a server side flag derived from the
 * reported value could disable the app remotely.
 *
 * The patch returns right after the mandatory `Activity` super call, so window
 * focus handling keeps working while the probe block is never reached and the
 * two libraries are never even loaded.
 */
object AntiTamperFingerprint : Fingerprint(
    definingClass = "Lcom/xiaomi/hm/health/activity/StartUpActivity;",
    name = "onWindowFocusChanged",
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        // The repack probe is the last of the two calls, which puts the
        // inserted return above the whole block.
        methodCall(
            smali = "Lcom/huami/datavaults/antirepack/AntiRepack;->method01()Z"
        )
    )
)

/**
 * Explicit Facebook AppEvents logging.
 *
 * The bundled Facebook SDK is reduced to two classes, and this is the entry
 * point that forwards an event to the obfuscated `dbb` AppEvents logger.
 *
 * The method is guarded by `xtc0.a()`, which is also read by the login and
 * sharing code, so that guard is deliberately left alone and only the two
 * logging entry points are stubbed instead.
 */
object FacebookLogEventFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/appevents/InternalAppEventsLogger;",
    name = "a",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            smali = "Ldbb;->o(Ljava/lang/String;Ljava/lang/Double;Landroid/os/Bundle;ZLjava/util/UUID;)V"
        )
    )
)

/**
 * Implicit Facebook AppEvents logging, the sibling of
 * [FacebookLogEventFingerprint].
 *
 * Identical body apart from the implicit flag it passes to `dbb.o`, so the two
 * are told apart by the exact method name rather than by their instructions.
 */
object FacebookLogEventImplicitlyFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/appevents/InternalAppEventsLogger;",
    name = "b",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            smali = "Ldbb;->o(Ljava/lang/String;Ljava/lang/Double;Landroid/os/Bundle;ZLjava/util/UUID;)V"
        )
    )
)

package app.template.patches.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_ZEPP

/**
 * Disable telemetry in Zepp.
 *
 * Zepp ships no third party advertising worth removing: the AdMob app id in the
 * manifest is declared without the AdMob SDK ever being bundled, and Huami's
 * own ad scaffold (`com.huami.ad`, seven classes) sits behind a server driven
 * splash/launch flow that is dormant in this release. The privacy surface that
 * remains is the app's own reporting, which this patch closes off.
 *
 * The app has a single in house analytics pipeline, and it is stopped at two
 * independent points so that neither a remote feature flag nor the launch
 * ordering can resurrect it:
 *
 *  1. **Bootstrap.** `AnalyticsInitFingerprint` marks the cold launch task that
 *     assigns the pipeline's master enable flag and builds its loggers. Skipping
 *     it leaves the pipeline uninitialised in the first place.
 *
 *  2. **Dispatch.** `AnalyticsDispatchFingerprint` marks the one method every
 *     event funnels through, reached from 418 usage event call sites and from
 *     the error event path alike. Returning here blocks any event that does get
 *     built, including one driven by the remote config path that assigns the
 *     enable flag from a server response after launch.
 *
 * Two further sources are closed on top of that:
 *
 *  3. **Facebook AppEvents.** Both the explicit and implicit logging entry
 *     points of the bundled (and much reduced) Facebook SDK are stubbed. The
 *     shared guard is left untouched because the login and sharing code reads it
 *     too.
 *
 *  4. **Native anti tamper probes.** `AntiTamperFingerprint` marks the single
 *     call site of the `libantidebug-lib.so` and `libantirepack-lib.so`
 *     probes. Returning immediately after the mandatory super call means neither
 *     library is loaded, and the repack probe can no longer report the install
 *     as repacked through its native-to-Java callback.
 *
 * Stopping the dispatcher also stops error/crash style events, since those are
 * carried by the same pipeline. That is deliberate: the goal is that nothing
 * about the device leaves the process.
 */
@Suppress("unused")
val disableTelemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Stops Zepp reporting usage, errors and advertising ids.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ZEPP)

    execute {
        // Central dispatcher: every usage and error event passes through here.
        AnalyticsDispatchFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Cold launch bootstrap: never assign the enable flag or build the
        // loggers, so the pipeline stays uninitialised.
        AnalyticsInitFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Facebook AppEvents: block both the explicit and the implicit
        // forwarding entry points, leaving the SDK's shared guard alone.
        FacebookLogEventFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        FacebookLogEventImplicitlyFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Anti tamper probes. Index 1 is immediately after the mandatory
        // `invoke-super`, which is the first instruction of this override, so
        // window focus handling keeps working while the probe block below it is
        // never reached. Returning at the matched probe itself would still let
        // the preceding `sget-object` initialise `AntiDebug` and load its
        // native library.
        AntiTamperFingerprint.method.addInstructions(
            1,
            """
                return-void
            """
        )
    }
}

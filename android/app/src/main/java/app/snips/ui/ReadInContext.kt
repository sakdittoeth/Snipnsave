package app.snips.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import app.snips.capture.deepLink
import app.snips.data.Snip

/**
 * Step 4 of HANDOVER.md §7 — get back to the exact sentence.
 *
 * §3 calls for Custom Tabs specifically, and the device findings say why:
 * a plain `ACTION_VIEW` can be claimed by the Substack app, which cannot
 * honour text fragments at all, so the reader lands on the headline with
 * nothing to explain it. Pinning the intent to a browser that implements
 * Custom Tabs keeps the fragment in play.
 *
 * The link itself is rebuilt by [deepLink], which also rewrites Substack's
 * `open.substack.com` interstitial — measured on a device, a fragment does
 * not survive that redirect.
 *
 * @return false when there is nothing to open, so the caller can say so.
 */
fun openInContext(context: Context, snip: Snip, toolbarColor: Int): Boolean {
    val url = deepLink(snip.url, snip.text, snip.fragmentTruncated) ?: return false

    val intent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setUrlBarHidingEnabled(true)
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbarColor)
                .build(),
        )
        .build()

    // Without an explicit package this is still an ACTION_VIEW under the hood,
    // and any app claiming the host can take it. Naming a Custom Tabs browser
    // is what actually keeps the link in a browser.
    CustomTabsClient.getPackageName(context, null)?.let { intent.intent.setPackage(it) }

    return try {
        intent.launchUrl(context, Uri.parse(url))
        true
    } catch (_: ActivityNotFoundException) {
        // No Custom Tabs browser on the device. A plain view intent may hand
        // the link to the Substack app and lose the fragment, but landing on
        // the post beats not opening it at all.
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

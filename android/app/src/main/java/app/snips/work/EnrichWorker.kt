package app.snips.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.snips.data.MetadataClient
import app.snips.data.SnipDatabase
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * §5: enrichment runs as a job, never inline. The snip is already saved and
 * already on screen with a hostname-derived publication; this fills the rest
 * in when the network gets round to it.
 *
 * "Metadata failure is never a save failure" is the rule that shapes the
 * result codes below.
 */
class EnrichWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(KEY_SNIP_ID) ?: return@withContext Result.failure()
        val dao = SnipDatabase.get(applicationContext).snips()

        // Gone, or already done, or nothing to fetch from.
        val snip = dao.byId(id) ?: return@withContext Result.success()
        if (snip.enriched || snip.url.isEmpty()) return@withContext Result.success()

        val metadata = try {
            MetadataClient().fetch(snip.url)
        } catch (_: Exception) {
            // A dead host, a redirect loop, a paywall that returns nothing —
            // all ordinary. Retry once or twice, then let it lie.
            return@withContext if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        } ?: return@withContext Result.success()

        // Anything already on the snip — a title the share handed us, a note —
        // stays. Enrichment fills gaps; it doesn't overwrite what we had.
        dao.update(
            snip.copy(
                publication = metadata.publication.ifEmpty { snip.publication },
                title = snip.title.ifEmpty { metadata.title },
                author = snip.author.ifEmpty { metadata.author },
                coverUrl = snip.coverUrl.ifEmpty { metadata.coverUrl },
                logoUrl = snip.logoUrl.ifEmpty { metadata.logoUrl },
                enriched = true,
            ),
        )
        Result.success()
    }

    companion object {
        private const val KEY_SNIP_ID = "snipId"
        private const val MAX_ATTEMPTS = 3

        fun enqueue(context: Context, snipId: String) {
            val request = OneTimeWorkRequestBuilder<EnrichWorker>()
                .setInputData(workDataOf(KEY_SNIP_ID to snipId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            // Keyed by snip id, so a double-tap or a re-share can't queue the
            // same fetch twice.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "enrich-$snipId",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}

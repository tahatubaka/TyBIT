package com.tybit.data.releases

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tybit.R
import com.tybit.data.innertube.Innertube
import com.tybit.data.innertube.InnertubeParser
import com.tybit.data.model.ShelfItem
import com.tybit.data.settings.AppSettings
import com.tybit.data.stats.ListeningStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Spotify's Release Radar, as a notification: twice a day, the newest albums
 * and singles of the artists played most on this device, and a notification
 * for each one not seen before. The first look at an artist only records what
 * is already out, so turning this on does not bring a back catalogue with it.
 *
 * Runs as a JobScheduler job — the platform's own — with network required.
 */
object ReleaseRadar {

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!AppSettings.releaseRadar.value) {
            scheduler.cancel(JOB_ID)
            return
        }
        if (scheduler.getPendingJob(JOB_ID) != null) return
        scheduler.schedule(
            JobInfo.Builder(JOB_ID, ComponentName(context, ReleaseRadarJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD_MS)
                .build(),
        )
    }

    /** One pass over the watched artists. */
    suspend fun check(context: Context) {
        if (!AppSettings.releaseRadar.value) return
        val seenPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for ((artistId, name) in ListeningStats.topArtists(MAX_ARTISTS)) {
            val page = runCatching { InnertubeParser.parseArtistPage(Innertube.browse(artistId)) }.getOrNull()
            if (page != null) {
                // Releases are the cards that open an album page (MPRE… ids);
                // each shelf leads with its newest.
                val releases = page.sections
                    .flatMap { shelf -> shelf.items.filter { it.browseId?.startsWith("MPRE") == true }.take(PER_SHELF) }
                    .distinctBy { it.browseId }
                val key = "seen_$artistId"
                val seen = seenPrefs.getStringSet(key, null)
                if (seen != null) {
                    releases.filter { it.browseId !in seen }.forEach { notify(context, name, it) }
                }
                seenPrefs.edit().putStringSet(key, seen.orEmpty() + releases.mapNotNull { it.browseId }).apply()
            }
            delay(SPACING_MS)
        }
    }

    /** True once, the first time it is asked while notifications aren't allowed yet. */
    fun shouldAskPermission(context: Context): Boolean {
        if (!AppSettings.releaseRadar.value || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false)) return false
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        return true
    }

    private fun notify(context: Context, artist: String, release: ShelfItem) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.release_radar), NotificationManager.IMPORTANCE_DEFAULT),
        )
        // The app opens release links itself (see MusicLink): straight to its page.
        val open = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/browse/${release.browseId}"))
            .setPackage(context.packageName)
        val tap = PendingIntent.getActivity(
            context,
            release.browseId.hashCode(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(context.getString(R.string.release_radar_title, artist))
            .setContentText(release.title)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        manager.notify(release.browseId.hashCode(), notification)
    }

    private const val JOB_ID = 4711
    private const val CHANNEL = "new_releases"
    private const val PREFS = "tybit_release_radar"
    private const val KEY_ASKED = "permission_asked"
    private const val MAX_ARTISTS = 15
    private const val PER_SHELF = 3
    private const val SPACING_MS = 1_500L
    private const val PERIOD_MS = 12 * 60 * 60 * 1000L
}

/** The job [ReleaseRadar.schedule] books: one [ReleaseRadar.check] per run. */
class ReleaseRadarJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            runCatching { ReleaseRadar.check(applicationContext) }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        scope.coroutineContext.cancelChildren()
        return true
    }
}

package com.example.core.update

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Native Android JobService that keeps silent 100 KB/s update pre-downloading alive
 * and resumable even after the user closes Sheildlenz from Recents or battery recovers > 20%.
 */
class AegisUpdateResumeJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "Resuming persisted update pre-download job.")
        try {
            UpdateChecker.resumePersistedPreloadFromJob(applicationContext) {
                try {
                    jobFinished(params, false)
                } catch (_: Throwable) {
                }
            }
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "Update resume job aborted safely: ${t.message}")
            try {
                jobFinished(params, false)
            } catch (_: Throwable) {
            }
            return false
        }
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return try {
            UpdateChecker.hasIncompletePersistedDownload(applicationContext)
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        private const val TAG = "AegisUpdateResumeJob"
        private const val JOB_ID = 7741

        fun schedule(context: Context) {
            try {
                val appCtx = context.applicationContext
                val scheduler = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(appCtx, AegisUpdateResumeJobService::class.java)
                val builder = JobInfo.Builder(JOB_ID, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setMinimumLatency(3_000L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder.setRequiresBatteryNotLow(true)
                }
                scheduler.schedule(builder.build())
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to schedule resume job safely: ${t.message}")
            }
        }

        fun cancel(context: Context) {
            try {
                val scheduler = context.applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                scheduler?.cancel(JOB_ID)
            } catch (_: Throwable) {
            }
        }
    }
}

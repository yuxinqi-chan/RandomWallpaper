package com.github.yuxinqi_chan.random_wallpaper

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WallpaperWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams), IRefreshWallpaper {

    override fun doWork(): Result {
        return try {
            refreshWallpaper()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        } finally {
            if (getAutoRefreshSwitch()) {
                val autoRefreshInterval = getAutoRefreshInterval()
                if (autoRefreshInterval != null) {
                    val oneTimeWorkRequest =
                        OneTimeWorkRequestBuilder<WallpaperWorker>().setInitialDelay(
                            autoRefreshInterval,
                            TimeUnit.MINUTES
                        ).build()
                    getWorkManager().enqueue(oneTimeWorkRequest)
                }
            }
        }
    }

    override fun requireContext(): Context {
        return context
    }

}
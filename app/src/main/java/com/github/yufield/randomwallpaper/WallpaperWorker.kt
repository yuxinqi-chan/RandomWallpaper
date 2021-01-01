package com.github.yufield.randomwallpaper

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WallpaperWorker(override val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams), IRefreshWallpaper {

    override fun doWork(): Result {
        return try {
            refreshWallpaper()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        } finally {
            val string =
                defaultSharedPreferences.getString(context.getString(R.string.auto_refresh_minute),
                    "0")
            Log.v(javaClass.simpleName,
                "${context.getString(R.string.auto_refresh_minute)},$string")
            val long = string?.toLongOrNull() ?: 0
            if (long > 0) {
                val oneTimeWorkRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                    .setInitialDelay(long, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
            }
        }
    }

}
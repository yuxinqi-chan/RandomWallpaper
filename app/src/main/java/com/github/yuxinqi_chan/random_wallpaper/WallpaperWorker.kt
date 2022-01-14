package com.github.yuxinqi_chan.random_wallpaper

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
            val string =
                defaultSharedPreferences.getString(
                    context.getString(R.string.auto_refresh_minute),
                    "0"
                )
            Log.v(
                javaClass.simpleName,
                "${context.getString(R.string.auto_refresh_minute)},$string"
            )
            var long = string?.toLongOrNull()
            if (long != null) {
                if (long < 1)
                    long = 1
                val oneTimeWorkRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                    .setInitialDelay(long, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
            }
        }
    }

    override fun requireContext(): Context {
        return context
    }

    override fun getString(resId: Int): String {
        return context.getString(resId)
    }

}
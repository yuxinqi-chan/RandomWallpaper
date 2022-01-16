package com.github.yuxinqi_chan.random_wallpaper

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import java.util.concurrent.TimeUnit

class MainPrefFragment : PreferenceFragmentCompat(), IRefreshWallpaper,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(javaClass.name, "onCreate")
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.v(javaClass.name, "onCreatePreferences")
        setPreferencesFromResource(R.xml.pref_main, rootKey)
        preferenceManager.findPreference<DocumentTreePreference>(getString(R.string.directory))
            ?.registerForActivityResult(this)
        preferenceManager.findPreference<RefreshButton>(getString(R.string.refresh_now))?.fragment =
            this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(javaClass.name, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = preferenceManager.sharedPreferences
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        logWorkInfo()
        if (getAutoRefreshSwitch()) {
            val workInfoList = getWorkManager()
                .getWorkInfosByTag(WallpaperWorker::class.java.name).get()
            if (!workInfoList.any { workInfo ->
                    workInfo.state == WorkInfo.State.ENQUEUED ||
                            workInfo.state == WorkInfo.State.RUNNING
                }) {
                startWork()
            }
        } else {
            getWorkManager()
                .cancelAllWorkByTag(WallpaperWorker::class.java.name)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Log.v(javaClass.name, "onSharedPreferenceChanged")
        if (key == getString(R.string.auto_refresh_switch)) {
            if (getAutoRefreshSwitch()) {
                startWork()
            } else {
                getWorkManager()
                    .cancelAllWorkByTag(WallpaperWorker::class.java.name)
            }
        }
        if (key == getString(R.string.auto_refresh_minute)) {
            getWorkManager()
                .cancelAllWorkByTag(WallpaperWorker::class.java.name)
            if (getAutoRefreshSwitch()) {
                startWork()
            }
        }
    }

    private fun logWorkInfo() {
        val workManager = getWorkManager()
        workManager.pruneWork()
        val workInfoList = workManager
            .getWorkInfosByTag(WallpaperWorker::class.java.name).get()
        workInfoList.forEach { workInfo ->
            Log.v(javaClass.name, "$workInfo")
        }
    }

    private fun startWork() {
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
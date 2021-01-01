package com.github.yufield.randomwallpaper

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.yufield.randomwallpaper.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), IRefreshWallpaper,
    SharedPreferences.OnSharedPreferenceChangeListener, CoroutineScope by MainScope() {
    override val context: Context
        get() = this
    private lateinit var binding: ActivityMainBinding
    private lateinit var workManager: WorkManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(this)
        binding = ActivityMainBinding.inflate(layoutInflater).apply { setContentView(root) }
        binding.button1.setOnClickListener {
            it as Button
            it.isClickable = false
            it.text = getString(R.string.refreshing)
            launch(Dispatchers.IO) {
                try {
                    refreshWallpaper()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    launch(Dispatchers.Main) {
                        it.isClickable = true
                        it.text = getString(R.string.refresh_now)
                    }
                }
            }
        }
        workManager.getWorkInfosByTag(WallpaperWorker::class.java.name).get()
            .also { Log.v(javaClass.simpleName, "workInfosByTag[:${it.size}]") }
            .forEach { Log.v(javaClass.simpleName, "$it") }
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        Log.v(javaClass.simpleName, "onDestroy")
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val string = sharedPreferences.getString(key, "0")
        Log.v(javaClass.simpleName, "$key,$string")
        if (key == getString(R.string.auto_refresh_minute)) {
            workManager.cancelAllWorkByTag(WallpaperWorker::class.java.name)
            val long = string?.toLongOrNull() ?: 0
            if (long > 0) {
                val oneTimeWorkRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                    .setInitialDelay(long, TimeUnit.MINUTES)
                    .build()
                workManager.enqueue(oneTimeWorkRequest)
            }
        }
    }
}



package com.github.yuxinqi_chan.random_wallpaper

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.work.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class MainPrefFragment : PreferenceFragmentCompat(), IRefreshWallpaper,
    SharedPreferences.OnSharedPreferenceChangeListener,
    CoroutineScope by MainScope() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var pickDirectoryLauncher: ActivityResultLauncher<Uri>
    private lateinit var workManager: WorkManager
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.v(javaClass.simpleName, "onCreatePreferences")
        setPreferencesFromResource(R.xml.pref_main, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(requireContext())
        /*只能在onCreate注册打开选择文件夹的操作*/
        val openDocumentTree = ActivityResultContracts.OpenDocumentTree()
        pickDirectoryLauncher =
            registerForActivityResult(openDocumentTree) { uri ->
                if (uri != null) {
                    sharedPreferences.edit()
                        .putString(getString(R.string.directory_uri), uri.toString())
                        .apply()
                    val pickDirectoryPreference =
                        preferenceManager.findPreference<Preference>(getString(R.string.directory_uri))
                    pickDirectoryPreference?.summary = uri.path
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(javaClass.simpleName, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = preferenceManager.sharedPreferences
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        logWorkInfo()
        val boolean = sharedPreferences.getBoolean(getString(R.string.auto_refresh_switch), false)
        if (boolean) {
            val workInfoList = workManager.getWorkInfosByTag(WallpaperWorker::class.java.name).get()
            if (!workInfoList.any { workInfo -> workInfo.state == WorkInfo.State.ENQUEUED }) {
                startWork()
            }
        } else {
            workManager.cancelAllWorkByTag(WallpaperWorker::class.java.name)
        }
        logWorkInfo()
        /*自动刷新时间*/
        val autoRefreshMinutePreference =
            preferenceManager.findPreference<EditTextPreference>(getString(R.string.auto_refresh_minute))
        if (autoRefreshMinutePreference != null) {
            autoRefreshMinutePreference.setOnBindEditTextListener { editText ->
                editText.setSingleLine()
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            handlePreferenceSummary(
                autoRefreshMinutePreference,
                getString(R.string.auto_refresh_minute),
                getString(R.string.default_refresh_minute)
            )
        }
        /*图片接口*/
        val apiUrlPreference =
            preferenceManager.findPreference<EditTextPreference>(getString(R.string.api_url))
        if (apiUrlPreference != null) {
            handlePreferenceSummary(
                apiUrlPreference,
                getString(R.string.api_url),
                getString(R.string.default_api_url)
            )
        }
        /*立刻刷新*/
        val refreshNowPreference =
            preferenceManager.findPreference<Preference>(getString(R.string.refresh_now))
        if (refreshNowPreference != null) {
            refreshNowPreference.setOnPreferenceClickListener { preference ->
                Log.v(javaClass.simpleName, getString(R.string.refresh_now))
                preference.isEnabled = false
                preference.title = getString(R.string.refreshing)
                launch(Dispatchers.IO) {
                    try {
                        refreshWallpaper()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        launch(Dispatchers.Main) {
                            preference.isEnabled = true
                            preference.title = getString(R.string.refresh_now)
                        }
                    }
                }
                true
            }
        }
        /*选择目录*/
        val pickDirectoryPreference =
            preferenceManager.findPreference<Preference>(getString(R.string.directory_uri))
        if (pickDirectoryPreference != null) {
            pickDirectoryPreference.setOnPreferenceClickListener {
                Log.v(javaClass.simpleName, getString(R.string.directory_uri))
                pickDirectoryLauncher.launch(Uri.EMPTY)
                true
            }
            pickDirectoryPreference.summary = Uri.parse(
                sharedPreferences.getString(
                    getString(R.string.directory_uri),
                    Uri.EMPTY.toString()
                )
            ).path
        }
    }

    private fun handlePreferenceSummary(preference: Preference, key: String, defaultValue: String) {
        preference.summary = sharedPreferences.getString(key, defaultValue)
        preference.setOnPreferenceChangeListener { p, newValue ->
            p.summary = newValue.toString()
            true
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Log.v(javaClass.simpleName, "onSharedPreferenceChanged")
        if (key == getString(R.string.auto_refresh_switch)) {
            logWorkInfo()
            val boolean = sharedPreferences.getBoolean(key, false)
            if (boolean) {
                startWork()
            } else {
                workManager.cancelAllWorkByTag(WallpaperWorker::class.java.name)
            }
            logWorkInfo()
        }
        if (key == getString(R.string.auto_refresh_minute)) {
            logWorkInfo()
            workManager.cancelAllWorkByTag(WallpaperWorker::class.java.name)
            startWork()
            logWorkInfo()
        }
    }

    private fun logWorkInfo() {
        val workInfoList = workManager.getWorkInfosByTag(WallpaperWorker::class.java.name).get()
        Log.v(javaClass.simpleName, "workInfoList[${workInfoList.size}]")
        workInfoList.forEach { workInfo ->
            Log.v(javaClass.simpleName, "$workInfo")
        }
    }

    private fun startWork() {
        var long = sharedPreferences.getString(getString(R.string.auto_refresh_minute), "1")
            ?.toLongOrNull()
        if (long != null) {
            if (long < 1)
                long = 1
            val oneTimeWorkRequest =
                OneTimeWorkRequestBuilder<WallpaperWorker>().setInitialDelay(long, TimeUnit.MINUTES)
                    .build()
            workManager.enqueue(oneTimeWorkRequest)
        }
    }
}
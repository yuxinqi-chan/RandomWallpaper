package com.github.yuxinqi_chan.random_wallpaper

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.work.WorkManager

interface IHelper {
    fun requireContext(): Context
    fun getApiUrl(): String? {
        return PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
            requireContext().getString(R.string.api_url),
            null
        )
    }

    fun getDirectoryUri(): Uri {
        return Uri.parse(
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                requireContext().getString(R.string.directory),
                null
            )
        )
    }

    fun getApiRandomSwitch(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
            requireContext().getString(R.string.api_random_switch),
            false
        )
    }

    fun getDirectoryRandomSwitch(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
            requireContext().getString(R.string.directory_random_switch),
            false
        )
    }

    fun getAutoRefreshSwitch(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
            requireContext().getString(R.string.auto_refresh_switch),
            false
        )
    }

    fun getAutoRefreshInterval(): Long? {
        return PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
            requireContext().getString(R.string.auto_refresh_minute),
            null
        )?.toLongOrNull()
    }

    fun getWorkManager(): WorkManager {
        return WorkManager.getInstance(requireContext())
    }

    fun toast(message: String?) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
package com.github.yufield.randomwallpaper

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class MainPrefFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)

    }
}
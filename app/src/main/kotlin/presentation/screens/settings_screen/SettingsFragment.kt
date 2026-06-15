package com.dunihuliapps.myglidingassistnat.presentation.screens.settings_screen

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import com.dunihuliapps.myglidingassistant.R

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs, rootKey)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        key?.let {
            viewModel.onPreferenceChanged(it)
            // The modern library often handles summary updates automatically if
            // app:useSimpleSummaryProvider="true" is set in prefs.xml
        }
    }
}
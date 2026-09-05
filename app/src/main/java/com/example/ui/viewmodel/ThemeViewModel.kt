package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)

    val themeMode: StateFlow<Int> = settingsStore.themeModeFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0
    )

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            settingsStore.setThemeMode(mode)
        }
    }
}

package com.lavallette.tides.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lavallette.tides.data.model.TideSnapshot
import com.lavallette.tides.data.model.WeatherSnapshot
import com.lavallette.tides.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val tides: TideSnapshot? = null,
    val weather: WeatherSnapshot? = null,
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh(force = true)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val hasData = _state.value.tides != null
            _state.update {
                it.copy(
                    loading = !hasData,
                    refreshing = hasData,
                    error = null
                )
            }
            try {
                val data = repo.load(forceRefresh = force)
                _state.update {
                    UiState(
                        loading = false,
                        refreshing = false,
                        tides = data.tides,
                        weather = data.weather,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = e.message ?: "Unable to load tide & weather data"
                    )
                }
            }
        }
    }
}

package com.codextraffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.TrafficUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrafficViewModel(
    private val repository: TrafficRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrafficUiState())
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.connectionStatus.collect { status ->
                _uiState.update { state ->
                    state.copy(
                        connectionStatus = status,
                        errorMessage = if (status == ConnectionStatus.Error) {
                            "BLE connection error"
                        } else {
                            null
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.snapshots.collect { snapshot ->
                _uiState.update { state ->
                    state.copy(snapshot = snapshot)
                }
            }
        }
    }

    fun start() {
        repository.start()
    }

    fun stop() {
        repository.stop()
    }

    fun requiredPermissions(): Array<String> = repository.requiredPermissions()

    fun hasRequiredPermissions(): Boolean = repository.hasRequiredPermissions()

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}


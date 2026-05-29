package com.codextraffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficSnapshot
import com.codextraffic.model.TrafficUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrafficViewModel(
    private val repository: TrafficRepository,
    private val hiddenProjectStore: HiddenProjectStore? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrafficUiState())
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()
    private var rawSnapshot = TrafficSnapshot.empty
    private var hiddenProjectIds = emptySet<String>()

    init {
        viewModelScope.launch {
            hiddenProjectIds = hiddenProjectStore?.hiddenProjectIds().orEmpty()
            _uiState.update { state -> state.withFilteredSnapshot(rawSnapshot, hiddenProjectIds) }
        }
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
                rawSnapshot = snapshot
                _uiState.update { state ->
                    state.withFilteredSnapshot(snapshot, hiddenProjectIds)
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

    fun hideProject(project: ProjectTraffic) {
        updateHiddenProjects(hiddenProjectIds + project.id)
    }

    fun restoreProject(project: ProjectTraffic) {
        updateHiddenProjects(hiddenProjectIds - project.id)
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }

    private fun updateHiddenProjects(ids: Set<String>) {
        hiddenProjectIds = ids
        _uiState.update { state -> state.withFilteredSnapshot(rawSnapshot, hiddenProjectIds) }
        viewModelScope.launch {
            hiddenProjectStore?.saveHiddenProjectIds(hiddenProjectIds)
        }
    }
}

private fun TrafficUiState.withFilteredSnapshot(
    snapshot: TrafficSnapshot,
    hiddenProjectIds: Set<String>,
): TrafficUiState {
    if (hiddenProjectIds.isEmpty()) {
        return copy(snapshot = snapshot, hiddenProjects = emptyList())
    }

    val hidden = snapshot.projects.filter { it.id in hiddenProjectIds }
    val visible = snapshot.projects.filterNot { it.id in hiddenProjectIds }
    return copy(
        snapshot = snapshot.copy(
            projects = visible,
            overall = visible.overallLight(),
        ),
        hiddenProjects = hidden,
    )
}

private fun List<ProjectTraffic>.overallLight(): TrafficLight = when {
    any { it.reason == ReasonCode.Work } -> TrafficLight.Green
    any { it.reason == ReasonCode.Recent } -> TrafficLight.Yellow
    any { it.reason == ReasonCode.Stale || it.reason == ReasonCode.Blocked || it.reason == ReasonCode.CodexOff } -> TrafficLight.Red
    isNotEmpty() -> TrafficLight.Red
    else -> TrafficLight.Red
}

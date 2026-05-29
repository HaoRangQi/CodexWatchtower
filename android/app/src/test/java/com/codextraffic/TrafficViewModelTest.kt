package com.codextraffic

import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrafficViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updatesConnectionStatusAndSnapshotIndependently() = runTest {
        val repository = FakeTrafficRepository()
        val viewModel = TrafficViewModel(repository)
        advanceUntilIdle()

        repository.connection.value = ConnectionStatus.Connected
        repository.snapshotsFlow.emit(
            TrafficSnapshot(
                version = 1,
                timestampSeconds = 10,
                overall = TrafficLight.Green,
                projects = listOf(
                    ProjectTraffic("id", "loading", TrafficLight.Green, 2, ReasonCode.Work)
                ),
                omittedCount = 0,
            )
        )
        advanceUntilIdle()

        assertEquals(ConnectionStatus.Connected, viewModel.uiState.value.connectionStatus)
        assertEquals(TrafficLight.Green, viewModel.uiState.value.snapshot.overall)
        assertEquals("loading", viewModel.uiState.value.snapshot.projects.single().name)
    }

    @Test
    fun startAndStopDelegateToRepository() {
        val repository = FakeTrafficRepository()
        val viewModel = TrafficViewModel(repository)

        viewModel.start()
        viewModel.stop()

        assertEquals(1, repository.startCalls)
        assertEquals(1, repository.stopCalls)
    }

    private class FakeTrafficRepository : TrafficRepository {
        val connection = MutableStateFlow(ConnectionStatus.Disconnected)
        val snapshotsFlow = MutableSharedFlow<TrafficSnapshot>()
        var startCalls = 0
        var stopCalls = 0

        override val connectionStatus: StateFlow<ConnectionStatus> = connection
        override val snapshots = snapshotsFlow

        override fun start() {
            startCalls += 1
        }

        override fun stop() {
            stopCalls += 1
        }

        override fun requiredPermissions(): Array<String> = emptyArray()

        override fun hasRequiredPermissions(): Boolean = true
    }
}

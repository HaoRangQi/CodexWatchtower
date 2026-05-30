package com.codextraffic

import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.PetFeedItem
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    @Test
    fun hiddenProjectsAreFilteredAndRemembered() = runTest {
        val repository = FakeTrafficRepository()
        val hiddenStore = FakeHiddenProjectStore()
        val viewModel = TrafficViewModel(repository, hiddenStore)
        val active = ProjectTraffic("active", "loading", TrafficLight.Green, 2, ReasonCode.Work)
        val hidden = ProjectTraffic("hidden", "old-job", TrafficLight.Green, 3, ReasonCode.Work)
        advanceUntilIdle()

        repository.snapshotsFlow.emit(
            TrafficSnapshot(
                version = 1,
                timestampSeconds = 10,
                overall = TrafficLight.Green,
                projects = listOf(active, hidden),
                omittedCount = 0,
                feedItems = listOf(
                    PetFeedItem("active", "正在推进 loading", "2 秒内有新动作", TrafficLight.Green, 2, ReasonCode.Work),
                    PetFeedItem("hidden", "正在推进 old-job", "3 秒内有新动作", TrafficLight.Green, 3, ReasonCode.Work),
                ),
            )
        )
        advanceUntilIdle()

        viewModel.hideProject(hidden)
        advanceUntilIdle()

        assertEquals(listOf(active), viewModel.uiState.value.snapshot.projects)
        assertEquals(listOf("active"), viewModel.uiState.value.snapshot.feedItems.map { it.projectId })
        assertEquals(listOf(hidden), viewModel.uiState.value.hiddenProjects)
        assertEquals(setOf("hidden"), hiddenStore.savedIds)
    }

    @Test
    fun restoringHiddenProjectMakesItVisibleAgain() = runTest {
        val repository = FakeTrafficRepository()
        val hiddenStore = FakeHiddenProjectStore(initialIds = setOf("hidden"))
        val viewModel = TrafficViewModel(repository, hiddenStore)
        val hidden = ProjectTraffic("hidden", "old-job", TrafficLight.Yellow, 3, ReasonCode.Recent)
        advanceUntilIdle()

        repository.snapshotsFlow.emit(
            TrafficSnapshot(
                version = 1,
                timestampSeconds = 10,
                overall = TrafficLight.Yellow,
                projects = listOf(hidden),
                omittedCount = 0,
            )
        )
        advanceUntilIdle()

        assertEquals(emptyList<ProjectTraffic>(), viewModel.uiState.value.snapshot.projects)
        assertEquals(listOf(hidden), viewModel.uiState.value.hiddenProjects)

        viewModel.restoreProject(hidden)
        advanceUntilIdle()

        assertEquals(listOf(hidden), viewModel.uiState.value.snapshot.projects)
        assertEquals(emptyList<ProjectTraffic>(), viewModel.uiState.value.hiddenProjects)
        assertEquals(emptySet<String>(), hiddenStore.savedIds)
    }

    @Test
    fun hybridRepositoryUsesHttpWhenBleCannotConnect() = runTest {
        val ble = FakeTrafficRepository()
        val http = FakeTrafficRepository()
        val repository = HybridTrafficRepository(ble, http, this)
        val snapshot = TrafficSnapshot(
            version = 1,
            timestampSeconds = 10,
            overall = TrafficLight.Green,
            projects = listOf(ProjectTraffic("id", "loading", TrafficLight.Green, 1, ReasonCode.Work)),
            omittedCount = 0,
        )

        repository.start()
        ble.connection.value = ConnectionStatus.PermissionMissing
        http.connection.value = ConnectionStatus.Connected
        http.snapshotsFlow.emit(snapshot)
        advanceUntilIdle()

        assertEquals(ConnectionStatus.Connected, repository.connectionStatus.value)
        assertEquals(snapshot, repository.snapshots.first())
        assertEquals(1, ble.startCalls)
        assertEquals(1, http.startCalls)

        repository.stop()
        advanceUntilIdle()
    }

    private class FakeTrafficRepository : TrafficRepository {
        val connection = MutableStateFlow(ConnectionStatus.Disconnected)
        val snapshotsFlow = MutableSharedFlow<TrafficSnapshot>(replay = 1)
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

    private class FakeHiddenProjectStore(
        initialIds: Set<String> = emptySet(),
    ) : HiddenProjectStore {
        var savedIds = initialIds

        override suspend fun hiddenProjectIds(): Set<String> = savedIds

        override suspend fun saveHiddenProjectIds(ids: Set<String>) {
            savedIds = ids
        }
    }
}

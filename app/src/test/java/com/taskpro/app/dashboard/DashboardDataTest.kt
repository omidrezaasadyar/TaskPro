package com.taskpro.app.dashboard

import android.os.Looper
import androidx.room.Room
import com.taskpro.app.data.*
import com.taskpro.app.notification.ReminderScheduler
import com.taskpro.app.ui.screens.dashboard.*
import com.taskpro.app.ui.screens.search.SearchViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DashboardDataTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Before fun setup() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        repository = TaskRepository(db.itemDao(), db.taskDao(), ReminderScheduler(app))
        db.itemDao().insert(Item(1, "Original group"))
        Unit
    }
    @After fun close() { scope.cancel(); db.close() }
    private fun await(condition: () -> Boolean) {
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idleFor(20, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Flow did not reach its expected state")
    }

    @Test fun creationEditStatusDeleteAndGroupRenameInvalidateTheProjection() = runBlocking {
        val events = Channel<List<TaskWithItemName>>(Channel.UNLIMITED)
        val job = scope.launch { repository.observeDashboardTasks().collect { events.send(it) } }
        suspend fun next(predicate: (List<TaskWithItemName>) -> Boolean): List<TaskWithItemName> = withTimeout(10_000) {
            var rows = events.receive()
            while (!predicate(rows)) rows = events.receive()
            rows
        }
        assertTrue(next { it.isEmpty() }.isEmpty())
        repository.addTask(1, "Created", "Notes", null)
        val created = next { it.size == 1 }.single().task
        repository.updateTask(created.copy(title = "Edited", dueAt = 12_345))
        assertEquals(12_345L, next { it.singleOrNull()?.task?.title == "Edited" }.single().task.dueAt)
        repository.setStatus(created.copy(title = "Edited", dueAt = 12_345), TaskStatus.COMPLETED)
        next { it.singleOrNull()?.task?.status == TaskStatus.COMPLETED }
        db.itemDao().update(Item(1, "Renamed group"))
        next { it.singleOrNull()?.itemName == "Renamed group" }
        repository.deleteTask(db.taskDao().getById(created.id)!!)
        next { it.isEmpty() }
        job.cancelAndJoin()
    }

    @Test fun groupDeletionCascadesOutOfDashboardAndReadingDoesNotReorderTasks() = runBlocking {
        db.taskDao().insert(Task(7, 1, "Later priority", position = 9))
        db.taskDao().insert(Task(8, 1, "Earlier priority", position = 0))
        assertEquals(listOf(7L, 8L), repository.observeDashboardTasks().first().map { it.task.id })
        assertEquals(listOf(9, 0), db.taskDao().getAll().map { it.position })
        repository.deleteItem(Item(1, "Original group"))
        assertTrue(repository.observeDashboardTasks().first().isEmpty())
    }

    @Test fun existingSearchMatchesTitlesAndNotesAcrossStatusesOnly() = runBlocking {
        db.taskDao().insert(Task(1, 1, "Budget", status = TaskStatus.COMPLETED))
        db.taskDao().insert(Task(2, 1, "Other", notes = "BUDGET review", status = TaskStatus.SNOOZED))
        db.taskDao().insert(Task(3, 1, "Unrelated"))
        assertEquals(setOf(1L, 2L), repository.searchTasks("budget").first().map { it.task.id }.toSet())
        assertTrue(repository.searchTasks("Original group").first().isEmpty())
    }

    @Test fun clockTimezoneMidnightAndDatabaseEditsRefreshViewModel() = runBlocking {
        val clock = MutableStateFlow(DashboardMoment(86_399_000, "UTC"))
        db.taskDao().insert(Task(1, 1, "Boundary", dueAt = 86_400_000 + 23 * 3_600_000, status = TaskStatus.COMPLETED))
        val model = DashboardViewModel(repository, clock)
        val job = scope.launch { model.state.collect() }
        await { model.state.value.summary?.scheduled == 0 }
        clock.value = DashboardMoment(86_400_000, "UTC")
        await { model.state.value.summary?.completed == 1 }
        clock.value = DashboardMoment(86_400_000, "America/Los_Angeles")
        await { model.state.value.summary?.completed == 0 }
        db.taskDao().update(db.taskDao().getById(1)!!.copy(dueAt = null))
        await { model.state.value.summary?.scheduled == 0 }
        job.cancelAndJoin()
        await { model.state.value.loading }
    }

    @Test fun failureIsNotAnEmptySuccessAndRetryCanRecover() {
        var broken = true
        val dao = object : TaskDao by db.taskDao() {
            override fun observeDashboardTasks(): Flow<List<TaskWithItemName>> =
                if (broken) flow { throw IOException("Synthetic failure") } else db.taskDao().observeDashboardTasks()
        }
        val app = RuntimeEnvironment.getApplication()
        val repo = TaskRepository(db.itemDao(), dao, ReminderScheduler(app))
        val model = DashboardViewModel(repo, flowOf(DashboardMoment(10, "UTC")))
        scope.launch { model.state.collect() }
        await { model.state.value.failed }
        assertNull(model.state.value.summary)
        broken = false; model.retry()
        await { model.state.value.summary != null }
        assertFalse(model.state.value.failed)
    }

    @Test fun homeStyleInteractionCannotExtendVaultAwayDeadline() = runBlocking {
        var clock = 0L
        val vault = com.taskpro.app.vault.VaultRepository(db.vaultDao()) { clock }
        vault.unlock("synthetic away deadline master".toCharArray(), create = true)
        vault.confirmRecoverySaved()
        vault.setVisible(false)
        clock = 50_000; vault.touch()
        clock = 61_000; vault.touch(); vault.checkInactivity()
        assertNull(vault.state.value.document)
    }

    @Test fun sharedSearchViewModelDebouncesClearsAndReportsFailures() = runBlocking {
        var broken = true
        val dao = object : TaskDao by db.taskDao() {
            override fun searchTasks(query: String): Flow<List<TaskWithItemName>> =
                if (broken) flow { throw IOException("Synthetic failure") } else db.taskDao().searchTasks(query)
        }
        val repo = TaskRepository(db.itemDao(), dao, ReminderScheduler(RuntimeEnvironment.getApplication()))
        db.taskDao().insert(Task(1, 1, "Title", notes = "Needle in notes"))
        val model = SearchViewModel(repo)
        scope.launch { model.state.collect() }
        model.onQueryChange("  needle  ")
        await { model.state.value.failed }
        assertEquals("needle", model.state.value.query)
        broken = false; model.retry()
        await { model.state.value.results.size == 1 }
        model.onQueryChange("")
        await { model.state.value.query.isEmpty() && model.state.value.results.isEmpty() }
    }
}

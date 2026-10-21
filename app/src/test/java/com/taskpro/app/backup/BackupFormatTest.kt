package com.taskpro.app.backup

import com.taskpro.app.data.Item
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.settings.BackgroundStyle
import com.taskpro.app.settings.SettingsSnapshot
import com.taskpro.app.settings.ThemeMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.MessageDigest

class BackupFormatTest {
    private val snapshot = BackupSnapshot(
        items = listOf(Item(7, "گروه / Group", 0xff123456.toInt(), 1234)),
        tasks = TaskStatus.entries.mapIndexed { index, status ->
            Task(index + 10L, 7, "کار Task $index", "Line 1\nیادداشت 😀", if (index == 0) null else 1900000000000L,
                status, index, 5678)
        },
        settings = SettingsSnapshot(ThemeMode.DARK, BackgroundStyle.AURORA, 12345),
        createdAt = 12345,
        appVersion = "1.3.0"
    )

    @Test fun roundTripPreservesEveryFieldAndUnicode() {
        assertEquals(snapshot, BackupFormat.decode(BackupFormat.encode(snapshot)))
    }

    @Test fun emptyBackupAndDefaultPreferencesAreSupported() {
        val empty = snapshot.copy(items = emptyList(), tasks = emptyList(),
            settings = SettingsSnapshot(ThemeMode.SYSTEM, BackgroundStyle.GRADIENT, null))
        assertEquals(empty, BackupFormat.decode(BackupFormat.encode(empty)))
    }

    @Test fun newerAppCanUseTheSameFormatVersion() {
        val newer = snapshot.copy(appVersion = "99.0.0")
        assertEquals(newer, BackupFormat.decode(BackupFormat.encode(newer)))
    }

    @Test fun unknownFormatVersionIsRejected() {
        val root = JSONObject(String(BackupFormat.encode(snapshot), Charsets.UTF_8)).put("version", 2)
        assertThrows(UnsupportedBackupException::class.java) { BackupFormat.decode(root.toString().toByteArray()) }
    }

    @Test fun corruptionTruncationAndTrailingContentAreRejected() {
        val bytes = BackupFormat.encode(snapshot)
        val damaged = JSONObject(String(bytes, Charsets.UTF_8)).put("sha256", "invalid")
        for (input in listOf(damaged.toString().toByteArray(), bytes.copyOf(bytes.size / 2), bytes + "false".toByteArray())) {
            assertThrows(InvalidBackupException::class.java) { BackupFormat.decode(input) }
        }
    }

    @Test fun rejectsDuplicateIdsAndOrphanedTasks() {
        for (invalid in listOf(
            snapshot.copy(items = snapshot.items + snapshot.items),
            snapshot.copy(tasks = snapshot.tasks + snapshot.tasks.first()),
            snapshot.copy(items = emptyList())
        )) {
            assertThrows(InvalidBackupException::class.java) { BackupFormat.encode(invalid) }
        }
    }

    @Test fun rejectsInvalidTypesEnumsAndMissingFieldsEvenWithValidChecksum() {
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.getJSONArray("tasks").getJSONObject(0).put("id", "10") },
            { it.getJSONArray("tasks").getJSONObject(0).put("position", -1) },
            { it.getJSONArray("tasks").getJSONObject(0).put("dueAt", 1.5) },
            { it.getJSONArray("tasks").getJSONObject(0).put("status", "UNKNOWN") },
            { it.getJSONArray("tasks").getJSONObject(0).remove("notes") },
            { it.getJSONObject("settings").put("theme", "UNKNOWN") }
        )
        mutations.forEach { mutation ->
            val root = JSONObject(String(BackupFormat.encode(snapshot), Charsets.UTF_8))
            val payload = JSONObject(root.getString("dataJson"))
            mutation(payload)
            val text = payload.toString()
            root.put("dataJson", text)
            root.put("sha256", MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) })
            assertThrows(InvalidBackupException::class.java) { BackupFormat.decode(root.toString().toByteArray()) }
        }
    }

    @Test fun oversizedInputIsRejected() {
        assertThrows(InvalidBackupException::class.java) { BackupFormat.decode(ByteArray(BackupFormat.MAX_BYTES + 1)) }
    }

    @Test fun excessiveNestingIsRejectedWithoutOverflowingTheParser() {
        val nested = "[".repeat(10000) + "]".repeat(10000)
        assertThrows(InvalidBackupException::class.java) { BackupFormat.decode(nested.toByteArray()) }
    }
}

package com.taskpro.app.backup

import com.taskpro.app.data.Item
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.settings.BackgroundStyle
import com.taskpro.app.settings.SettingsSnapshot
import com.taskpro.app.settings.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.security.MessageDigest

data class BackupSnapshot(
    val items: List<Item>,
    val tasks: List<Task>,
    val settings: SettingsSnapshot,
    val createdAt: Long,
    val appVersion: String
)

class InvalidBackupException : Exception()
class UnsupportedBackupException : Exception()

/** Portable data, independent of Room's on-device schema and WAL files. */
object BackupFormat {
    const val VERSION = 1
    const val MAX_BYTES = 20 * 1024 * 1024
    private const val MAX_RECORDS = 100_000
    private const val MAX_TIMESTAMP = 253402300799999L

    fun encode(snapshot: BackupSnapshot): ByteArray {
        val payload = JSONObject().apply {
            put("createdAt", snapshot.createdAt)
            put("appVersion", snapshot.appVersion)
            put("settings", JSONObject().apply {
                put("theme", snapshot.settings.theme.name)
                put("background", snapshot.settings.background.name)
                put("lastBackupAt", snapshot.settings.lastBackupAt ?: JSONObject.NULL)
            })
            put("items", JSONArray().apply {
                snapshot.items.forEach { item -> put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("colorArgb", item.colorArgb ?: JSONObject.NULL)
                    put("createdAt", item.createdAt)
                }) }
            })
            put("tasks", JSONArray().apply {
                snapshot.tasks.forEach { task -> put(JSONObject().apply {
                    put("id", task.id)
                    put("itemId", task.itemId)
                    put("title", task.title)
                    put("notes", task.notes)
                    put("dueAt", task.dueAt ?: JSONObject.NULL)
                    put("status", task.status.name)
                    put("position", task.position)
                    put("createdAt", task.createdAt)
                }) }
            })
        }.toString()
        val bytes = JSONObject().apply {
            put("format", "com.taskpro.backup")
            put("version", VERSION)
            // Hash the exact UTF-8 payload, without relying on JSON key ordering.
            put("dataJson", payload)
            put("sha256", digest(payload))
        }.toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Backup exceeds size limit" }
        decode(bytes) // Never export a file that this version cannot restore.
        return bytes
    }

    fun decode(bytes: ByteArray): BackupSnapshot {
        try {
            require(bytes.size <= MAX_BYTES)
            val root = parseObject(Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString())
            require(root.string("format") == "com.taskpro.backup")
            if (root.integer("version") != VERSION.toLong()) throw UnsupportedBackupException()
            val payload = root.string("dataJson")
            require(root.string("sha256") == digest(payload))
            val data = parseObject(payload)
            val preferences = data.getJSONObject("settings")
            val settings = SettingsSnapshot(
                ThemeMode.valueOf(preferences.string("theme")),
                BackgroundStyle.valueOf(preferences.string("background")),
                preferences.nullableInteger("lastBackupAt")?.also { require(it in 0..MAX_TIMESTAMP) }
            )
            val itemArray = data.getJSONArray("items")
            val taskArray = data.getJSONArray("tasks")
            require(itemArray.length() <= MAX_RECORDS && taskArray.length() <= MAX_RECORDS)
            val items = (0 until itemArray.length()).map { index ->
                val item = itemArray.getJSONObject(index)
                Item(
                    id = item.id("id"),
                    name = item.string("name").also { require(it.isNotBlank()) },
                    colorArgb = item.nullableInteger("colorArgb")?.also {
                        require(it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
                    }?.toInt(),
                    createdAt = item.timestamp("createdAt")
                )
            }
            val ids = items.map { it.id }.toSet()
            require(ids.size == items.size)
            val tasks = (0 until taskArray.length()).map { index ->
                val task = taskArray.getJSONObject(index)
                Task(
                    id = task.id("id"),
                    itemId = task.id("itemId").also { require(it in ids) },
                    title = task.string("title").also { require(it.isNotBlank()) },
                    notes = task.string("notes"),
                    dueAt = task.nullableInteger("dueAt")?.also { require(it in 0..MAX_TIMESTAMP) },
                    status = TaskStatus.valueOf(task.string("status")),
                    position = task.integer("position").also { require(it in 0..Int.MAX_VALUE.toLong()) }.toInt(),
                    createdAt = task.timestamp("createdAt")
                )
            }
            require(tasks.map { it.id }.toSet().size == tasks.size)
            val appVersion = data.string("appVersion").also { require(it.isNotBlank() && it.length <= 100) }
            return BackupSnapshot(items, tasks, settings, data.timestamp("createdAt"), appVersion)
        } catch (e: UnsupportedBackupException) {
            throw e
        } catch (e: Exception) {
            throw InvalidBackupException()
        }
    }

    internal fun parseObject(text: String): JSONObject {
        // Bound nesting before invoking the recursive JSON parser on an external file.
        var depth = 0
        var quote: Char? = null
        var escaped = false
        text.forEach { char ->
            if (quote != null) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == quote) quote = null
            } else when (char) {
                '"' -> quote = char
                '/', '\'' -> throw InvalidBackupException()
                '{', '[' -> { depth++; require(depth <= 16) }
                '}', ']' -> { depth--; require(depth >= 0) }
            }
        }
        require(quote == null && depth == 0)
        val tokener = JSONTokener(text)
        val result = tokener.nextValue() as JSONObject
        require(tokener.nextClean() == '\u0000')
        return result
    }

    private fun digest(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun JSONObject.string(key: String): String = get(key) as String
    private fun JSONObject.integer(key: String): Long {
        val value = get(key)
        require(value is Int || value is Long)
        return (value as Number).toLong()
    }
    private fun JSONObject.nullableInteger(key: String): Long? {
        require(has(key))
        return if (isNull(key)) null else integer(key)
    }
    private fun JSONObject.id(key: String) = integer(key).also { require(it in 1 until Long.MAX_VALUE) }
    private fun JSONObject.timestamp(key: String) = integer(key).also { require(it in 0..MAX_TIMESTAMP) }
}

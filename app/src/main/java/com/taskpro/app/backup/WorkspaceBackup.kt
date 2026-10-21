package com.taskpro.app.backup

import com.taskpro.app.vault.VaultCrypto
import org.json.JSONObject

enum class BackupScope { FULL, TASKS, VAULT }
data class WorkspaceBackup(
    val tasks: BackupSnapshot?, val vault: String?, val createdAt: Long, val appVersion: String,
    val legacy: Boolean = false
)

object WorkspaceBackupFormat {
    const val MAX_BYTES = 130 * 1024 * 1024
    fun encode(snapshot: WorkspaceBackup, password: CharArray): ByteArray {
        require(snapshot.tasks != null || snapshot.vault != null)
        val plain = JSONObject().put("version", 2).put("createdAt", snapshot.createdAt).put("appVersion", snapshot.appVersion)
            .put("tasks", snapshot.tasks?.let { String(BackupFormat.encode(it), Charsets.UTF_8) } ?: JSONObject.NULL)
            .put("vault", snapshot.vault ?: JSONObject.NULL).toString().toByteArray(Charsets.UTF_8)
        return try {
            JSONObject().put("format", "com.taskpro.workspace").put("version", 2)
                .put("envelope", VaultCrypto.create(password, plain, "backup")).toString().toByteArray(Charsets.UTF_8)
                .also { require(it.size <= MAX_BYTES) }
        } finally { plain.fill(0); password.fill('\u0000') }
    }

    fun decode(bytes: ByteArray, password: CharArray): WorkspaceBackup {
        try {
            require(bytes.size <= MAX_BYTES)
            val json = BackupFormat.parseObject(String(bytes, Charsets.UTF_8))
            if (json.getString("format") == "com.taskpro.backup") {
                val legacy = BackupFormat.decode(bytes)
                return WorkspaceBackup(legacy, null, legacy.createdAt, legacy.appVersion, true)
            }
            require(json.getString("format") == "com.taskpro.workspace")
            if (json.getInt("version") != 2) throw UnsupportedBackupException()
            val plain = VaultCrypto.open(json.getString("envelope"), password, "backup")
            try {
                val data = BackupFormat.parseObject(String(plain, Charsets.UTF_8))
                require(data.getInt("version") == 2)
                val tasks = if (data.isNull("tasks")) null else BackupFormat.decode(data.getString("tasks").toByteArray(Charsets.UTF_8))
                val vault = if (data.isNull("vault")) null else data.getString("vault").also { VaultCrypto.inspect(it) }
                require(tasks != null || vault != null)
                return WorkspaceBackup(tasks, vault, data.getLong("createdAt"), data.getString("appVersion"))
            } finally { plain.fill(0) }
        } finally { password.fill('\u0000') }
    }
}

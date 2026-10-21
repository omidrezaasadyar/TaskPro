package com.taskpro.app.branding

import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import com.taskpro.app.BuildConfig
import com.taskpro.app.R
import com.taskpro.app.TaskProApp
import com.taskpro.app.backup.BackupScope
import com.taskpro.app.backup.WorkspaceBackup
import com.taskpro.app.backup.WorkspaceBackupFormat
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.notification.ReminderScheduler
import com.taskpro.app.settings.BackgroundStyle
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.navigation.Screen
import com.taskpro.app.vault.VaultClipboard
import com.taskpro.app.vault.VaultCrypto
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS")
class BrandingCompatibilityTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun englishLauncherAndPublicLabelsUseApprovedBrand() {
        assertEquals("Vaurivo", context.applicationInfo.loadLabel(context.packageManager).toString())
        assertEquals("Vaurivo", context.getString(R.string.app_name))
        assertEquals("Tasks", context.getString(R.string.workspace_tasks))
        assertEquals("Vault", context.getString(R.string.vault_title))
        assertEquals("Vaurivo | v${BuildConfig.VERSION_NAME}", context.getString(R.string.vault_version, BuildConfig.VERSION_NAME))
        assertEquals("Vaurivo backup", context.getString(R.string.backup_share_label))
        assertTrue(context.getString(R.string.settings_about).contains("Vaurivo"))
        assertTrue(context.getString(R.string.vault_recovery_save_note).contains("Vaurivo"))
    }

    @Test @Config(qualifiers = "fa-rIR") fun persianLauncherKeepsLatinBrandAndLocalizedModules() {
        assertEquals("Vaurivo", context.applicationInfo.loadLabel(context.packageManager).toString())
        assertEquals("Vaurivo", context.getString(R.string.app_name))
        assertEquals("کارها", context.getString(R.string.workspace_tasks))
        assertEquals("گاوصندوق", context.getString(R.string.vault_title))
        assertEquals(View.LAYOUT_DIRECTION_RTL, context.resources.configuration.layoutDirection)
        assertEquals("Vaurivo | v${BuildConfig.VERSION_NAME}", context.getString(R.string.vault_version, BuildConfig.VERSION_NAME))
        assertEquals("پشتیبان Vaurivo", context.getString(R.string.backup_share_label))
        assertTrue(context.getString(R.string.settings_about).contains("Vaurivo"))
        assertTrue(context.getString(R.string.vault_recovery_save_note).contains("Vaurivo"))
    }

    @Test fun allEnglishAndPersianDisplayResourcesExcludeRetiredBrandAndModuleNames() {
        val retired = Regex("Task\\s?Pro|تسک[\\s\\u200c-]*پرو|Task Manager|Secure Vault|گاوصندوق امن|VAURIVO", RegexOption.IGNORE_CASE)
        for (locale in listOf("values", "values-fa")) {
            strings(locale).forEach { (key, value) ->
                // Vaurivo is case-sensitive even though retired-brand detection is not.
                assertFalse("Retired display text in $locale/$key", retired.containsMatchIn(value.replace("Vaurivo", "")))
            }
        }
    }

    @Test fun everyPublicStringHasPersianTranslationAndMatchingFormatArguments() {
        val english = strings("values")
        val persian = strings("values-fa")
        assertEquals(english.keys, persian.keys)
        val argument = Regex("%(?:\\d+\\$)?[ds]")
        english.forEach { (key, value) ->
            assertEquals("Format arguments for $key", argument.findAll(value).map { it.value }.sorted().toList(),
                argument.findAll(persian.getValue(key)).map { it.value }.sorted().toList())
        }
    }

    @Test fun clipboardDisplayBrandChangesWithoutChangingOwnershipOrSensitiveHandling() {
        val clipboard = VaultClipboard(context)
        val manager = context.getSystemService(ClipboardManager::class.java)
        clipboard.copy("synthetic test value")
        val description = checkNotNull(manager.primaryClipDescription)
        assertEquals("Vaurivo", description.label.toString())
        assertTrue(description.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
        assertNotNull(description.extras!!.getString("taskpro.clip"))
        clipboard.clearOwned()
        assertFalse(manager.hasPrimaryClip())
        clipboard.copy("another synthetic test value")
        manager.setPrimaryClip(ClipData.newPlainText("Other application", "unrelated content"))
        clipboard.clearOwned()
        assertEquals("unrelated content", manager.primaryClip!!.getItemAt(0).text.toString())
    }

    @Test fun originalTaskProLegacyBackupStillDecodesWithStableIdsAndPreferences() {
        // Deliberately independent of the current encoder and display resource names.
        val payload = """{"createdAt":1,"appVersion":"1.3.0","settings":{"theme":"DARK","background":"AURORA","lastBackupAt":null},"items":[{"id":42,"name":"Synthetic group","colorArgb":null,"createdAt":1}],"tasks":[{"id":77,"itemId":42,"title":"Synthetic task","notes":"Preserved note","dueAt":null,"status":"PENDING","position":0,"createdAt":1}]}"""
        val checksum = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val legacy = JSONObject().put("format", "com.taskpro.backup").put("version", 1)
            .put("dataJson", payload).put("sha256", checksum).toString().toByteArray(Charsets.UTF_8)
        val decoded = WorkspaceBackupFormat.decode(legacy, charArrayOf())
        assertTrue(decoded.legacy)
        assertNull(decoded.vault)
        val tasks = checkNotNull(decoded.tasks)
        assertEquals(42L, tasks.items.single().id)
        assertEquals(77L, tasks.tasks.single().id)
        assertEquals(42L, tasks.tasks.single().itemId)
        assertEquals("Preserved note", tasks.tasks.single().notes)
        assertEquals(TaskStatus.PENDING, tasks.tasks.single().status)
        assertEquals(ThemeMode.DARK, tasks.settings.theme)
        assertEquals(BackgroundStyle.AURORA, tasks.settings.background)
    }

    @Test fun encryptedBackupRetainsOriginalProtocolKdfAndTaskProAssociatedData() {
        val password = "synthetic backup test phrase"
        val vault = VaultCrypto.create(password.toCharArray(), "synthetic record".toByteArray(), "vault")
        val snapshot = WorkspaceBackup(null, vault, 1234, "2.0.4")
        val bytes = WorkspaceBackupFormat.encode(snapshot, password.toCharArray())
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        assertEquals("com.taskpro.workspace", root.getString("format"))
        assertEquals(2, root.getInt("version"))
        assertEquals(snapshot, WorkspaceBackupFormat.decode(bytes, password.toCharArray()))
        val envelope = root.getString("envelope")
        val json = JSONObject(envelope)
        assertEquals(1, json.getInt("version"))
        assertEquals("argon2id", json.getString("kdf"))
        assertEquals(19456, json.getInt("memory"))
        assertEquals(2, json.getInt("iterations"))
        assertEquals(1, json.getInt("parallelism"))
        val key = VaultCrypto.unwrap(envelope, password.toCharArray(), "backup")
        val plain = try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, hex(json.getString("dataNonce"))))
                updateAAD("TaskPro:backup:data:1:argon2id:19456:2:1".toByteArray(Charsets.UTF_8))
                doFinal(hex(json.getString("data")))
            }
        } finally { key.fill(0) }
        try { assertEquals(vault, JSONObject(String(plain, Charsets.UTF_8)).getString("vault")) }
        finally { plain.fill(0) }
    }

    @Test fun installationNavigationReminderAndPersistedEnumsStayUnchanged() {
        assertEquals("com.taskpro.app", BuildConfig.APPLICATION_ID)
        assertEquals("com.taskpro.app", R::class.java.`package`!!.name)
        assertEquals("task_reminders", TaskProApp.REMINDER_CHANNEL_ID)
        assertEquals("com.taskpro.app.ACTION_REMINDER", ReminderScheduler.ACTION_REMINDER)
        assertEquals("extra_task_id", ReminderScheduler.EXTRA_TASK_ID)
        assertEquals("extra_task_title", ReminderScheduler.EXTRA_TASK_TITLE)
        assertEquals(listOf("workspace", "home", "vault", "search", "settings", "item/{itemId}", "item/{itemId}/status/{status}"),
            listOf(Screen.Workspace, Screen.Home, Screen.Vault, Screen.Search, Screen.Settings, Screen.ItemDetail, Screen.ItemStatusList).map { it.route })
        assertEquals(listOf("PENDING", "COMPLETED", "SNOOZED"), TaskStatus.entries.map { it.name })
        assertEquals(listOf("SYSTEM", "LIGHT", "DARK"), ThemeMode.entries.map { it.name })
        assertEquals(listOf("PLAIN", "GRADIENT", "AURORA"), BackgroundStyle.entries.map { it.name })
        assertEquals(listOf("FULL", "TASKS", "VAULT"), BackupScope.entries.map { it.name })
    }

    private fun hex(text: String) = text.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun strings(qualifier: String): Map<String, String> {
        val resources = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val result = linkedMapOf<String, String>()
        val parser = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder()
        File(resources, qualifier).listFiles()!!.filter { it.extension == "xml" }.forEach { file ->
            val nodes = parser.parse(file).getElementsByTagName("string")
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                val key = node.attributes.getNamedItem("name").nodeValue
                assertNull("Duplicate resource $qualifier/$key", result.put(key, node.textContent))
            }
        }
        assertTrue(result.isNotEmpty())
        return result
    }
}

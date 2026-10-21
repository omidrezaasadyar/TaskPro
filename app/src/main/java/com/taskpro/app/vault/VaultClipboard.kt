package com.taskpro.app.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.taskpro.app.R
import kotlinx.coroutines.*
import java.util.UUID
import java.security.MessageDigest

/** Clear only a clip still owned by this copy action; OS/background restrictions may prevent it. */
class VaultClipboard(context: Context) {
    private val label = context.getString(R.string.app_name)
    private val manager = context.getSystemService(ClipboardManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var owned: String? = null
    private var ownedHash: ByteArray? = null
    fun copy(value: String) {
        val token = UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(label, value)
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
            putString("taskpro.clip", token)
        }
        manager.setPrimaryClip(clip)
        owned = token
        ownedHash?.fill(0)
        ownedHash = hash(value)
        job?.cancel()
        job = scope.launch { delay(30_000); clearOwned() }
    }
    fun clearOwned() {
        val token = owned ?: return
        runCatching {
            val clip = manager.primaryClip
            val sameContent = clip?.itemCount == 1 && clip.getItemAt(0).text?.let { text ->
                MessageDigest.isEqual(ownedHash, hash(text.toString()))
            } == true
            if (sameContent && manager.primaryClipDescription?.extras?.getString("taskpro.clip") == token) {
                if (Build.VERSION.SDK_INT >= 28) manager.clearPrimaryClip()
                else manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
        owned = null
        ownedHash?.fill(0); ownedHash = null
    }
    private fun hash(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        return try { MessageDigest.getInstance("SHA-256").digest(bytes) } finally { bytes.fill(0) }
    }
}

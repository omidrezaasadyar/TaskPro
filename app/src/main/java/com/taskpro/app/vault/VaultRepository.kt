package com.taskpro.app.vault

import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

data class VaultState(val exists: Boolean = false, val document: VaultDocument? = null, val generation: Long = 0,
                      val pendingRecovery: String? = null)

class VaultRepository(private val dao: VaultDao, private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime) {
    companion object { const val AWAY_LOCK_MILLIS = 60_000L }
    private var visible = true
    private var foreground = true
    private var awaySince: Long? = null
    internal val mutex = Mutex()
    private val monitor = Any()
    private var key: ByteArray? = null
    private var envelope: String? = null
    private var revision = 0L
    private var generation = 0L
    private var touched = elapsedRealtime()
    private val mutableState = MutableStateFlow(VaultState())
    val state = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            while (isActive) {
                delay(1000)
                checkInactivity()
            }
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val exists = dao.get() != null
        synchronized(monitor) { mutableState.value = mutableState.value.copy(exists = exists) }
    }

    fun touch() = synchronized(monitor) { touched = elapsedRealtime() }
    fun setVisible(value: Boolean) = synchronized(monitor) {
        checkInactivity(); visible = value; updateAway()
    }
    fun setForeground(value: Boolean) = synchronized(monitor) {
        checkInactivity(); foreground = value; updateAway()
    }
    private fun updateAway() {
        if (visible && foreground) { awaySince = null; touch() }
        else if (awaySince == null) awaySince = elapsedRealtime()
    }
    internal fun checkInactivity() = synchronized(monitor) {
        val doc = mutableState.value.document
        val since = awaySince
        if (doc != null && (if (since != null) elapsedRealtime() - since >= AWAY_LOCK_MILLIS
            else elapsedRealtime() - touched >= doc.autoLockSeconds * 1000L)) lock()
    }

    suspend fun credentialBinding(): String = withContext(Dispatchers.IO) {
        binding(checkNotNull(dao.get()).envelope)
    }
    private fun binding(value: String): String {
        val json = VaultCrypto.inspect(value)
        return android.util.Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(
            (json.getString("salt") + json.getString("wrappedKey")).toByteArray()), android.util.Base64.NO_WRAP)
    }
    suspend fun authenticateDeviceSetup(password: CharArray): Pair<String, ByteArray> = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                val stored = checkNotNull(dao.get())
                val candidate = VaultCrypto.unwrap(stored.envelope, password, "vault")
                try {
                    val plain = VaultCrypto.decrypt(stored.envelope, candidate, "vault")
                    try { VaultDocument.decode(plain) } finally { plain.fill(0) }
                    binding(stored.envelope) to candidate.copyOf()
                } finally { candidate.fill(0) }
            }
        } finally { password.fill('\u0000') }
    }
    suspend fun unlockDevice(candidate: ByteArray, expectedBinding: String, token: Long) = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                val stored = checkNotNull(dao.get())
                check(binding(stored.envelope) == expectedBinding)
                val plain = VaultCrypto.decrypt(stored.envelope, candidate, "vault")
                val doc = try { VaultDocument.decode(plain) } finally { plain.fill(0) }
                val pending = if (VaultCrypto.inspect(stored.envelope).optJSONObject("recovery")?.optBoolean("confirmed", true) == false)
                    VaultCrypto.viewRecovery(stored.envelope, candidate) else null
                synchronized(monitor) {
                    check(token == generation && foreground && visible)
                    key?.fill(0); key = candidate.copyOf(); envelope = stored.envelope; revision = stored.revision
                    touch(); mutableState.value = VaultState(true, doc, generation, pending)
                }
            }
        } finally { candidate.fill(0) }
    }
    fun lock() = synchronized(monitor) {
        generation++
        key?.fill(0); key = null; envelope = null
        mutableState.value = VaultState(mutableState.value.exists, generation = generation)
    }

    private fun newVaultEnvelope(password: CharArray): String {
        val plain = VaultDocument().encode()
        return try {
            val initial = VaultCrypto.create(password, plain, "vault")
            val dataKey = VaultCrypto.unwrap(initial, password, "vault")
            try {
                val json = VaultCrypto.inspect(VaultCrypto.enableRecovery(initial, dataKey, VaultCrypto.newRecoveryKey().toCharArray()))
                json.getJSONObject("recovery").put("confirmed", false)
                json.toString()
            } finally { dataKey.fill(0) }
        } finally { plain.fill(0) }
    }

    /** Explicit new-vault creation; the previous encrypted vault must be durably exported first. */
    suspend fun createAfterPreservingBackup(password: CharArray, saveBackup: suspend (ByteArray) -> Unit) = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                val stored = checkNotNull(dao.get())
                check(state.value.document == null)
                val token = state.value.generation
                val replacement = newVaultEnvelope(password)
                val candidate = VaultCrypto.unwrap(replacement, password, "vault")
                try {
                    val pending = VaultCrypto.viewRecovery(replacement, candidate)
                    val snapshot = com.taskpro.app.backup.WorkspaceBackup(null, stored.envelope, System.currentTimeMillis(), com.taskpro.app.BuildConfig.VERSION_NAME)
                    val bytes = com.taskpro.app.backup.WorkspaceBackupFormat.encode(snapshot, password.copyOf())
                    try { saveBackup(bytes) } finally { bytes.fill(0) }
                    currentCoroutineContext().ensureActive()
                    check(state.value.generation == token && state.value.document == null)
                    withContext(NonCancellable) {
                        check(dao.update(replacement, stored.revision) == 1) { "Vault changed during setup" }
                        synchronized(monitor) {
                            val sessionCurrent = generation == token
                            generation++; key?.fill(0); key = null; envelope = null
                            if (sessionCurrent && foreground && visible) {
                                key = candidate.copyOf(); envelope = replacement; revision = stored.revision + 1; touch()
                                mutableState.value = VaultState(true, VaultDocument(), generation, pending)
                            } else mutableState.value = VaultState(true, generation = generation)
                        }
                    }
                } finally { candidate.fill(0) }
            }
        } finally { password.fill('\u0000') }
    }

    suspend fun unlock(password: CharArray, create: Boolean = false) = withContext(Dispatchers.IO) {
        val token = synchronized(monitor) { generation }
        try {
            mutex.withLock {
                var stored = dao.get()
                if (create) {
                    check(stored == null)
                    stored = VaultStore(envelope = newVaultEnvelope(password))
                    dao.insert(stored)
                    synchronized(monitor) { mutableState.value = mutableState.value.copy(exists = true) }
                }
                check(stored != null)
                val candidate = VaultCrypto.unwrap(stored.envelope, password, "vault")
                try {
                    val plain = VaultCrypto.decrypt(stored.envelope, candidate, "vault")
                    val document = try { VaultDocument.decode(plain) } finally { plain.fill(0) }
                    synchronized(monitor) {
                        check(token == generation && foreground && visible) { "Session ended" }
                        key?.fill(0); key = candidate.copyOf()
                        envelope = stored.envelope; revision = stored.revision
                        touched = elapsedRealtime()
                        val recovery = VaultCrypto.inspect(stored.envelope).optJSONObject("recovery")
                        val pending = if (recovery?.optBoolean("confirmed", true) == false)
                            VaultCrypto.viewRecovery(stored.envelope, candidate) else null
                        mutableState.value = VaultState(true, document, generation, pending)
                    }
                } finally { candidate.fill(0) }
            }
        } finally { password.fill('\u0000') }
    }

    suspend fun edit(transform: (VaultDocument) -> VaultDocument) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token: Long
            val localKey: ByteArray
            val oldEnvelope: String
            val oldRevision: Long
            val doc: VaultDocument
            synchronized(monitor) {
                doc = checkNotNull(mutableState.value.document)
                localKey = checkNotNull(key).copyOf(); oldEnvelope = checkNotNull(envelope)
                token = generation; oldRevision = revision
            }
            try {
                val updated = transform(doc)
                val plain = updated.encode()
                val encrypted = try { VaultCrypto.encrypt(oldEnvelope, localKey, plain, "vault") } finally { plain.fill(0) }
                check(dao.update(encrypted, oldRevision) == 1) { "Vault changed; unlock again" }
                synchronized(monitor) {
                    if (token == generation && key != null) {
                        envelope = encrypted; revision++; touch()
                        mutableState.value = mutableState.value.copy(document = updated)
                    }
                }
            } finally { localKey.fill(0) }
        }
    }

    suspend fun changePassword(old: CharArray, new: CharArray) = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                val stored = checkNotNull(dao.get())
                val localKey = VaultCrypto.unwrap(stored.envelope, old, "vault")
                try {
                    synchronized(monitor) { check(key != null && MessageDigest.isEqual(key, localKey)) }
                    val plain = VaultCrypto.decrypt(stored.envelope, localKey, "vault")
                    val encrypted = try { VaultCrypto.retainRecovery(stored.envelope, VaultCrypto.wrap(new, localKey, plain, "vault")) } finally { plain.fill(0) }
                    check(dao.update(encrypted, stored.revision) == 1)
                    lock()
                } finally { localKey.fill(0) }
            }
        } finally { old.fill('\u0000'); new.fill('\u0000') }
    }

    suspend fun setupRecovery(password: CharArray, viewOnly: Boolean = false): String = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                val token = synchronized(monitor) { checkNotNull(key); generation }
                val stored = checkNotNull(dao.get())
                val candidate = VaultCrypto.unwrap(stored.envelope, password, "vault")
                try {
                    synchronized(monitor) { check(token == generation && MessageDigest.isEqual(key, candidate)) }
                    if (viewOnly) {
                        val result = VaultCrypto.viewRecovery(stored.envelope, candidate)
                        synchronized(monitor) { check(token == generation && key != null); touch() }
                        return@withLock result
                    }
                    val recovery = VaultCrypto.newRecoveryKey()
                    val nextKey = VaultCrypto.randomBytes(32)
                    val plain = VaultCrypto.decrypt(stored.envelope, candidate, "vault")
                    try {
                        // Rotate the data key so an old recovery wrapper cannot decrypt future records.
                        val updated = VaultCrypto.enableRecovery(VaultCrypto.wrap(password, nextKey, plain, "vault"), nextKey, recovery.toCharArray())
                        check(dao.update(updated, stored.revision) == 1)
                        synchronized(monitor) {
                            check(token == generation && key != null)
                            key?.fill(0); key = nextKey.copyOf()
                            envelope = updated; revision = stored.revision + 1; touch()
                        }
                    } finally { nextKey.fill(0); plain.fill(0) }
                    recovery
                } finally { candidate.fill(0) }
            }
        } finally { password.fill('\u0000') }
    }

    suspend fun confirmRecoverySaved() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = synchronized(monitor) { checkNotNull(key); checkNotNull(mutableState.value.pendingRecovery); generation }
            val stored = checkNotNull(dao.get())
            val json = VaultCrypto.inspect(stored.envelope)
            json.getJSONObject("recovery").put("confirmed", true)
            val updated = json.toString()
            check(dao.update(updated, stored.revision) == 1)
            synchronized(monitor) {
                if (key != null && generation == token) {
                    envelope = updated; revision = stored.revision + 1
                    mutableState.value = mutableState.value.copy(pendingRecovery = null)
                }
            }
        }
    }

    suspend fun recoverPassword(recovery: CharArray, newPassword: CharArray) = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                val stored = checkNotNull(dao.get())
                val updated = VaultCrypto.recover(stored.envelope, recovery, newPassword)
                check(dao.update(updated, stored.revision) == 1)
                lock()
            }
        } finally { recovery.fill('\u0000'); newPassword.fill('\u0000') }
    }
}

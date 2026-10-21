package com.taskpro.app.vault

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-bound convenience credentials. Never exported with the portable vault. */
class VaultDeviceUnlock(context: Context, private val repository: VaultRepository,
    private val storage: File = File(context.noBackupFilesDir, "vault-device-unlock.json"),
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val elapsedClock: () -> Long = SystemClock::elapsedRealtime,
    private val cipherProvider: ((Int, Boolean, String, ByteArray?) -> Cipher)? = null) {
    private val file = AtomicFile(storage)
    private val mutex = Mutex()
    private var blockedUntilElapsed = 0L
    enum class FailureStage(val code: String) {
        MASTER_PASSWORD("DU01"), PIN_ENCRYPTION("DU02"), KEYSTORE("DU03"), STORAGE("DU04"), SESSION_CHANGED("DU05")
    }
    class Failure(val stage: FailureStage, cause: Exception) : Exception(stage.code, cause)
    class WrongPin(cause: Exception) : Exception("Incorrect PIN", cause)
    private inline fun <T> stage(value: FailureStage, block: () -> T): T = try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Failure) { throw e }
        catch (e: Exception) { throw Failure(value, e) }
    private fun read(): JSONObject = stage(FailureStage.STORAGE) {
        if (!file.baseFile.exists()) JSONObject() else
            file.openRead().use { JSONObject(it.readBytes().toString(Charsets.UTF_8)) }
    }
    private fun write(json: JSONObject) = stage(FailureStage.STORAGE) {
        check(!file.baseFile.exists() || file.baseFile.isFile)
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        // AtomicFile can log some commit failures instead of throwing; do not report success without readback.
        file.openRead().use { check(it.readBytes().contentEquals(bytes)) }
    }
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)
    private fun key(alias: String, biometric: Boolean, create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create)
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        if (biometric) {
            spec.setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            else @Suppress("DEPRECATION") spec.setUserAuthenticationValidityDurationSeconds(-1)
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(spec.build()); generateKey()
        }
    }
    private fun cipher(mode: Int, biometric: Boolean, binding: String, nonce: ByteArray? = null): Cipher = stage(FailureStage.KEYSTORE) {
        val alias = if (biometric) "taskpro.vault.biometric.v1" else "taskpro.vault.pin.v1"
        val result = cipherProvider?.invoke(mode, biometric, binding, nonce) ?: Cipher.getInstance("AES/GCM/NoPadding").apply {
            val secret = key(alias, biometric, mode == Cipher.ENCRYPT_MODE)
            if (nonce == null) init(mode, secret) else init(mode, secret, GCMParameterSpec(128, nonce))
        }
        if (!biometric) result.updateAAD("TaskPro:device:pin:$binding".toByteArray(Charsets.UTF_8))
        result
    }
    suspend fun available(): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val binding = repository.credentialBinding()
            val json = read()
            (json.optJSONObject("pin")?.optString("binding") == binding) to
                (json.optJSONObject("bio")?.optString("binding") == binding)
        }
    }
    suspend fun setPin(password: CharArray, pin: CharArray) = withContext(Dispatchers.IO) {
        try {
            require(pin.size in 4..6 && pin.all { it in '0'..'9' })
            mutex.withLock {
                val (binding, dataKey) = stage(FailureStage.MASTER_PASSWORD) { repository.authenticateDeviceSetup(password) }
                try {
                    // Argon2id protects the PIN layer; Keystore prevents portable offline PIN guessing.
                    val pass = ("TaskPro PIN:" + String(pin)).toCharArray()
                    val inner = try { stage(FailureStage.PIN_ENCRYPTION) { VaultCrypto.create(pass, dataKey, "device-pin").toByteArray() } } finally { pass.fill('\u0000') }
                    val record = try {
                        stage(FailureStage.KEYSTORE) {
                            val encrypted = cipher(Cipher.ENCRYPT_MODE, false, binding)
                            val nonce = checkNotNull(encrypted.iv).copyOf()
                            val data = encrypted.doFinal(inner)
                            // Verify the actual device provider before replacing a working PIN profile.
                            val verified = cipher(Cipher.DECRYPT_MODE, false, binding, nonce).doFinal(data)
                            try { check(java.security.MessageDigest.isEqual(inner, verified)) } finally { verified.fill(0) }
                            JSONObject().put("binding", binding).put("nonce", encode(nonce)).put("data", encode(data))
                        }
                    } finally { inner.fill(0) }
                    stage(FailureStage.SESSION_CHANGED) { check(repository.credentialBinding() == binding) }
                    write(runCatching { read() }.getOrElse { JSONObject() }.put("pin", record).put("failures", 0).put("until", 0))
                    blockedUntilElapsed = 0
                } finally { dataKey.fill(0) }
            }
        } finally { password.fill('\u0000'); pin.fill('\u0000') }
    }
    suspend fun remainingSeconds(): Int = withContext(Dispatchers.IO) {
        mutex.withLock { remaining(read()) }
    }
    private fun remaining(json: JSONObject): Int {
        val left = maxOf(json.optLong("until") - wallClock(), blockedUntilElapsed - elapsedClock())
        return ((left.coerceIn(0, 30_000) + 999) / 1000).toInt()
    }
    suspend fun unlockPin(pin: CharArray) = withContext(Dispatchers.IO) {
        val token = repository.state.value.generation
        try {
            mutex.withLock {
                val json = read()
                check(remaining(json) == 0)
                val failures = if (json.optLong("until") != 0L) 1 else json.optInt("failures") + 1
                json.put("failures", failures).put("until", 0)
                if (failures >= 5) {
                    json.put("until", wallClock() + 30_000)
                    blockedUntilElapsed = elapsedClock() + 30_000
                }
                write(json) // Charge the attempt before doing cryptography, including interrupted attempts.
                val record = json.getJSONObject("pin")
                val binding = record.getString("binding")
                val inner = stage(FailureStage.KEYSTORE) { cipher(Cipher.DECRYPT_MODE, false, binding, decode(record.getString("nonce")))
                    .doFinal(decode(record.getString("data"))) }
                val pass = ("TaskPro PIN:" + String(pin)).toCharArray()
                val candidate = try { VaultCrypto.open(inner.toString(Charsets.UTF_8), pass, "device-pin") }
                    catch (e: javax.crypto.BadPaddingException) { throw WrongPin(e) }
                    finally { inner.fill(0); pass.fill('\u0000') }
                repository.unlockDevice(candidate, binding, token)
                write(json.put("failures", 0).put("until", 0)); blockedUntilElapsed = 0
            }
        } finally { pin.fill('\u0000') }
    }
    class BiometricOperation(val cipher: Cipher, val binding: String, val bytes: ByteArray, val setup: Boolean, val token: Long) {
        private var cleared = false
        @Synchronized fun clear() { bytes.fill(0); cleared = true }
        @Synchronized fun consume(authenticated: Cipher): ByteArray {
            check(!cleared && authenticated === cipher)
            return try {
                // Auth-per-use Keystore operations must not receive data before BiometricPrompt succeeds.
                authenticated.updateAAD("TaskPro:device:bio:$binding".toByteArray(Charsets.UTF_8))
                authenticated.doFinal(bytes)
            } finally { clear() }
        }
    }
    suspend fun prepareBiometric(password: CharArray? = null): BiometricOperation = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                val token = repository.state.value.generation
                if (password != null) {
                    val (binding, dataKey) = stage(FailureStage.MASTER_PASSWORD) { repository.authenticateDeviceSetup(password) }
                    try {
                        // Setup may replace a key invalidated by changed biometric enrollment.
                        val encryption = try { cipher(Cipher.ENCRYPT_MODE, true, binding) } catch (e: Failure) {
                            if (e.cause !is android.security.keystore.KeyPermanentlyInvalidatedException) throw e
                            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("taskpro.vault.biometric.v1") }
                            cipher(Cipher.ENCRYPT_MODE, true, binding)
                        }
                        BiometricOperation(encryption, binding, dataKey.copyOf(), true, token)
                    } finally { dataKey.fill(0) }
                } else {
                    val record = read().getJSONObject("bio")
                    val binding = record.getString("binding")
                    check(binding == repository.credentialBinding())
                    BiometricOperation(cipher(Cipher.DECRYPT_MODE, true, binding, decode(record.getString("nonce"))),
                        binding, decode(record.getString("data")), false, token)
                }
            }
        } finally { password?.fill('\u0000') }
    }
    suspend fun finishBiometric(operation: BiometricOperation, authenticated: Cipher) = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                check(authenticated === operation.cipher)
                check(repository.state.value.generation == operation.token && repository.credentialBinding() == operation.binding)
                val result = stage(FailureStage.KEYSTORE) { operation.consume(authenticated) }
                if (operation.setup) {
                    try { write(runCatching { read() }.getOrElse { JSONObject() }.put("bio", JSONObject().put("binding", operation.binding)
                        .put("nonce", encode(authenticated.iv)).put("data", encode(result)))) } finally { result.fill(0) }
                } else repository.unlockDevice(result, operation.binding, operation.token)
            }
        } finally { operation.clear() }
    }
}

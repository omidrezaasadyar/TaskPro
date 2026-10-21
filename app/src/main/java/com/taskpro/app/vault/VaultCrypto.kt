package com.taskpro.app.vault

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Platform AES-GCM and Bouncy Castle Argon2id; no custom cryptographic primitives. */
object VaultCrypto {
    private val random = SecureRandom()
    const val MAX_ENVELOPE = 128 * 1024 * 1024
    private const val MEMORY = 19456
    private const val ITERATIONS = 2

    fun randomBytes(size: Int) = ByteArray(size).also(random::nextBytes)

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        require(password.size <= 1024 && salt.size == 16)
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13).withSalt(salt)
            .withMemoryAsKB(MEMORY).withIterations(ITERATIONS).withParallelism(1).build()
        return ByteArray(32).also { key ->
            try { Argon2BytesGenerator().apply { init(parameters) }.generateBytes(password, key) }
            finally { parameters.clear() }
        }
    }

    private fun crypt(mode: Int, key: ByteArray, nonce: ByteArray, bytes: ByteArray, purpose: String): ByteArray {
        require(key.size == 32 && nonce.size == 12)
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            updateAAD("TaskPro:$purpose:1:argon2id:$MEMORY:$ITERATIONS:1".toByteArray(Charsets.UTF_8))
            doFinal(bytes)
        }
    }

    // Hex avoids Android API-level dependencies and represents binary bytes only.
    private fun ByteArray.hex(): String {
        val digits = "0123456789abcdef"
        return String(CharArray(size * 2) { index ->
            val value = this[index / 2].toInt() and 0xff
            digits[if (index % 2 == 0) value ushr 4 else value and 15]
        })
    }
    private fun String.bytes(): ByteArray {
        require(length % 2 == 0 && length <= MAX_ENVELOPE)
        return ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    fun inspect(text: String): JSONObject {
        require(text.length <= MAX_ENVELOPE)
        val json = com.taskpro.app.backup.BackupFormat.parseObject(text)
        require(json.getInt("version") == 1 && json.getString("kdf") == "argon2id")
        require(json.getInt("memory") == MEMORY && json.getInt("iterations") == ITERATIONS && json.getInt("parallelism") == 1)
        require(json.getString("salt").length == 32 && json.getString("wrapNonce").length == 24)
        require(json.getString("wrappedKey").length == 96 && json.getString("dataNonce").length == 24)
        require(json.getString("data").length in 32..MAX_ENVELOPE)
        if (json.has("recovery")) {
            val recovery = json.getJSONObject("recovery")
            require(recovery.getInt("version") == 1)
            require(recovery.getString("nonce").length == 24 && recovery.getString("key").length == 96)
            if (recovery.has("viewNonce") || recovery.has("viewKey")) {
                require(recovery.getString("viewNonce").length == 24 && recovery.getString("viewKey").length == 96)
            }
        }
        return json
    }

    fun newRecoveryKey(): String {
        val bytes = randomBytes(32)
        return try { bytes.hex().chunked(8).joinToString("-") } finally { bytes.fill(0) }
    }

    private fun recoveryBytes(value: CharArray): ByteArray {
        require(value.size <= 128)
        val normalized = value.filterNot { it == '-' || it.isWhitespace() }.joinToString("")
        require(normalized.length == 64 && normalized.all { it in "0123456789abcdefABCDEF" })
        return normalized.bytes()
    }

    fun enableRecovery(envelope: String, dataKey: ByteArray, recoveryKey: CharArray): String {
        val wrapping = recoveryBytes(recoveryKey)
        return try {
            val nonce = randomBytes(12)
            val viewNonce = randomBytes(12)
            inspect(envelope).put("recovery", JSONObject().put("version", 1).put("nonce", nonce.hex())
                .put("viewNonce", viewNonce.hex())
                .put("viewKey", crypt(Cipher.ENCRYPT_MODE, dataKey, viewNonce, wrapping, "vault:recovery:view").hex())
                .put("key", crypt(Cipher.ENCRYPT_MODE, wrapping, nonce, dataKey, "vault:recovery:key").hex())).toString()
        } finally { wrapping.fill(0); recoveryKey.fill('\u0000') }
    }

    fun viewRecovery(envelope: String, dataKey: ByteArray): String {
        val recovery = inspect(envelope).getJSONObject("recovery")
        val bytes = crypt(Cipher.DECRYPT_MODE, dataKey, recovery.getString("viewNonce").bytes(),
            recovery.getString("viewKey").bytes(), "vault:recovery:view")
        return try { bytes.hex().chunked(8).joinToString("-") } finally { bytes.fill(0) }
    }

    fun recover(envelope: String, recoveryKey: CharArray, newPassword: CharArray): String {
        try {
            val recovery = inspect(envelope).getJSONObject("recovery")
            val wrapping = recoveryBytes(recoveryKey)
            val dataKey = try {
                crypt(Cipher.DECRYPT_MODE, wrapping, recovery.getString("nonce").bytes(),
                    recovery.getString("key").bytes(), "vault:recovery:key")
            } finally { wrapping.fill(0) }
            val plain = try { decrypt(envelope, dataKey, "vault") } finally { dataKey.fill(0) }
            return try {
                VaultDocument.decode(plain)
                // Rotate the data key too, and consume recovery access for this vault revision.
                create(newPassword, plain, "vault")
            } finally { plain.fill(0) }
        } finally { recoveryKey.fill('\u0000'); newPassword.fill('\u0000') }
    }

    fun retainRecovery(previous: String, updated: String): String {
        val old = inspect(previous)
        return if (old.has("recovery")) inspect(updated).put("recovery", old.getJSONObject("recovery")).toString() else updated
    }

    fun create(password: CharArray, plain: ByteArray, purpose: String): String {
        val key = randomBytes(32)
        return try { wrap(password, key, plain, purpose) } finally { key.fill(0) }
    }

    fun wrap(password: CharArray, key: ByteArray, plain: ByteArray, purpose: String): String {
        require(password.size in 12..1024) { "Use at least 12 characters" }
        val salt = randomBytes(16)
        val wrapping = derive(password, salt)
        val nonce = randomBytes(12)
        return try {
            val json = JSONObject().put("version", 1).put("kdf", "argon2id")
                .put("memory", MEMORY).put("iterations", ITERATIONS).put("parallelism", 1)
                .put("salt", salt.hex()).put("wrapNonce", nonce.hex())
                .put("wrappedKey", crypt(Cipher.ENCRYPT_MODE, wrapping, nonce, key, "$purpose:key").hex())
            encrypt(json.toString(), key, plain, purpose)
        } finally { wrapping.fill(0) }
    }

    fun encrypt(envelope: String, key: ByteArray, plain: ByteArray, purpose: String): String {
        val json = JSONObject(envelope)
        val nonce = randomBytes(12)
        return json.put("dataNonce", nonce.hex())
            .put("data", crypt(Cipher.ENCRYPT_MODE, key, nonce, plain, "$purpose:data").hex()).toString()
            .also { require(it.length <= MAX_ENVELOPE) }
    }

    fun unwrap(envelope: String, password: CharArray, purpose: String): ByteArray {
        val json = inspect(envelope)
        val wrapping = derive(password, json.getString("salt").bytes())
        return try {
            crypt(Cipher.DECRYPT_MODE, wrapping, json.getString("wrapNonce").bytes(),
                json.getString("wrappedKey").bytes(), "$purpose:key")
        } finally { wrapping.fill(0) }
    }

    fun decrypt(envelope: String, key: ByteArray, purpose: String): ByteArray {
        val json = inspect(envelope)
        return crypt(Cipher.DECRYPT_MODE, key, json.getString("dataNonce").bytes(), json.getString("data").bytes(), "$purpose:data")
    }

    fun open(envelope: String, password: CharArray, purpose: String): ByteArray {
        val key = unwrap(envelope, password, purpose)
        return try { decrypt(envelope, key, purpose) } finally { key.fill(0) }
    }

    fun generate(length: Int, lower: Boolean, upper: Boolean, digits: Boolean, symbols: Boolean): String {
        require(length in 12..128)
        val groups = listOfNotNull(
            if (lower) "abcdefghijklmnopqrstuvwxyz" else null,
            if (upper) "ABCDEFGHIJKLMNOPQRSTUVWXYZ" else null,
            if (digits) "0123456789" else null,
            if (symbols) "!@#$%^&*()-_=+[]{}:,.?" else null
        )
        require(groups.isNotEmpty())
        val alphabet = groups.joinToString("")
        // Rejection sampling is uniform over strings satisfying the chosen character classes.
        while (true) {
            val candidate = CharArray(length) { alphabet[random.nextInt(alphabet.length)] }
            if (groups.all { group -> candidate.any { it in group } }) return String(candidate).also { candidate.fill('\u0000') }
            candidate.fill('\u0000')
        }
    }
}

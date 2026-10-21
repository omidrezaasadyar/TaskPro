package com.taskpro.app.vault

import androidx.room.Room
import com.taskpro.app.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VaultDeviceUnlockTest {
    @get:Rule val files = TemporaryFolder()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    private lateinit var device: VaultDeviceUnlock
    private lateinit var file: java.io.File
    private var now = 100_000L
    private val master = "synthetic master phrase"
    // Real AES-GCM, substituting only the unavailable device Keystore/authentication in JVM tests.
    private val key = SecretKeySpec(VaultCrypto.randomBytes(32), "AES")
    private val factory: (Int, Boolean, String, ByteArray?) -> Cipher = { mode, bio, binding, nonce ->
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            if (nonce == null) init(mode, key) else init(mode, key, GCMParameterSpec(128, nonce))
        }
    }
    private fun newDevice() = VaultDeviceUnlock(RuntimeEnvironment.getApplication(), repo, file, { now }, { now }, factory)
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repo = VaultRepository(db.vaultDao()) { now }
        repo.unlock(master.toCharArray(), create = true)
        repo.confirmRecoverySaved()
        repo.edit { it.copy(records = listOf(VaultRecord(title = "Synthetic entry"))) }
        file = java.io.File(files.root, "device.json")
        device = newDevice()
    }
    @After fun close() { repo.lock(); db.close() }
    @Test fun pinUnlocksSameVaultButWrongPinCannotAndCooldownSurvivesRestart() = runBlocking {
        device.setPin(master.toCharArray(), "123456".toCharArray())
        assertFalse(file.readText().contains(master))
        assertFalse(file.readText().contains("123456"))
        repo.lock()
        repeat(5) { try { device.unlockPin("000000".toCharArray()); fail() } catch (_: Exception) { } }
        assertEquals(30, device.remainingSeconds())
        device = newDevice()
        assertEquals(30, device.remainingSeconds())
        try { device.unlockPin("123456".toCharArray()); fail() } catch (_: Exception) { }
        assertNull(repo.state.value.document)
        now += 30_001
        device.unlockPin("123456".toCharArray())
        assertEquals("Synthetic entry", repo.state.value.document!!.records.single().title)
        assertEquals(0, device.remainingSeconds())
    }
    @Test fun passwordChangeInvalidatesPinAndSetupRequiresMaster() = runBlocking {
        try { device.setPin("wrong master".toCharArray(), "123456".toCharArray()); fail() } catch (_: Exception) { }
        assertFalse(file.exists())
        device.setPin(master.toCharArray(), "123456".toCharArray())
        repo.changePassword(master.toCharArray(), "new synthetic master".toCharArray())
        assertFalse(device.available().first)
        try { device.unlockPin("123456".toCharArray()); fail() } catch (_: Exception) { }
        assertNull(repo.state.value.document)
    }
    @Test fun failedSetupKeepsExistingPinAndEncryptedVaultIntact() = runBlocking {
        device.setPin(master.toCharArray(), "1234".toCharArray())
        val previous = file.readBytes()
        val vault = db.vaultDao().get()!!.envelope
        val password = "incorrect synthetic master".toCharArray()
        val pin = "5678".toCharArray()
        try { device.setPin(password, pin); fail() }
        catch (e: VaultDeviceUnlock.Failure) { assertEquals(VaultDeviceUnlock.FailureStage.MASTER_PASSWORD, e.stage) }
        assertTrue(password.all { it == '\u0000' } && pin.all { it == '\u0000' })
        assertArrayEquals(previous, file.readBytes())
        val unavailable = VaultDeviceUnlock(RuntimeEnvironment.getApplication(), repo, file, cipherProvider = { _, _, _, _ ->
            throw java.security.InvalidKeyException("Synthetic unavailable device key")
        })
        try { unavailable.setPin(master.toCharArray(), "5678".toCharArray()); fail() }
        catch (e: VaultDeviceUnlock.Failure) { assertEquals(VaultDeviceUnlock.FailureStage.KEYSTORE, e.stage) }
        assertArrayEquals(previous, file.readBytes())
        assertEquals(vault, db.vaultDao().get()!!.envelope)
        repo.lock(); device.unlockPin("1234".toCharArray())
        assertEquals("Synthetic entry", repo.state.value.document!!.records.single().title)
    }
    @Test fun keystoreReadbackFailureDoesNotSaveAnUnusablePin() = runBlocking {
        val broken = VaultDeviceUnlock(RuntimeEnvironment.getApplication(), repo, file, cipherProvider = { mode, bio, binding, nonce ->
            if (mode == Cipher.DECRYPT_MODE) throw java.security.InvalidKeyException("Synthetic readback failure")
            factory(mode, bio, binding, nonce)
        })
        val vault = db.vaultDao().get()!!.envelope
        try { broken.setPin(master.toCharArray(), "12345".toCharArray()); fail() }
        catch (e: VaultDeviceUnlock.Failure) { assertEquals(VaultDeviceUnlock.FailureStage.KEYSTORE, e.stage) }
        assertFalse(file.exists())
        assertEquals(vault, db.vaultDao().get()!!.envelope)
    }
    @Test fun storageFailureIsReportedWithoutChangingTheVault() = runBlocking {
        val directory = java.io.File(files.root, "not-a-file").apply { mkdirs() }
        val broken = VaultDeviceUnlock(RuntimeEnvironment.getApplication(), repo, directory, cipherProvider = factory)
        val vault = db.vaultDao().get()!!.envelope
        try { broken.setPin(master.toCharArray(), "123456".toCharArray()); fail() }
        catch (e: VaultDeviceUnlock.Failure) { assertEquals(VaultDeviceUnlock.FailureStage.STORAGE, e.stage) }
        assertEquals(vault, db.vaultDao().get()!!.envelope)
    }
    @Test fun biometricCipherMustDecryptKeyAndStaleCallbackCannotUnlock() = runBlocking {
        val setup = device.prepareBiometric(master.toCharArray())
        device.finishBiometric(setup, setup.cipher)
        assertTrue(setup.bytes.all { it == 0.toByte() })
        repo.lock()
        val stale = device.prepareBiometric()
        repo.lock()
        try { device.finishBiometric(stale, stale.cipher); fail() } catch (_: Exception) { }
        assertNull(repo.state.value.document)
        val valid = device.prepareBiometric()
        device.finishBiometric(valid, valid.cipher)
        assertEquals("Synthetic entry", repo.state.value.document!!.records.single().title)
    }
    @Test fun biometricDataIsSubmittedOnlyAfterAuthenticationAndOldWrappersStillOpen() = runBlocking {
        var authorized = false
        var aadCalls = 0
        val guarded = VaultDeviceUnlock(RuntimeEnvironment.getApplication(), repo, file, cipherProvider = { mode, bio, binding, nonce ->
            check(bio)
            val delegate = factory(mode, bio, binding, nonce)
            val spi = object : javax.crypto.CipherSpi() {
                override fun engineSetMode(value: String) = Unit
                override fun engineSetPadding(value: String) = Unit
                override fun engineGetBlockSize() = delegate.blockSize
                override fun engineGetOutputSize(size: Int) = delegate.getOutputSize(size)
                override fun engineGetIV() = delegate.iv
                override fun engineGetParameters() = delegate.parameters
                override fun engineInit(mode: Int, key: java.security.Key, random: java.security.SecureRandom?) = Unit
                override fun engineInit(mode: Int, key: java.security.Key, params: java.security.spec.AlgorithmParameterSpec?, random: java.security.SecureRandom?) = Unit
                override fun engineInit(mode: Int, key: java.security.Key, params: java.security.AlgorithmParameters?, random: java.security.SecureRandom?) = Unit
                override fun engineUpdateAAD(input: ByteArray, offset: Int, length: Int) {
                    check(authorized) { "Data submitted before authentication" }
                    aadCalls++
                    delegate.updateAAD(input, offset, length)
                }
                override fun engineUpdate(input: ByteArray, offset: Int, length: Int): ByteArray = delegate.update(input, offset, length)
                override fun engineUpdate(input: ByteArray, offset: Int, length: Int, output: ByteArray, outputOffset: Int) = delegate.update(input, offset, length, output, outputOffset)
                override fun engineDoFinal(input: ByteArray, offset: Int, length: Int): ByteArray {
                    check(authorized)
                    return delegate.doFinal(input, offset, length)
                }
                override fun engineDoFinal(input: ByteArray, offset: Int, length: Int, output: ByteArray, outputOffset: Int) = delegate.doFinal(input, offset, length, output, outputOffset)
            }
            @Suppress("DEPRECATION")
            val provider = object : java.security.Provider("SyntheticAuth", 1.0, "Synthetic authentication gate") {}
            object : Cipher(spi, provider, "AES/GCM/NoPadding") {}.apply { init(mode, key) }
        })
        val setup = guarded.prepareBiometric(master.toCharArray())
        assertEquals(0, aadCalls)
        authorized = true
        guarded.finishBiometric(setup, setup.cipher)
        assertEquals(1, aadCalls)

        // Existing device profiles use the same AAD bytes; only their submission time changed.
        val (binding, dataKey) = repo.authenticateDeviceSetup(master.toCharArray())
        val legacyCipher = factory(Cipher.ENCRYPT_MODE, true, binding, null)
        legacyCipher.updateAAD("TaskPro:device:bio:$binding".toByteArray(Charsets.UTF_8))
        val nonce = legacyCipher.iv
        val encrypted = try { legacyCipher.doFinal(dataKey) } finally { dataKey.fill(0) }
        val json = org.json.JSONObject(file.readText())
        json.getJSONObject("bio").put("nonce", android.util.Base64.encodeToString(nonce, android.util.Base64.NO_WRAP))
            .put("data", android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
        file.writeText(json.toString())
        repo.lock(); authorized = false
        val operation = guarded.prepareBiometric()
        assertEquals(1, aadCalls)
        authorized = true
        guarded.finishBiometric(operation, operation.cipher)
        assertEquals(2, aadCalls)
        assertEquals("Synthetic entry", repo.state.value.document!!.records.single().title)
    }
    @Test fun awayTimeoutIsOneMinuteAndChecksDeadlineBeforeReturning() = runBlocking {
        repo.setVisible(false)
        now += 59_999; repo.checkInactivity()
        assertNotNull(repo.state.value.document)
        repo.touch(); now += 1
        repo.setVisible(true)
        assertNull(repo.state.value.document)
        repo.unlock(master.toCharArray())
        repo.setForeground(false)
        now += 60_000; repo.setForeground(true)
        assertNull(repo.state.value.document)
        repo.unlock(master.toCharArray())
        repo.setVisible(false); now += 10_000; repo.setVisible(true)
        assertNotNull(repo.state.value.document)
    }
}

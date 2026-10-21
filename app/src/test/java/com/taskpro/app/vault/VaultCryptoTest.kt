package com.taskpro.app.vault

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VaultCryptoTest {
    private val password = "test-only long master phrase"
    @Test fun recoveryViewingIsEncryptedAuthenticatedAndLegacyCompatible() {
        val original = VaultCrypto.create(password.toCharArray(), VaultDocument().encode(), "vault")
        val key = VaultCrypto.unwrap(original, password.toCharArray(), "vault")
        try {
            val recovery = VaultCrypto.newRecoveryKey()
            val updated = VaultCrypto.enableRecovery(original, key, recovery.toCharArray())
            assertEquals(recovery, VaultCrypto.viewRecovery(updated, key))
            assertFalse(updated.contains(recovery.replace("-", "")))
            val tampered = JSONObject(updated)
            val view = tampered.getJSONObject("recovery").getString("viewKey")
            tampered.getJSONObject("recovery").put("viewKey", (if (view[0] == '0') "1" else "0") + view.drop(1))
            assertThrows(Exception::class.java) { VaultCrypto.viewRecovery(tampered.toString(), key) }
            val legacy = JSONObject(updated)
            legacy.getJSONObject("recovery").remove("viewKey")
            legacy.getJSONObject("recovery").remove("viewNonce")
            VaultCrypto.inspect(legacy.toString())
            assertThrows(Exception::class.java) { VaultCrypto.viewRecovery(legacy.toString(), key) }
            val restored = VaultCrypto.recover(legacy.toString(), recovery.toCharArray(), password.toCharArray())
            assertEquals(VaultDocument(), VaultDocument.decode(VaultCrypto.open(restored, password.toCharArray(), "vault")))
        } finally { key.fill(0) }
    }
    @Test fun authenticatesAllContentAndRejectsWrongPasswordTamperingAndWrongPurpose() {
        val plain = "synthetic-credential-only".toByteArray()
        val encrypted = VaultCrypto.create(password.toCharArray(), plain, "vault")
        assertFalse(encrypted.contains(String(plain)))
        assertArrayEquals(plain, VaultCrypto.open(encrypted, password.toCharArray(), "vault"))
        assertThrows(Exception::class.java) { VaultCrypto.open(encrypted, "not the password".toCharArray(), "vault") }
        assertThrows(Exception::class.java) { VaultCrypto.open(encrypted, password.toCharArray(), "backup") }
        val json = JSONObject(encrypted)
        val data = json.getString("data")
        json.put("data", (if (data.first() == '0') "1" else "0") + data.drop(1))
        assertThrows(Exception::class.java) { VaultCrypto.open(json.toString(), password.toCharArray(), "vault") }
    }
    @Test fun freshNoncesForEveryWriteAndBoundedKdfParameters() {
        val encrypted = VaultCrypto.create(password.toCharArray(), "test".toByteArray(), "vault")
        val key = VaultCrypto.unwrap(encrypted, password.toCharArray(), "vault")
        try {
            val updated = VaultCrypto.encrypt(encrypted, key, "test".toByteArray(), "vault")
            assertNotEquals(JSONObject(encrypted).getString("dataNonce"), JSONObject(updated).getString("dataNonce"))
            val huge = JSONObject(encrypted).put("memory", Int.MAX_VALUE).toString()
            assertThrows(IllegalArgumentException::class.java) { VaultCrypto.open(huge, password.toCharArray(), "vault") }
        } finally { key.fill(0) }
    }
    @Test fun generatorRespectsLengthAndEveryEnabledCharacterClass() {
        val generated = (1..100).map { VaultCrypto.generate(24, true, true, true, true) }
        assertEquals(100, generated.toSet().size)
        generated.forEach { value ->
            assertEquals(24, value.length)
            assertTrue(value.any(Char::isLowerCase)); assertTrue(value.any(Char::isUpperCase))
            assertTrue(value.any(Char::isDigit)); assertTrue(value.any { !it.isLetterOrDigit() })
        }
        assertTrue(VaultCrypto.generate(12, false, false, true, false).all(Char::isDigit))
        assertThrows(IllegalArgumentException::class.java) { VaultCrypto.generate(12, false, false, false, false) }
    }
    @Test fun documentRoundTripAndSearchNeverMatchSecretFields() {
        val parent = VaultCategory(name = "بانک", color = 123, icon = CategoryIcon.BANK)
        val child = VaultCategory(name = "Work", parentId = parent.id)
        val record = VaultRecord(categoryId = child.id, title = "Synthetic bank", values = mapOf("password" to "secret-only-value", "username" to "tester"),
            custom = listOf(CustomField(label = "Secret field", value = "hidden-custom", sensitive = true)), tags = listOf("work"), favorite = true)
        val doc = VaultDocument(listOf(parent, child), listOf(record))
        assertEquals(doc, VaultDocument.decode(doc.encode()))
        assertFalse(record.matches("secret-only-value", "")); assertFalse(record.matches("hidden-custom", ""))
        assertTrue(record.matches("tester", "")); assertTrue(record.matches("work", ""))
        assertThrows(IllegalArgumentException::class.java) { doc.copy(categories = listOf(parent.copy(parentId = child.id), child)).validate() }
        assertThrows(IllegalArgumentException::class.java) { doc.copy(categories = emptyList()).validate() }
        assertFalse(VaultDocument.validUrl("javascript:alert(1)"))
        assertFalse(VaultDocument.validUrl("https://user:secret@example.com"))
        assertTrue(VaultDocument.validUrl("https://example.com"))
    }
    @Test fun allTypedFieldsRoundTripAndInvalidDataIsRejected() {
        val custom = listOf(
            CustomField(label = "Text", value = "sample"),
            CustomField(label = "Secret", value = "synthetic", type = FieldType.SECRET),
            CustomField(label = "URL", value = "https://example.com", type = FieldType.URL),
            CustomField(label = "Email", value = "test@example.com", type = FieldType.EMAIL),
            CustomField(label = "Number", value = "123.45", type = FieldType.NUMBER),
            CustomField(label = "Date", value = "2026-10-17", type = FieldType.DATE),
            CustomField(label = "Notes", value = "line one\nline two", type = FieldType.NOTES, sensitive = true)
        )
        val record = VaultRecord(title = "All fields", custom = custom, values = mapOf(
            "url" to "https://example.com", "username" to "test", "password" to "synthetic",
            "email" to "test@example.com", "phone" to "+12345", "reference" to "REF", "notes" to "Notes"))
        val document = VaultDocument(records = listOf(record))
        assertEquals(document, VaultDocument.decode(document.encode()))
        for ((type, invalid) in listOf(FieldType.DATE to "2026-02-31", FieldType.NUMBER to "not numeric", FieldType.URL to "file:///secret")) {
            assertThrows(IllegalArgumentException::class.java) {
                document.copy(records = listOf(record.copy(custom = listOf(CustomField(label = "Invalid", type = type, value = invalid))))).validate()
            }
        }
    }
}

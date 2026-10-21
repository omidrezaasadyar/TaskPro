package com.taskpro.app.vault

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class FieldType { TEXT, SECRET, URL, EMAIL, NUMBER, DATE, NOTES }
enum class CategoryIcon { FOLDER, BANK, EMAIL, WEB, BUSINESS, SERVER, KEY, PERSON }
data class VaultCategory(
    val id: String = UUID.randomUUID().toString(), val parentId: String? = null,
    val name: String, val color: Int = 0xff0d9488.toInt(),
    val icon: CategoryIcon = CategoryIcon.FOLDER, val position: Int = 0
)
data class CustomField(
    val id: String = UUID.randomUUID().toString(), val label: String = "",
    val value: String = "", val type: FieldType = FieldType.TEXT, val sensitive: Boolean = false
) {
    val secret get() = sensitive || type == FieldType.SECRET
    override fun toString() = "CustomField($id)"
}
data class VaultRecord(
    val id: String = UUID.randomUUID().toString(), val categoryId: String? = null,
    val title: String = "", val values: Map<String, String> = emptyMap(),
    val custom: List<CustomField> = emptyList(), val tags: List<String> = emptyList(),
    val favorite: Boolean = false, val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = createdAt
) {
    override fun toString() = "VaultRecord($id)"
    fun matches(query: String, categoryName: String): Boolean {
        val publicValues = values.filterKeys { it != "password" && it != "notes" }.values
        return (listOf(title, categoryName) + tags + publicValues + custom.filter { !it.secret }.map { it.value })
            .any { it.contains(query, ignoreCase = true) }
    }
}
data class VaultDocument(
    val categories: List<VaultCategory> = emptyList(), val records: List<VaultRecord> = emptyList(),
    val autoLockSeconds: Int = 120
) {
    fun validate() {
        require(autoLockSeconds in listOf(30, 60, 120, 300, 600))
        require(categories.map { it.id }.toSet().size == categories.size && records.map { it.id }.toSet().size == records.size)
        val byId = categories.associateBy { it.id }
        categories.forEach { category ->
            require(category.id.isNotBlank() && category.name.isNotBlank() && category.position >= 0)
            val visited = mutableSetOf(category.id)
            var parent = category.parentId
            while (parent != null) {
                require(visited.add(parent) && parent in byId) { "Invalid category hierarchy" }
                parent = byId.getValue(parent).parentId
            }
        }
        records.forEach {
            require(it.id.isNotBlank() && it.title.isNotBlank() && (it.categoryId == null || it.categoryId in byId))
            require(it.createdAt >= 0 && it.modifiedAt >= it.createdAt)
            require(it.values.keys.all { key -> key in STANDARD_FIELDS })
            require(it.custom.map { f -> f.id }.toSet().size == it.custom.size)
            it.custom.forEach { f ->
                require(f.id.isNotBlank() && f.label.isNotBlank())
                if (f.value.isNotBlank()) when (f.type) {
                    FieldType.NUMBER -> require(f.value.toBigDecimalOrNull() != null)
                    FieldType.DATE -> require(validDate(f.value))
                    FieldType.URL -> require(validUrl(f.value))
                    FieldType.EMAIL -> require(f.value.contains('@'))
                    else -> Unit
                }
            }
            it.values["url"]?.takeIf(String::isNotBlank)?.let { url -> require(validUrl(url)) }
        }
    }

    fun encode(): ByteArray {
        validate()
        return JSONObject().put("version", 1).put("autoLockSeconds", autoLockSeconds)
            .put("categories", JSONArray().apply { categories.forEach { c -> put(JSONObject()
                .put("id", c.id).put("parentId", c.parentId ?: JSONObject.NULL).put("name", c.name)
                .put("color", c.color).put("icon", c.icon.name).put("position", c.position)) } })
            .put("records", JSONArray().apply { records.forEach { r -> put(JSONObject()
                .put("id", r.id).put("categoryId", r.categoryId ?: JSONObject.NULL).put("title", r.title)
                .put("values", JSONObject(r.values)).put("tags", JSONArray(r.tags)).put("favorite", r.favorite)
                .put("createdAt", r.createdAt).put("modifiedAt", r.modifiedAt)
                .put("custom", JSONArray().apply { r.custom.forEach { f -> put(JSONObject()
                    .put("id", f.id).put("label", f.label).put("value", f.value)
                    .put("type", f.type.name).put("sensitive", f.sensitive)) } })) } })
            .toString().toByteArray(Charsets.UTF_8).also { require(it.size <= MAX_DOCUMENT_BYTES) }
    }

    companion object {
        const val MAX_DOCUMENT_BYTES = 4 * 1024 * 1024
        val STANDARD_FIELDS = listOf("url", "username", "email", "password", "phone", "reference", "notes")
        fun validUrl(value: String): Boolean = runCatching {
            val uri = java.net.URI(value)
            uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false)
        private fun validDate(value: String): Boolean = runCatching {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).apply { isLenient = false }
            value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && format.parse(value) != null
        }.getOrDefault(false)
        fun decode(bytes: ByteArray): VaultDocument {
            require(bytes.size <= MAX_DOCUMENT_BYTES)
            val json = com.taskpro.app.backup.BackupFormat.parseObject(String(bytes, Charsets.UTF_8))
            require(json.getInt("version") == 1)
            val cats = json.getJSONArray("categories")
            val records = json.getJSONArray("records")
            return VaultDocument(
                (0 until cats.length()).map { i -> cats.getJSONObject(i).let { c -> VaultCategory(
                    c.getString("id"), if (c.isNull("parentId")) null else c.getString("parentId"),
                    c.getString("name"), c.getInt("color"), CategoryIcon.valueOf(c.getString("icon")), c.getInt("position")) } },
                (0 until records.length()).map { i -> records.getJSONObject(i).let { r ->
                    val fields = r.getJSONObject("values")
                    val custom = r.getJSONArray("custom")
                    val tags = r.getJSONArray("tags")
                    VaultRecord(r.getString("id"), if (r.isNull("categoryId")) null else r.getString("categoryId"),
                        r.getString("title"), fields.keys().asSequence().associateWith { fields.getString(it) },
                        (0 until custom.length()).map { n -> custom.getJSONObject(n).let { f -> CustomField(
                            f.getString("id"), f.getString("label"), f.getString("value"),
                            FieldType.valueOf(f.getString("type")), f.getBoolean("sensitive")) } },
                        (0 until tags.length()).map { tags.getString(it) }, r.getBoolean("favorite"),
                        r.getLong("createdAt"), r.getLong("modifiedAt"))
                } }, json.getInt("autoLockSeconds")
            ).also { it.validate() }
        }
    }
}

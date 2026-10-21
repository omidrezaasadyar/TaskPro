package com.taskpro.app.vault

import java.util.UUID

internal object VaultDashboardFixtures {
    fun id(name: String): String = UUID.nameUUIDFromBytes("synthetic-vault-$name".toByteArray()).toString()
    fun document(persian: Boolean = false): VaultDocument {
        val names = if (persian) listOf("بانک‌ها", "ایمیل", "حساب‌ها", "امور کاری", "حساب‌های پس‌انداز", "خدمات")
            else listOf("Banks", "Email", "Accounts", "Business", "Savings", "Services")
        val keys = listOf("banks", "email", "accounts", "business", "savings", "services")
        val icons = listOf(CategoryIcon.BANK, CategoryIcon.EMAIL, CategoryIcon.PERSON, CategoryIcon.BUSINESS, CategoryIcon.FOLDER, CategoryIcon.WEB)
        val colors = listOf(0xffb45a54, 0xff3a86ad, 0xff258365, 0xffb88c33, 0xffeeeeee, 0xff111111)
        val groups = names.mapIndexed { i, name -> VaultCategory(id(keys[i]), when (i) { 4 -> id("banks"); 5 -> id("savings"); else -> null },
            name, colors[i].toInt(), icons[i], i) }
        val titles = if (persian) listOf("ایمیل شخصی", "ورود به بانک", "حساب پس‌انداز", "یادداشت شخصی", "سامانهٔ کاری")
            else listOf("Personal email", "Bank login", "Savings login", "Unfiled account", "Business portal")
        val locations = listOf("email", "banks", "savings", null, "business")
        val records = titles.mapIndexed { i, title -> VaultRecord(id("record$i"), locations[i]?.let(::id), title,
            values = mapOf("username" to "synthetic-user-$i", "password" to "synthetic-secret-$i", "notes" to "private-note-$i"),
            custom = listOf(CustomField(id("custom$i"), "Secret", "private-custom-$i", FieldType.SECRET)),
            tags = listOf(if (i == 4) "work" else "personal"), favorite = i == 0,
            createdAt = 1_700_000_000_000L + i * 1000, modifiedAt = 1_700_000_100_000L - i * 1000) }
        return VaultDocument(groups, records, 300)
    }
}

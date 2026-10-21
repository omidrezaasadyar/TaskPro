package com.taskpro.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/** User-selectable theme. SYSTEM follows the device setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The whole-app background that item/task screens sit on top of:
 *  - PLAIN   solid theme surface
 *  - GRADIENT soft brand gradient (default)
 *  - AURORA  more colourful multi-stop gradient
 */
enum class BackgroundStyle { PLAIN, GRADIENT, AURORA }

data class SettingsSnapshot(
    val theme: ThemeMode,
    val background: BackgroundStyle,
    val lastBackupAt: Long?
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists app preferences: light/dark theme and the background style. */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val backgroundKey = stringPreferencesKey("background_style")
    private val lastBackupKey = longPreferencesKey("last_backup_at")

    val lastBackupAt: Flow<Long?> = context.dataStore.data.map { it[lastBackupKey] }

    suspend fun snapshot(): SettingsSnapshot {
        val prefs = context.dataStore.data.first()
        return SettingsSnapshot(
            prefs[themeKey]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
            prefs[backgroundKey]?.let(BackgroundStyle::valueOf) ?: BackgroundStyle.GRADIENT,
            prefs[lastBackupKey]
        )
    }

    suspend fun replace(snapshot: SettingsSnapshot) {
        context.dataStore.edit {
            it[themeKey] = snapshot.theme.name
            it[backgroundKey] = snapshot.background.name
            if (snapshot.lastBackupAt == null) it.remove(lastBackupKey)
            else it[lastBackupKey] = snapshot.lastBackupAt
        }
    }

    suspend fun recordBackup(time: Long) {
        context.dataStore.edit { it[lastBackupKey] = time }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    val backgroundStyle: Flow<BackgroundStyle> = context.dataStore.data.map { prefs ->
        prefs[backgroundKey]?.let { runCatching { BackgroundStyle.valueOf(it) }.getOrNull() }
            ?: BackgroundStyle.GRADIENT
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setBackgroundStyle(style: BackgroundStyle) {
        context.dataStore.edit { it[backgroundKey] = style.name }
    }
}

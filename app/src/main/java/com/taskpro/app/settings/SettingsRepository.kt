package com.taskpro.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** User-selectable theme. SYSTEM follows the device setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The whole-app background that item/task screens sit on top of:
 *  - PLAIN   solid theme surface
 *  - GRADIENT soft brand gradient (default)
 *  - AURORA  more colourful multi-stop gradient
 */
enum class BackgroundStyle { PLAIN, GRADIENT, AURORA }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists app preferences: light/dark theme and the background style. */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val backgroundKey = stringPreferencesKey("background_style")

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

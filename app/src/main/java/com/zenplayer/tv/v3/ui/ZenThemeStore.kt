package com.zenplayer.tv.v3.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

typealias ZenThemePreferences = ZenThemeState

private val Context.zenThemeDataStore by preferencesDataStore(name = "zen_theme")

class ZenThemeStore(private val context: Context) {
    private object Keys {
        val preset = stringPreferencesKey("preset")
        val receiver = stringPreferencesKey("receiver")
        val background = stringPreferencesKey("background")
        val backgroundBlur = intPreferencesKey("background_blur")
        val backgroundIntensity = floatPreferencesKey("background_intensity")
        val animationSpeed = stringPreferencesKey("animation_speed")
        val reducedMotion = booleanPreferencesKey("reduced_motion")
        val glassOpacity = floatPreferencesKey("glass_opacity")
        val glowStrength = floatPreferencesKey("glow_strength")
    }

    val state: Flow<ZenThemeState> = context.zenThemeDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map { preferences ->
            val preset = preferences[Keys.preset]
                ?.let { value -> runCatching { ZenThemePreset.valueOf(value) }.getOrNull() }
                ?: ZenThemePreset.Aurora
            val receiver = preferences[Keys.receiver]
                ?.let { value -> runCatching { ZenReceiverTheme.valueOf(value) }.getOrNull() }
            val background = preferences[Keys.background]
                ?.let { value -> runCatching { ZenBackgroundPreset.valueOf(value) }.getOrNull() }
                ?: ZenBackgroundPreset.AuroraFlow
            val animationSpeed = preferences[Keys.animationSpeed]
                ?.let { value -> runCatching { ZenAnimationSpeed.valueOf(value) }.getOrNull() }
                ?: ZenAnimationSpeed.Slow

            ZenThemeState(
                preset = preset,
                receiver = receiver,
                background = background,
                backgroundBlur = preferences[Keys.backgroundBlur] ?: 70,
                backgroundIntensity = (preferences[Keys.backgroundIntensity] ?: .55f).coerceIn(.15f, 1f),
                animationSpeed = animationSpeed,
                reducedMotion = preferences[Keys.reducedMotion] ?: false,
                glassOpacity = (preferences[Keys.glassOpacity] ?: preset.glassOpacity).coerceIn(.25f, .90f),
                glowStrength = (preferences[Keys.glowStrength] ?: preset.glowStrength).coerceIn(0f, 1f)
            )
        }

    suspend fun save(state: ZenThemeState) {
        context.zenThemeDataStore.edit { preferences ->
            preferences[Keys.preset] = state.preset.name
            state.receiver?.let { preferences[Keys.receiver] = it.name } ?: preferences.remove(Keys.receiver)
            preferences[Keys.background] = state.background.name
            preferences[Keys.backgroundBlur] = state.backgroundBlur.coerceIn(0, 100)
            preferences[Keys.backgroundIntensity] = state.backgroundIntensity.coerceIn(.15f, 1f)
            preferences[Keys.animationSpeed] = state.animationSpeed.name
            preferences[Keys.reducedMotion] = state.reducedMotion
            preferences[Keys.glassOpacity] = state.glassOpacity.coerceIn(.25f, .90f)
            preferences[Keys.glowStrength] = state.glowStrength.coerceIn(0f, 1f)
        }
    }
}

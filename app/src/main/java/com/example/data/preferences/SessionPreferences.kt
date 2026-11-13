package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "farmaciadb_prefs")

class SessionPreferences(private val context: Context) {

  private object Keys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LOGGED_USER_UID = stringPreferencesKey("logged_user_uid")
  }

  private val safeData: Flow<Preferences> = context.dataStore.data.catch { exception ->
    if (exception is IOException) {
      emit(emptyPreferences())
    } else {
      throw exception
    }
  }

  val darkModeFlow: Flow<Boolean> = safeData.map { prefs ->
    prefs[Keys.DARK_MODE] ?: false
  }

  val loggedUserIdFlow: Flow<String?> = safeData.map { prefs ->
    prefs[Keys.LOGGED_USER_UID]?.takeIf { it.isNotBlank() }
  }

  suspend fun setDarkMode(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DARK_MODE] = enabled
    }
  }

  suspend fun setLoggedUserId(userId: String?) {
    context.dataStore.edit { prefs ->
      if (userId.isNullOrBlank()) {
        prefs.remove(Keys.LOGGED_USER_UID)
      } else {
        prefs[Keys.LOGGED_USER_UID] = userId
      }
    }
  }
}

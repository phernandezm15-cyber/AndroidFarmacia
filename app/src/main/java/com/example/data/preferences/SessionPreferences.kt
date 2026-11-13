package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "farmaciadb_prefs")

class SessionPreferences(private val context: Context) {

  private object Keys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LOGGED_USER_ID = intPreferencesKey("logged_user_id")
  }

  val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[Keys.DARK_MODE] ?: false
  }

  val loggedUserIdFlow: Flow<Int?> = context.dataStore.data.map { prefs ->
    prefs[Keys.LOGGED_USER_ID]
  }

  suspend fun setDarkMode(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DARK_MODE] = enabled
    }
  }

  suspend fun setLoggedUserId(userId: Int?) {
    context.dataStore.edit { prefs ->
      if (userId == null) {
        prefs.remove(Keys.LOGGED_USER_ID)
      } else {
        prefs[Keys.LOGGED_USER_ID] = userId
      }
    }
  }
}

package com.example.data.repository

import android.content.Context
import com.example.data.local.FarmaciaDatabase
import com.example.data.remote.SupabaseConfig

object FarmaciaRepositoryProvider {
  @Volatile
  private var INSTANCE: FarmaciaRepository? = null

  fun get(context: Context): FarmaciaRepository {
    return INSTANCE ?: synchronized(this) {
      INSTANCE ?: create(context.applicationContext ?: context).also { INSTANCE = it }
    }
  }

  private fun create(appContext: Context): FarmaciaRepository {
    return if (SupabaseConfig.isConfigured) {
      SupabaseFarmaciaRepository(SupabaseConfig.createApi())
    } else {
      RoomFarmaciaRepository(FarmaciaDatabase.getInstance(appContext).dao())
    }
  }
}

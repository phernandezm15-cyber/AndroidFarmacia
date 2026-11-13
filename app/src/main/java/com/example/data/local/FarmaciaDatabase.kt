package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    UserEntity::class,
    MedicamentoEntity::class,
    ProveedorEntity::class,
    ClienteEntity::class,
    CompraEntity::class,
    VentaEntity::class,
    VentaItemEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class FarmaciaDatabase : RoomDatabase() {
  abstract fun dao(): FarmaciaDao

  companion object {
    @Volatile
    private var INSTANCE: FarmaciaDatabase? = null

    fun getInstance(context: Context): FarmaciaDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          FarmaciaDatabase::class.java,
          "farmaciadb.sqlite",
        ).build().also { INSTANCE = it }
      }
    }
  }
}

package com.pangea.oriontek.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pangea.oriontek.data.local.dao.ClientDao
import com.pangea.oriontek.data.local.entity.AddressEntity
import com.pangea.oriontek.data.local.entity.ClientEntity


@Database(
    entities = [
        ClientEntity::class,
        AddressEntity::class ],
    version = 1,
    exportSchema = false
)
abstract class ClientDatabase : RoomDatabase(){

    abstract fun clientDao(): ClientDao
}
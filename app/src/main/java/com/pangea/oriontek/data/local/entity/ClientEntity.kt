package com.pangea.oriontek.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val lastName: String,
    val company: String,
    val email: String,
    val phone: String,
    val photoResId: Int = 0,
)
package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val slug: String,
    val active: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "prizes")
data class PrizeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val name: String,
    val description: String,
    val displayText: String,
    val resultText: String,
    val discount: String, // e.g. "20%"
    val weight: Int,
    val quantity: Int,
    val unlimitedQuantity: Boolean = false,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "access_codes")
data class AccessCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val code: String,
    val status: String = "AVAILABLE", // AVAILABLE, USED, INACTIVE
    val usedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "spins")
data class SpinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val accessCodeId: Long,
    val prizeId: Long,
    val clientName: String? = null,
    val observation: String? = null,
    val prizeNameSnapshot: String,
    val discountSnapshot: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long = 0, // 0 for global or specific campaign id
    val key: String,
    val value: String
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val nome: String,
    val whatsapp: String,
    val cleanPhone: String,
    val vencimento: String,
    val codigo: String,
    val sent: Boolean = false,
    val sentAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

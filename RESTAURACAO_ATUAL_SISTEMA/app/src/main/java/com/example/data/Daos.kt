package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE slug = :slug LIMIT 1")
    suspend fun getCampaignBySlug(slug: String): CampaignEntity?

    @Query("SELECT * FROM campaigns WHERE id = :id LIMIT 1")
    suspend fun getCampaignById(id: Long): CampaignEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity): Long

    @Update
    suspend fun updateCampaign(campaign: CampaignEntity)

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun deleteCampaign(id: Long)
}

@Dao
interface PrizeDao {
    @Query("SELECT * FROM prizes WHERE campaignId = :campaignId")
    fun getPrizesForCampaign(campaignId: Long): Flow<List<PrizeEntity>>

    @Query("SELECT * FROM prizes WHERE campaignId = :campaignId AND active = 1")
    suspend fun getActivePrizesForCampaign(campaignId: Long): List<PrizeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrize(prize: PrizeEntity): Long

    @Update
    suspend fun updatePrize(prize: PrizeEntity)

    @Query("DELETE FROM prizes WHERE id = :id")
    suspend fun deletePrize(id: Long)
}

@Dao
interface AccessCodeDao {
    @Query("SELECT * FROM access_codes WHERE campaignId = :campaignId ORDER BY createdAt DESC")
    fun getCodesForCampaign(campaignId: Long): Flow<List<AccessCodeEntity>>

    @Query("SELECT * FROM access_codes WHERE campaignId = :campaignId AND code = :code LIMIT 1")
    suspend fun getCodeByString(campaignId: Long, code: String): AccessCodeEntity?

    @Query("SELECT * FROM access_codes WHERE code = :code LIMIT 1")
    suspend fun getCodeGlobal(code: String): AccessCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCode(code: AccessCodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodes(codes: List<AccessCodeEntity>)

    @Update
    suspend fun updateCode(code: AccessCodeEntity)

    @Query("DELETE FROM access_codes WHERE id = :id")
    suspend fun deleteCode(id: Long)

    @Query("DELETE FROM access_codes WHERE id IN (:ids)")
    suspend fun deleteCodes(ids: List<Long>)

    @Query("DELETE FROM access_codes WHERE campaignId = :campaignId AND status = 'USED'")
    suspend fun deleteUsedCodes(campaignId: Long)
}

@Dao
interface SpinDao {
    @Query("SELECT * FROM spins WHERE campaignId = :campaignId ORDER BY createdAt DESC")
    fun getSpinsForCampaign(campaignId: Long): Flow<List<SpinEntity>>

    @Query("SELECT * FROM spins ORDER BY createdAt DESC")
    fun getAllSpins(): Flow<List<SpinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpin(spin: SpinEntity): Long

    @Update
    suspend fun updateSpin(spin: SpinEntity)

    @Query("DELETE FROM spins WHERE id = :id")
    suspend fun deleteSpin(id: Long)

    @Query("DELETE FROM spins WHERE id IN (:ids)")
    suspend fun deleteSpins(ids: List<Long>)

    @Query("DELETE FROM spins")
    suspend fun clearAllSpins()
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE campaignId = :campaignId OR campaignId = 0")
    fun getSettings(campaignId: Long): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings WHERE campaignId = :campaignId AND key = :key LIMIT 1")
    suspend fun getSetting(campaignId: Long, key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: SettingEntity)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE campaignId = :campaignId ORDER BY id DESC")
    fun getClientsForCampaign(campaignId: Long): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientEntity>)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id IN (:ids)")
    suspend fun deleteClients(ids: List<Long>)

    @Query("DELETE FROM clients WHERE campaignId = :campaignId")
    suspend fun clearClientsForCampaign(campaignId: Long)
}

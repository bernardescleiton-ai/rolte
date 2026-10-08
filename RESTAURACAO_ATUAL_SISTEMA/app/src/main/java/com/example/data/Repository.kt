package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {
    val campaignDao = db.campaignDao()
    val prizeDao = db.prizeDao()
    val accessCodeDao = db.accessCodeDao()
    val spinDao = db.spinDao()
    val settingDao = db.settingDao()
    val clientDao = db.clientDao()

    val allCampaigns: Flow<List<CampaignEntity>> = campaignDao.getAllCampaigns()
    val allSpins: Flow<List<SpinEntity>> = spinDao.getAllSpins()

    suspend fun getCampaignBySlug(slug: String): CampaignEntity? = campaignDao.getCampaignBySlug(slug)
    suspend fun getCampaignById(id: Long): CampaignEntity? = campaignDao.getCampaignById(id)
    suspend fun insertCampaign(campaign: CampaignEntity): Long = campaignDao.insertCampaign(campaign)
    suspend fun updateCampaign(campaign: CampaignEntity) = campaignDao.updateCampaign(campaign)
    suspend fun deleteCampaign(id: Long) = campaignDao.deleteCampaign(id)

    fun getPrizesForCampaign(campaignId: Long): Flow<List<PrizeEntity>> = prizeDao.getPrizesForCampaign(campaignId)
    suspend fun insertPrize(prize: PrizeEntity): Long = prizeDao.insertPrize(prize)
    suspend fun updatePrize(prize: PrizeEntity) = prizeDao.updatePrize(prize)
    suspend fun deletePrize(id: Long) = prizeDao.deletePrize(id)

    fun getCodesForCampaign(campaignId: Long): Flow<List<AccessCodeEntity>> = accessCodeDao.getCodesForCampaign(campaignId)
    suspend fun getCodeByString(campaignId: Long, code: String): AccessCodeEntity? = accessCodeDao.getCodeByString(campaignId, code)
    suspend fun insertCode(code: AccessCodeEntity) = accessCodeDao.insertCode(code)
    suspend fun insertCodes(codes: List<AccessCodeEntity>) = accessCodeDao.insertCodes(codes)
    suspend fun updateCode(code: AccessCodeEntity) = accessCodeDao.updateCode(code)
    suspend fun deleteCode(id: Long) = accessCodeDao.deleteCode(id)
    suspend fun deleteCodes(ids: List<Long>) = accessCodeDao.deleteCodes(ids)
    suspend fun deleteUsedCodes(campaignId: Long) = accessCodeDao.deleteUsedCodes(campaignId)

    fun getSpinsForCampaign(campaignId: Long): Flow<List<SpinEntity>> = spinDao.getSpinsForCampaign(campaignId)
    suspend fun insertSpin(spin: SpinEntity): Long = spinDao.insertSpin(spin)
    suspend fun updateSpin(spin: SpinEntity) = spinDao.updateSpin(spin)
    suspend fun deleteSpin(id: Long) = spinDao.deleteSpin(id)
    suspend fun deleteSpins(ids: List<Long>) = spinDao.deleteSpins(ids)
    suspend fun clearAllSpins() = spinDao.clearAllSpins()

    fun getSettings(campaignId: Long): Flow<List<SettingEntity>> = settingDao.getSettings(campaignId)
    suspend fun getSetting(campaignId: Long, key: String): SettingEntity? = settingDao.getSetting(campaignId, key)
    suspend fun saveSetting(setting: SettingEntity) = settingDao.insertOrUpdateSetting(setting)

    fun getClientsForCampaign(campaignId: Long): Flow<List<ClientEntity>> = clientDao.getClientsForCampaign(campaignId)
    suspend fun insertClient(client: ClientEntity): Long = clientDao.insertClient(client)
    suspend fun insertClients(clients: List<ClientEntity>) = clientDao.insertClients(clients)
    suspend fun updateClient(client: ClientEntity) = clientDao.updateClient(client)
    suspend fun deleteClient(client: ClientEntity) = clientDao.deleteClient(client)
    suspend fun deleteClients(ids: List<Long>) = clientDao.deleteClients(ids)
    suspend fun clearClientsForCampaign(campaignId: Long) = clientDao.clearClientsForCampaign(campaignId)
}

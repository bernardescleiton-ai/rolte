package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CampaignEntity::class,
        PrizeEntity::class,
        AccessCodeEntity::class,
        SpinEntity::class,
        SettingEntity::class,
        ClientEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun campaignDao(): CampaignDao
    abstract fun prizeDao(): PrizeDao
    abstract fun accessCodeDao(): AccessCodeDao
    abstract fun spinDao(): SpinDao
    abstract fun settingDao(): SettingDao
    abstract fun clientDao(): ClientDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roleta_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateData(database)
                    }
                }
            }

            private suspend fun prepopulateData(db: AppDatabase) {
                val campaignDao = db.campaignDao()
                val prizeDao = db.prizeDao()
                val codeDao = db.accessCodeDao()
                val settingDao = db.settingDao()

                // Create default campaign
                val campaignId = campaignDao.insertCampaign(
                    CampaignEntity(
                        name = "Outubro Premiado",
                        slug = "outubro",
                        active = true
                    )
                )

                // Create default prizes
                val p1 = prizeDao.insertPrize(
                    PrizeEntity(
                        campaignId = campaignId,
                        name = "5% de Desconto",
                        description = "Desconto em compras acima de R$ 50",
                        displayText = "5%",
                        resultText = "5% DE DESCONTO",
                        discount = "5%",
                        weight = 40,
                        quantity = 500,
                        unlimitedQuantity = false,
                        active = true
                    )
                )
                val p2 = prizeDao.insertPrize(
                    PrizeEntity(
                        campaignId = campaignId,
                        name = "10% de Desconto",
                        description = "Desconto em qualquer produto",
                        displayText = "10%",
                        resultText = "10% DE DESCONTO",
                        discount = "10%",
                        weight = 30,
                        quantity = 200,
                        unlimitedQuantity = false,
                        active = true
                    )
                )
                val p3 = prizeDao.insertPrize(
                    PrizeEntity(
                        campaignId = campaignId,
                        name = "15% de Desconto",
                        description = "Desconto especial de primavera",
                        displayText = "15%",
                        resultText = "15% DE DESCONTO",
                        discount = "15%",
                        weight = 20,
                        quantity = 50,
                        unlimitedQuantity = false,
                        active = true
                    )
                )
                val p4 = prizeDao.insertPrize(
                    PrizeEntity(
                        campaignId = campaignId,
                        name = "20% de Desconto",
                        description = "Super desconto exclusivo",
                        displayText = "20%",
                        resultText = "20% DE DESCONTO",
                        discount = "20%",
                        weight = 8,
                        quantity = 20,
                        unlimitedQuantity = false,
                        active = true
                    )
                )
                val p5 = prizeDao.insertPrize(
                    PrizeEntity(
                        campaignId = campaignId,
                        name = "50% de Desconto",
                        description = "Prêmio máximo da roleta!",
                        displayText = "50%",
                        resultText = "50% DE DESCONTO",
                        discount = "50%",
                        weight = 2,
                        quantity = 5,
                        unlimitedQuantity = false,
                        active = true
                    )
                )

                // Create sample access codes
                val sampleCodes = listOf(
                    "RLT-7X92KP",
                    "RLT-4M8Q2A",
                    "RLT-9ZK31B",
                    "RLT-2P7X8M",
                    "RLT-TEST01"
                )
                sampleCodes.forEach { code ->
                    codeDao.insertCode(
                        AccessCodeEntity(
                            campaignId = campaignId,
                            code = code,
                            status = "AVAILABLE"
                        )
                    )
                }

                // Default settings
                val defaultSettings = mapOf(
                    "title" to "ROLETA DA SORTE",
                    "subtitle" to "Gire a roleta e ganhe descontos e benefícios incríveis!",
                    "code_placeholder" to "Digite seu código (ex: RLT-7X92KP)",
                    "liberate_button" to "LIBERAR ROLETA",
                    "spin_button" to "CLIQUE AQUI E GIRE A ROLETA",
                    "result_title" to "🎉 PARABÉNS! 🎉",
                    "bg_color" to "#0B0F19",
                    "text_color" to "#FFFFFF",
                    "btn_color" to "#FACC15"
                )
                defaultSettings.forEach { (key, value) ->
                    settingDao.insertOrUpdateSetting(
                        SettingEntity(
                            campaignId = campaignId,
                            key = key,
                            value = value
                        )
                    )
                }
            }
        }
    }
}

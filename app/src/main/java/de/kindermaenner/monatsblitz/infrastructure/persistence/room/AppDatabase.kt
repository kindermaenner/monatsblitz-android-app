package de.kindermaenner.monatsblitz.infrastructure.persistence.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.ModeDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.ModeEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef

@Database(
    entities = [
        PlayerEntity::class,
        TournamentEntity::class,
        GameEntity::class,
        TournamentPlayerCrossRef::class,
        ModeEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao

    abstract fun tournamentDao(): TournamentDao

    abstract fun gameDao(): GameDao

    abstract fun tournamentPlayerDao(): TournamentPlayerDao

    abstract fun modeDao(): ModeDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `players_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `remoteId` INTEGER,
                        `name` TEXT NOT NULL COLLATE NOCASE,
                        `vorname` TEXT NOT NULL COLLATE NOCASE,
                        `dirty` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `players_new` (`id`, `remoteId`, `name`, `vorname`, `dirty`)
                    SELECT `id`, `remoteId`, `name`, `vorname`, `dirty` FROM `players`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `players`")
                db.execSQL("ALTER TABLE `players_new` RENAME TO `players`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_players_vorname_name` ON `players` (`vorname`, `name`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `modes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `remoteId` INTEGER,
                        `name` TEXT NOT NULL COLLATE NOCASE,
                        `dirty` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_modes_name` ON `modes` (`name`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "monatsblitz.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { INSTANCE = it }
            }
    }
}

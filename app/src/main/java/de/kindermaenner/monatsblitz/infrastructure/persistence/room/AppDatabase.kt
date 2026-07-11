package de.kindermaenner.monatsblitz.infrastructure.persistence.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef

@Database(
    entities = [
        PlayerEntity::class,
        TournamentEntity::class,
        GameEntity::class,
        TournamentPlayerCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao

    abstract fun tournamentDao(): TournamentDao

    abstract fun tournamentPlayerDao(): TournamentPlayerDao

    abstract fun gameDao(): GameDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "monatsblitz.db"
                ).build().also { INSTANCE = it }
            }
    }
}
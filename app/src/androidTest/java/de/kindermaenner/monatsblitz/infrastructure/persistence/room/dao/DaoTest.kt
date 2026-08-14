package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import org.junit.After
import org.junit.Before

abstract class DaoTest {
    protected lateinit var db: AppDatabase
    protected lateinit var playerDao: PlayerDao
    protected lateinit var tournamentDao: TournamentDao
    protected lateinit var gameDao: GameDao
    protected lateinit var tournamentPlayerDao: TournamentPlayerDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        playerDao = db.playerDao()
        tournamentDao = db.tournamentDao()
        gameDao = db.gameDao()
        tournamentPlayerDao = db.tournamentPlayerDao()
    }

    @After
    fun closeDb() {
        db.close()
    }
}

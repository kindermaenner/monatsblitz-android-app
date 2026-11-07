package de.kindermaenner.monatsblitz.infrastructure.persistence.room

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(config)
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
        helper.close()
    }

    private fun getColumnNames(table: String): List<String> {
        val columns = mutableListOf<String>()
        val cursor = db.query("PRAGMA table_info($table)")
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(1)) // name column
        }
        cursor.close()
        return columns
    }

    @Test
    fun migration_4_to_5_recreatesPlayersTableWithNocaseCollation() {
        db.execSQL(
            """
            CREATE TABLE `players` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `remoteId` INTEGER,
                `name` TEXT NOT NULL,
                `vorname` TEXT NOT NULL,
                `dirty` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO `players` (`id`, `remoteId`, `name`, `vorname`, `dirty`) VALUES (1, 10, 'Mustermann', 'Max', 0)")

        AppDatabase.MIGRATION_4_5.migrate(db)

        val columns = getColumnNames("players")
        assertTrue(columns.contains("name"))
        assertTrue(columns.contains("vorname"))

        // Check data preserved
        val cursor = db.query("SELECT id, name, vorname FROM players WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("Mustermann", cursor.getString(1))
        assertEquals("Max", cursor.getString(2))
        cursor.close()
    }

    @Test
    fun migration_5_to_6_createsModesTable() {
        AppDatabase.MIGRATION_5_6.migrate(db)

        val columns = getColumnNames("modes")
        assertTrue(columns.contains("id"))
        assertTrue(columns.contains("remoteId"))
        assertTrue(columns.contains("name"))
        assertTrue(columns.contains("dirty"))
    }

    @Test
    fun migration_6_to_7_addsHasLeftToTournamentPlayerCrossRef() {
        db.execSQL(
            """
            CREATE TABLE `tournament_player_cross_ref` (
                `tournamentId` INTEGER NOT NULL,
                `playerId` INTEGER NOT NULL,
                `rank` INTEGER,
                `points` REAL,
                PRIMARY KEY(`tournamentId`, `playerId`)
            )
            """.trimIndent()
        )

        AppDatabase.MIGRATION_6_7.migrate(db)

        val columns = getColumnNames("tournament_player_cross_ref")
        assertTrue(columns.contains("hasLeft"))
    }

    @Test
    fun migration_7_to_8_addsIsLocalAndIsLocalOnly() {
        db.execSQL("CREATE TABLE `players` (`id` INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE `tournaments` (`id` INTEGER PRIMARY KEY NOT NULL)")

        AppDatabase.MIGRATION_7_8.migrate(db)

        assertTrue(getColumnNames("players").contains("isLocal"))
        assertTrue(getColumnNames("tournaments").contains("isLocalOnly"))
    }

    @Test
    fun migration_8_to_9_addsLastParticipatedToPlayers() {
        db.execSQL("CREATE TABLE `players` (`id` INTEGER PRIMARY KEY NOT NULL)")

        AppDatabase.MIGRATION_8_9.migrate(db)

        assertTrue(getColumnNames("players").contains("lastParticipated"))
    }

    @Test
    fun migration_9_to_10_addsRufnameAndIndexToPlayers() {
        db.execSQL(
            """
            CREATE TABLE `players` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `vorname` TEXT NOT NULL,
                `name` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_players_vorname_name` ON `players` (`vorname`, `name`)")

        AppDatabase.MIGRATION_9_10.migrate(db)

        assertTrue(getColumnNames("players").contains("rufname"))
    }

    @Test
    fun migration_10_to_11_addsCountsForRankingAndLabelToTournaments() {
        db.execSQL("CREATE TABLE `tournaments` (`id` INTEGER PRIMARY KEY NOT NULL)")

        AppDatabase.MIGRATION_10_11.migrate(db)

        val columns = getColumnNames("tournaments")
        assertTrue(columns.contains("countsForRanking"))
        assertTrue(columns.contains("label"))
    }

    @Test
    fun database_getInstanceReturnsSingleton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = AppDatabase.getInstance(context)
        val db2 = AppDatabase.getInstance(context)
        assertNotNull(db1)
        assertEquals(db1, db2)
        db1.close()
    }
}

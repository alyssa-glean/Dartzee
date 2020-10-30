package dartzee.db

import dartzee.core.helper.getFutureTime
import dartzee.core.helper.getPastTime
import dartzee.core.util.DateStatics
import dartzee.core.util.getSqlDateNow
import dartzee.helper.*
import dartzee.logging.CODE_MERGE_ERROR
import dartzee.logging.CODE_TEST_CONNECTION_ERROR
import dartzee.logging.Severity
import dartzee.utils.DartsDatabaseUtil
import dartzee.utils.Database
import dartzee.utils.InjectedThings.mainDatabase
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.sql.Timestamp

class TestDatabaseMerger: AbstractTest()
{
    @Test
    fun `Should return false if connecting to remote database fails`()
    {
        val remoteName = "jdbc:derby:memory:invalid;create=false"
        val remoteDatabase = Database("invalid", remoteName)

        val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase)
        merger.validateMerge() shouldBe false
        verifyLog(CODE_TEST_CONNECTION_ERROR, Severity.ERROR)
        dialogFactory.errorsShown.shouldContainExactly("An error occurred connecting to the remote database.")
    }

    @Test
    fun `Should return false and log an error if remote database version cannot be verified`()
    {
        val remoteDatabase = mockk<Database>(relaxed = true)
        every { remoteDatabase.testConnection() } returns true
        every { remoteDatabase.getDatabaseVersion() } returns null
        
        val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase)
        merger.validateMerge() shouldBe false
        verifyLog(CODE_MERGE_ERROR, Severity.ERROR)
        dialogFactory.errorsShown.shouldContainExactly("An error occurred connecting to the remote database.")
    }

    @Test
    fun `Should return false if remote database has higher version`()
    {
        usingInMemoryDatabase { remoteDatabase ->
            remoteDatabase.updateDatabaseVersion(DartsDatabaseUtil.DATABASE_VERSION + 1)

            val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase)
            merger.validateMerge() shouldBe false
            dialogFactory.errorsShown.shouldContainExactly("The remote database contains data written by a higher Dartzee version. \n\nYou will need to update to the latest version of Dartzee before syncing again.")
        }
    }

    @Test
    fun `Should return false if unable to migrate remote database`()
    {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val dbVersion = DartsDatabaseUtil.DATABASE_VERSION - 1
            remoteDatabase.updateDatabaseVersion(dbVersion)

            val migrator = DatabaseMigrator(emptyMap())
            val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase, databaseMigrator = migrator)
            val result = merger.validateMerge()
            result shouldBe false

            val dbDetails =
                "Remote version: $dbVersion, min supported: ${DartsDatabaseUtil.DATABASE_VERSION}, current: ${DartsDatabaseUtil.DATABASE_VERSION}"
            dialogFactory.errorsShown.shouldContainExactly(
                "Remote database is too out-of-date to be upgraded by this version of Dartzee. " +
                        "Please downgrade to an earlier version so that the data can be converted.\n\n$dbDetails"
            )
        }
    }

    @Test
    fun `Should migrate remote database to latest version and return true on success`()
    {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val dbVersion = DartsDatabaseUtil.DATABASE_VERSION - 1
            remoteDatabase.updateDatabaseVersion(dbVersion)

            val migrations = mapOf(dbVersion to listOf
            { database: Database -> database.executeUpdate("CREATE TABLE Test(RowId VARCHAR(36))") }
            )

            val migrator = DatabaseMigrator(migrations)
            val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase, databaseMigrator = migrator)
            val result = merger.validateMerge()
            result shouldBe true

            remoteDatabase.getDatabaseVersion() shouldBe DartsDatabaseUtil.DATABASE_VERSION
            remoteDatabase.executeQueryAggregate("SELECT COUNT(1) FROM Test") shouldBe 0
        }
    }

    @Test
    fun `Should insert into SyncAudit when performing the merge`()
    {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase, remoteName = "Goomba")
            val result = merger.performMerge()

            SyncAuditEntity.getLastSyncDate(result, "Goomba").shouldNotBeNull()
        }
    }

    @Test
    fun `Should not sync the PendingLogs table`()
    {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            PendingLogsEntity.factory("foo").saveToDatabase()

            val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase, remoteName = "Goomba")
            val resultingDatabase = merger.performMerge()

            getCountFromTable("PendingLogs", resultingDatabase) shouldBe 0
            getCountFromTable("PendingLogs", mainDatabase) shouldBe 1
        }
    }

    @Test
    fun `Should only sync rows that were modified since the last sync`()
    {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val dtLastSync = setUpLastSync(mainDatabase)

            val oldGame = insertGame(dtLastUpdate = getPastTime(dtLastSync), database = mainDatabase)
            val newGame = insertGame(dtLastUpdate = getFutureTime(dtLastSync), database = mainDatabase)

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDatabase = merger.performMerge()

            getCountFromTable("Game", resultingDatabase) shouldBe 1
            GameEntity(resultingDatabase).retrieveForId(oldGame.rowId, false).shouldBeNull()
            GameEntity(resultingDatabase).retrieveForId(newGame.rowId, false).shouldNotBeNull()
        }
    }

    @Test
    fun `Should sync all rows if no last sync date`()
    {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val oldGame = insertGame(dtLastUpdate = Timestamp(500), database = mainDatabase)
            val newGame = insertGame(dtLastUpdate = getSqlDateNow(), database = mainDatabase)

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDatabase = merger.performMerge()

            getCountFromTable("Game", resultingDatabase) shouldBe 2
            GameEntity(resultingDatabase).retrieveForId(oldGame.rowId, false).shouldNotBeNull()
            GameEntity(resultingDatabase).retrieveForId(newGame.rowId, false).shouldNotBeNull()
        }
    }

    @Test
    fun `Should reassign localIds in order for new games`()
    {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            insertGame(localId = 7, database = remoteDatabase)

            val gameOne = insertGame(localId = 1, dtCreation = Timestamp(500), database = mainDatabase)
            val gameTwo = insertGame(localId = 2, dtCreation = Timestamp(1000), database = mainDatabase)

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDatabase = merger.performMerge()

            val gameDao = GameEntity(resultingDatabase)
            gameDao.retrieveForId(gameOne.rowId)!!.localId shouldBe 8
            gameDao.retrieveForId(gameTwo.rowId)!!.localId shouldBe 9
        }
    }

    @Test
    fun `Should not reassign localId for a modified game`()
    {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val dtLastSync = setUpLastSync(mainDatabase)

            val oldGame = insertGame(
                localId = 4,
                dtFinish = DateStatics.END_OF_TIME,
                dtLastUpdate = getPastTime(dtLastSync),
                database = remoteDatabase
            )
            insertGame(localId = 7, database = remoteDatabase)

            insertGame(
                database = mainDatabase,
                uuid = oldGame.rowId,
                localId = 4,
                dtFinish = getSqlDateNow(),
                dtLastUpdate = getFutureTime(dtLastSync)
            )

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDb = merger.performMerge()

            val dao = GameEntity(resultingDb)
            val resultingGame = dao.retrieveForId(oldGame.rowId)!!
            resultingGame.localId shouldBe 4
            resultingGame.dtFinish shouldNotBe DateStatics.END_OF_TIME
        }
    }

    private fun setUpLastSync(database: Database): Timestamp
    {
        SyncAuditEntity.insertSyncAudit(database, "Goomba")
        return SyncAuditEntity.getLastSyncDate(database, "Goomba")!!
    }

    private fun makeDatabaseMerger(localDatabase: Database = mainDatabase,
                                   remoteDatabase: Database,
                                   databaseMigrator: DatabaseMigrator = DatabaseMigrator(emptyMap()),
                                   remoteName: String = "Goomba") =
        DatabaseMerger(localDatabase, remoteDatabase, databaseMigrator, remoteName)
}
package dartzee.db

import dartzee.achievements.AchievementType
import dartzee.core.helper.getFutureTime
import dartzee.core.helper.getPastTime
import dartzee.core.util.DateStatics
import dartzee.core.util.getSqlDateNow
import dartzee.game.GameType
import dartzee.helper.AbstractTest
import dartzee.helper.getCountFromTable
import dartzee.helper.insertAchievement
import dartzee.helper.insertDart
import dartzee.helper.insertGame
import dartzee.helper.insertParticipant
import dartzee.helper.insertPlayer
import dartzee.helper.usingInMemoryDatabase
import dartzee.utils.Database
import dartzee.utils.InjectedThings.mainDatabase
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.sql.Timestamp
import org.junit.jupiter.api.Test

class TestDatabaseMerger : AbstractTest() {
    @Test
    fun `Should insert into SyncAudit when performing the merge`() {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase, remoteName = "Goomba")
            val result = merger.performMerge()

            val data = SyncAuditEntity.getLastSyncData(result)!!
            data.remoteName shouldBe "Goomba"
        }
    }

    @Test
    fun `Should not sync the PendingLogs table`() {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            PendingLogsEntity.factory("foo").saveToDatabase()

            val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase, remoteName = "Goomba")
            val resultingDatabase = merger.performMerge()

            getCountFromTable("PendingLogs", resultingDatabase) shouldBe 0
            getCountFromTable("PendingLogs", mainDatabase) shouldBe 1
        }
    }

    @Test
    fun `Should not sync the Achievement table`() {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            insertAchievement(database = mainDatabase)

            val merger = makeDatabaseMerger(remoteDatabase = remoteDatabase, remoteName = "Goomba")
            val resultingDatabase = merger.performMerge()

            getCountFromTable("Achievement", resultingDatabase) shouldBe 0
            getCountFromTable("Achievement", mainDatabase) shouldBe 1
        }
    }

    @Test
    fun `Should only sync rows that were modified since the last sync`() {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val dtLastSync = setUpLastSync(mainDatabase)

            val oldGame =
                insertGame(dtLastUpdate = getPastTime(dtLastSync), database = mainDatabase)
            val newGame =
                insertGame(dtLastUpdate = getFutureTime(dtLastSync), database = mainDatabase)

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDatabase = merger.performMerge()

            getCountFromTable("Game", resultingDatabase) shouldBe 1
            GameEntity(resultingDatabase).retrieveForId(oldGame.rowId, false).shouldBeNull()
            GameEntity(resultingDatabase).retrieveForId(newGame.rowId, false).shouldNotBeNull()
        }
    }

    @Test
    fun `Should sync all rows if no last sync date`() {
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
    fun `Should sync deletion audits and remove corresponding rows from the remote`() {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val game = insertGame(database = remoteDatabase)

            DeletionAuditEntity.factoryAndSave(game)

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDatabase = merger.performMerge()

            getCountFromTable("Game", resultingDatabase) shouldBe 0
            getCountFromTable("DeletionAudit", resultingDatabase) shouldBe 1
        }
    }

    @Test
    fun `Should handle syncing a newly deleted entity that never made it to the remote`() {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val game = insertGame(database = mainDatabase)

            DeletionAuditEntity.factoryAndSave(game)

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDatabase = merger.performMerge()

            getCountFromTable("Game", resultingDatabase) shouldBe 0
            getCountFromTable("DeletionAudit", resultingDatabase) shouldBe 1
        }
    }

    @Test
    fun `Should reassign localIds in order for new games`() {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            insertGame(localId = 7, database = remoteDatabase)

            val gameOne =
                insertGame(localId = 1, dtCreation = Timestamp(500), database = mainDatabase)
            val gameTwo =
                insertGame(localId = 2, dtCreation = Timestamp(1000), database = mainDatabase)

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDatabase = merger.performMerge()

            val gameDao = GameEntity(resultingDatabase)
            gameDao.retrieveForId(gameOne.rowId)!!.localId shouldBe 8
            gameDao.retrieveForId(gameTwo.rowId)!!.localId shouldBe 9
        }
    }

    @Test
    fun `Should not reassign localId for a modified game`() {
        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val dtLastSync = setUpLastSync(mainDatabase)

            val oldGame =
                insertGame(
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

    @Test
    fun `Should regenerate achievement rows for players whose achievements have changed`() {
        val (playerId, gameId) = setUpThreeDarterData()

        insertAchievement(
            playerId = playerId,
            type = AchievementType.X01_BEST_THREE_DART_SCORE,
            achievementCounter = 60
        )

        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDb = merger.performMerge()

            getCountFromTable("Achievement", resultingDb) shouldBe 1

            val remoteRow =
                AchievementEntity(resultingDb).retrieveEntity("PlayerId = '$playerId'")!!
            remoteRow.achievementCounter shouldBe 140
            remoteRow.achievementType shouldBe AchievementType.X01_BEST_THREE_DART_SCORE
            remoteRow.gameIdEarned shouldBe gameId
        }
    }

    @Test
    fun `Should not replace achievement rows for players whose achievements have not changed`() {
        val (playerId, _) = setUpThreeDarterData()

        insertAchievement(
            playerId = playerId,
            type = AchievementType.X01_BEST_THREE_DART_SCORE,
            achievementCounter = 60
        )

        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val remotePlayer = insertPlayer(database = remoteDatabase)
            insertAchievement(
                playerId = remotePlayer.rowId,
                type = AchievementType.X01_BEST_THREE_DART_SCORE,
                database = remoteDatabase,
                achievementCounter = 25
            )

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDb = merger.performMerge()

            getCountFromTable("Achievement", mainDatabase) shouldBe 1
            getCountFromTable("Achievement", resultingDb) shouldBe 2

            val remoteRow =
                AchievementEntity(resultingDb)
                    .retrieveEntity("PlayerId = '${remotePlayer.rowId}'")!!
            remoteRow.achievementCounter shouldBe 25
            remoteRow.achievementType shouldBe AchievementType.X01_BEST_THREE_DART_SCORE
        }
    }

    @Test
    fun `Should only run achievement conversion for changed refs`() {
        val (playerId, _) = setUpThreeDarterData()

        insertAchievement(
            playerId = playerId,
            type = AchievementType.X01_BEST_FINISH,
            achievementCounter = 60
        )

        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDb = merger.performMerge()

            getCountFromTable("Achievement", resultingDb) shouldBe 0
        }
    }

    @Test
    fun `Should not run achievement conversion at all if no achievements changed`() {
        val (playerId, _) = setUpThreeDarterData()

        usingInMemoryDatabase(withSchema = true) { remoteDatabase ->
            val remotePlayer = insertPlayer(uuid = playerId, database = remoteDatabase)
            insertAchievement(
                playerId = remotePlayer.rowId,
                type = AchievementType.GOLF_BEST_GAME,
                database = remoteDatabase,
                achievementCounter = 18
            )

            val merger = makeDatabaseMerger(mainDatabase, remoteDatabase)
            val resultingDb = merger.performMerge()

            getCountFromTable("Achievement", mainDatabase) shouldBe 0
            getCountFromTable("Achievement", resultingDb) shouldBe 1

            val remoteRow =
                AchievementEntity(resultingDb)
                    .retrieveEntity("PlayerId = '${remotePlayer.rowId}'")!!
            remoteRow.achievementCounter shouldBe 18
            remoteRow.achievementType shouldBe AchievementType.GOLF_BEST_GAME
        }
    }

    private fun setUpThreeDarterData(): Pair<String, String> {
        val p = insertPlayer()
        val g = insertGame(gameType = GameType.X01)
        val pt = insertParticipant(playerId = p.rowId, gameId = g.rowId)

        insertDart(
            pt,
            startingScore = 501,
            roundNumber = 1,
            ordinal = 1,
            score = 20,
            multiplier = 3
        )
        insertDart(
            pt,
            startingScore = 441,
            roundNumber = 1,
            ordinal = 2,
            score = 20,
            multiplier = 3
        )
        insertDart(
            pt,
            startingScore = 381,
            roundNumber = 1,
            ordinal = 3,
            score = 20,
            multiplier = 1
        )

        return Pair(p.rowId, g.rowId)
    }

    private fun setUpLastSync(database: Database): Timestamp {
        SyncAuditEntity.insertSyncAudit(database, "Goomba")
        return SyncAuditEntity.getLastSyncData(database)!!.lastSynced
    }

    private fun makeDatabaseMerger(
        localDatabase: Database = mainDatabase,
        remoteDatabase: Database,
        remoteName: String = "Goomba"
    ) = DatabaseMerger(localDatabase, remoteDatabase, remoteName)
}

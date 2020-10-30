package dartzee.db

import dartzee.core.util.DateStatics
import dartzee.core.util.getSqlDateNow
import dartzee.game.GameType
import dartzee.helper.*
import dartzee.logging.CODE_SQL_EXCEPTION
import dartzee.logging.exceptions.WrappedSqlException
import dartzee.utils.InjectedThings.mainDatabase
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.string.shouldNotBeEmpty
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import org.junit.Test
import java.sql.Timestamp

class TestGameEntity: AbstractEntityTest<GameEntity>()
{
    override fun factoryDao() = GameEntity()

    @Test
    fun `LocalId field should be unique`()
    {
        wipeTable("Game")

        insertGame(localId = 5)
        verifyNoLogs(CODE_SQL_EXCEPTION)

        val ex = shouldThrow<WrappedSqlException> {
            insertGame(localId = 5)
        }

        val sqle = ex.sqlException
        sqle.message shouldContain "duplicate key"

        getCountFromTable("Game") shouldBe 1
    }

    @Test
    fun `LocalIds should be assigned along with RowId`()
    {
        wipeTable("Game")

        val entity = GameEntity()
        entity.assignRowId()

        entity.rowId.shouldNotBeEmpty()
        entity.localId shouldBe 1
    }

    @Test
    fun `isFinished should work correctly`()
    {
        val entity = GameEntity()
        entity.isFinished() shouldBe false

        entity.dtFinish = getSqlDateNow()
        entity.isFinished() shouldBe true
    }

    @Test
    fun `Should get the participant count based on its own row ID`()
    {
        wipeTable("Participant")
        wipeTable("Game")

        val game = GameEntity()
        val gameId = game.assignRowId()
        game.saveToDatabase()
        game.getParticipantCount() shouldBe 0

        insertParticipant()
        game.getParticipantCount() shouldBe 0

        insertParticipant(gameId = gameId)
        game.getParticipantCount() shouldBe 1
    }

    @Test
    fun `Should handle no participants when getting players vector`()
    {
        wipeTable("Participant")
        wipeTable("Game")

        val game = GameEntity()
        game.saveToDatabase()

        game.retrievePlayersVector().shouldBeEmpty()
    }

    @Test
    fun `Should return the player vector correctly`()
    {
        wipeTable("Player")
        wipeTable("Participant")
        wipeTable("Game")

        //Insert a random player
        val game = GameEntity()
        val gameId = game.assignRowId()
        game.saveToDatabase()

        insertPlayerForGame("Clive", gameId)
        insertPlayerForGame("Bob", randomGuid())

        val players = game.retrievePlayersVector()
        players.shouldHaveSize(1)

        players.first().name shouldBe "Clive"
    }


    @Test
    fun `Should get the description along with the params`()
    {
        val game = GameEntity()
        game.gameParams = "foo"
        game.gameType = GameType.GOLF
        game.getTypeDesc() shouldBe "Golf - foo holes"
    }

    @Test
    fun `Factory and save individual game`()
    {
        val game = GameEntity.factoryAndSave(GameType.X01, "301")

        val gameId = game.rowId
        game.localId shouldNotBe -1
        game.gameType shouldBe GameType.X01
        game.gameParams shouldBe "301"
        game.dtFinish shouldBe DateStatics.END_OF_TIME
        game.dartsMatchId shouldBe ""
        game.matchOrdinal shouldBe -1

        game.retrieveForId(gameId) shouldNotBe null
    }

    @Test
    fun `Factory and save for a match`()
    {
        val match = DartsMatchEntity.factoryFirstTo(4)
        match.gameType = GameType.GOLF
        match.gameParams = "18"

        val matchId = match.rowId

        val gameOne = GameEntity.factoryAndSave(match)
        gameOne.matchOrdinal shouldBe 1
        gameOne.dartsMatchId shouldBe matchId
        gameOne.gameType shouldBe GameType.GOLF
        gameOne.gameParams shouldBe "18"
        gameOne.rowId shouldNotBe ""

        val gameTwo = GameEntity.factoryAndSave(match)
        gameTwo.matchOrdinal shouldBe 2
        gameTwo.dartsMatchId shouldBe matchId
        gameTwo.gameType shouldBe GameType.GOLF
        gameTwo.gameParams shouldBe "18"
        gameTwo.rowId shouldNotBe ""
    }

    @Test
    fun `Should retrieve games in the right order for a match`()
    {
        val matchId = randomGuid()

        val millis = System.currentTimeMillis()
        val gameTwoId = insertGame(dartsMatchId = matchId, matchOrdinal = 1, dtCreation = Timestamp(millis)).rowId
        val gameOneId = insertGame(dartsMatchId = matchId, matchOrdinal = 1, dtCreation = Timestamp(millis - 5)).rowId
        insertGame(dartsMatchId = randomGuid())
        val gameThreeId = insertGame(dartsMatchId = matchId, matchOrdinal = 2, dtCreation = Timestamp(millis - 10)).rowId

        val gameIds = GameEntity.retrieveGamesForMatch(matchId).map{it.rowId}.toList()
        gameIds.shouldContainExactly(gameOneId, gameTwoId, gameThreeId)
    }

    @Test
    fun `Should map localId to gameId`()
    {
        val gameOne = insertGame(localId = 1)
        val gameTwo = insertGame(localId = 2)

        GameEntity.getGameId(1) shouldBe gameOne.rowId
        GameEntity.getGameId(2) shouldBe gameTwo.rowId
        GameEntity.getGameId(3) shouldBe null
    }

    @Test
    fun `Should reassign localId when merging into another database`()
    {
        usingInMemoryDatabase(withSchema = true) { otherDatabase ->
            insertGame(database = otherDatabase)
            insertGame(database = otherDatabase)

            val game = insertGame(database = mainDatabase)
            game.localId shouldBe 1

            game.mergeIntoDatabase(otherDatabase)
            game.localId shouldBe 3

            val retrieved = GameEntity(otherDatabase).retrieveForId(game.rowId)!!
            retrieved.localId shouldBe 3
        }
    }
}
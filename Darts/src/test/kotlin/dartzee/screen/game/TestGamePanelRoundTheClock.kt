package dartzee.screen.game

import dartzee.core.obj.HashMapList
import dartzee.`object`.Dart
import dartzee.achievements.ACHIEVEMENT_REF_CLOCK_BEST_STREAK
import dartzee.db.*
import dartzee.screen.game.GamePanelRoundTheClock
import dartzee.screen.game.scorer.DartsScorerRoundTheClock
import dartzee.db.TestAchievementEntity
import dartzee.helper.AbstractTest
import dartzee.helper.randomGuid
import io.kotlintest.shouldBe
import org.junit.Test

class TestGamePanelRoundTheClock: AbstractTest()
{
    @Test
    fun `Should load a current streak across several rounds`()
    {
        val panel = TestRoundTheClockGamePanel()

        val roundOne = mutableListOf(Dart(20, 1), factoryClockHit(1), factoryClockHit(2))
        val roundTwo = mutableListOf(factoryClockHit(3), factoryClockHit(4), factoryClockHit(5), factoryClockHit(6))

        val hm = HashMapList<Int, Dart>()
        hm[1] = roundOne
        hm[2] = roundTwo

        panel.loadDartsForParticipant(0, hm, 2)

        panel.hmPlayerNumberToCurrentStreak[0] shouldBe 6
    }

    @Test
    fun `Should load a current streak of 0 if the last dart was a miss`()
    {
        val panel = TestRoundTheClockGamePanel()

        val roundOne = mutableListOf(factoryClockHit(1), factoryClockHit(2), Dart(5, 1))
        val hm = HashMapList<Int, Dart>()
        hm[1] = roundOne

        panel.loadDartsForParticipant(0, hm, 1)

        panel.hmPlayerNumberToCurrentStreak[0] shouldBe 0
    }

    @Test
    fun `Should not update the achievement for a completed hit streak of 1`()
    {
        val playerId = randomGuid()
        val panel = TestRoundTheClockGamePanel(playerId)

        val dartsThrown = listOf(factoryClockHit(1), Dart(15, 1), Dart(12, 2))
        panel.setDartsThrown(dartsThrown)
        panel.updateBestStreakAchievement()

        AchievementEntity().retrieveEntity("PlayerId = '$playerId'") shouldBe null
    }

    @Test
    fun `Should not update the achievement for a partial hit streak of 1`()
    {
        val playerId = randomGuid()
        val panel = TestRoundTheClockGamePanel(playerId)

        val dartsThrown = listOf(Dart(15, 1), Dart(12, 2), factoryClockHit(1))
        panel.setDartsThrown(dartsThrown)
        panel.updateBestStreakAchievement()

        AchievementEntity().retrieveEntity("PlayerId = '$playerId'") shouldBe null
        panel.hmPlayerNumberToCurrentStreak[0] shouldBe 1
    }

    @Test
    fun `Should save the best streak even when the player has subsequently missed`()
    {
        val playerId = randomGuid()
        val panel = TestRoundTheClockGamePanel(playerId)

        val dartsThrown = listOf(factoryClockHit(1), factoryClockHit(2), Dart(12, 2))
        panel.setDartsThrown(dartsThrown)
        panel.updateBestStreakAchievement()

        val achievement = AchievementEntity().retrieveEntity("PlayerId = '$playerId'")!!
        achievement.achievementCounter shouldBe 2
        achievement.achievementRef shouldBe ACHIEVEMENT_REF_CLOCK_BEST_STREAK
    }

    @Test
    fun `Should add on to the current streak if one exists`()
    {
        val playerId = randomGuid()
        val panel = TestRoundTheClockGamePanel(playerId)
        panel.hmPlayerNumberToCurrentStreak[0] = 5

        val dartsThrown = listOf(factoryClockHit(1), factoryClockHit(2), factoryClockHit(3))
        panel.setDartsThrown(dartsThrown)
        panel.updateBestStreakAchievement()

        val achievement = AchievementEntity.retrieveAchievement(ACHIEVEMENT_REF_CLOCK_BEST_STREAK, playerId)!!
        achievement.achievementCounter shouldBe 8
        achievement.gameIdEarned shouldBe panel.getGameId()

        panel.hmPlayerNumberToCurrentStreak[0] shouldBe 8
    }

    private fun factoryClockHit(clockTarget: Int): Dart
    {
        val dart = Dart(clockTarget, 1)
        dart.startingScore = clockTarget
        return dart
    }

    class TestRoundTheClockGamePanel(currentPlayerId: String = randomGuid())
        : GamePanelRoundTheClock(TestAchievementEntity.FakeDartsScreen(), GameEntity.factoryAndSave(GAME_TYPE_ROUND_THE_CLOCK, CLOCK_TYPE_STANDARD))
    {
        init
        {
            for (i in 0..3)
            {
                val scorer = DartsScorerRoundTheClock(this)
                scorer.init(PlayerEntity(), CLOCK_TYPE_STANDARD)
                hmPlayerNumberToDartsScorer[i] = scorer
            }

            activeScorer = hmPlayerNumberToDartsScorer[0]!!
            currentPlayerNumber = 0
            val pt = ParticipantEntity()
            pt.playerId = currentPlayerId
            hmPlayerNumberToParticipant[0] = pt
            currentRoundNumber = 1
        }

        fun setDartsThrown(dartsThrown: List<Dart>)
        {
            this.dartsThrown.addAll(dartsThrown)
        }
    }
}
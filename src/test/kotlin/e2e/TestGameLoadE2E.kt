package e2e

import dartzee.`object`.Dart
import dartzee.`object`.GameLauncher
import dartzee.awaitCondition
import dartzee.game.GameType
import dartzee.helper.AbstractRegistryTest
import dartzee.helper.retrieveGame
import dartzee.screen.ScreenCache
import dartzee.screen.game.AbstractDartsGameScreen
import dartzee.shouldBeVisible
import dartzee.utils.PREFERENCES_BOOLEAN_AI_AUTO_CONTINUE
import dartzee.utils.PREFERENCES_INT_AI_SPEED
import dartzee.utils.PreferenceUtil
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import org.junit.Test
import javax.swing.SwingUtilities

class TestGameLoadE2E: AbstractRegistryTest()
{
    override fun getPreferencesAffected() = listOf(PREFERENCES_INT_AI_SPEED, PREFERENCES_BOOLEAN_AI_AUTO_CONTINUE)

    override fun beforeEachTest()
    {
        super.beforeEachTest()
        PreferenceUtil.saveInt(PREFERENCES_INT_AI_SPEED, 0)
        PreferenceUtil.saveBoolean(PREFERENCES_BOOLEAN_AI_AUTO_CONTINUE, false)
    }

    @Test
    fun `E2E - Game load and AI resume`()
    {
        val (winner, loser) = createPlayers()

        GameLauncher().launchNewGame(listOf(winner, loser), GameType.X01, "501")

        awaitCondition { retrieveGame().isFinished() }

        val gameId = retrieveGame().rowId
        val originalGameScreen = ScreenCache.getDartsGameScreen(gameId)!!
        val loserProgress = originalGameScreen.getScorer("Loser").getLatestScoreRemaining()

        closeOpenGames()

        SwingUtilities.invokeAndWait { GameLauncher().loadAndDisplayGame(gameId) }

        val gameScreen = ScreenCache.getDartsGameScreen(gameId)!!
        gameScreen.shouldBeVisible()

        verifyGameLoadedCorrectly(gameScreen)

        gameScreen.toggleStats()

        val loserScorer = gameScreen.getScorer("Loser")
        loserScorer.getLatestScoreRemaining() shouldBe loserProgress
        loserScorer.shouldBePaused()
        loserScorer.resume()

        awaitCondition(10000) { loserScorer.playerIsFinished() }
    }

    private fun verifyGameLoadedCorrectly(gameScreen: AbstractDartsGameScreen)
    {
        val winnerScorer = gameScreen.getScorer("Winner")
        winnerScorer.lblResult.text shouldBe "9 Darts"
        winnerScorer.getDartsForRow(0).shouldContainExactly(Dart(20, 3), Dart(20, 3), Dart(20, 3))
        winnerScorer.getDartsForRow(1).shouldContainExactly(Dart(20, 3), Dart(20, 3), Dart(20, 3))
        winnerScorer.getDartsForRow(2).shouldContainExactly(Dart(20, 3), Dart(19, 3), Dart(12, 2))
    }
}
package e2e

import dartzee.`object`.Dart
import dartzee.ai.AimDart
import dartzee.db.PlayerEntity
import dartzee.game.GameType
import dartzee.helper.AbstractRegistryTest
import dartzee.helper.insertGame
import dartzee.helper.predictableX01Model
import dartzee.listener.DartboardListener
import dartzee.screen.game.DartsGameScreen
import dartzee.utils.PREFERENCES_INT_AI_SPEED
import dartzee.utils.PreferenceUtil
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.*

class TestX01E2E: AbstractRegistryTest()
{
    override fun getPreferencesAffected() = listOf(PREFERENCES_INT_AI_SPEED)

    override fun beforeEachTest()
    {
        super.beforeEachTest()
        PreferenceUtil.saveInt(PREFERENCES_INT_AI_SPEED, 100)
    }

    @Test
    fun `E2E - 501 - bust and mercy rule`()
    {
        val game = insertGame(gameType = GameType.X01, gameParams = "301")

        val parentWindow = DartsGameScreen(game, 1)
        parentWindow.isVisible = true

        val listener = mockk<DartboardListener>(relaxed = true)
        parentWindow.gamePanel.dartboard.addDartboardListener(listener)

        // T20, T20, T20  -- 121
        // T20, D20,   1  --  20
        // D15            --  20 (bust)
        // 10, 5, 0       --   5
        //  0, 0, 0       --   5
        //  1             --   4 (mercy)
        // D2             --   0
        val aiModel = predictableX01Model(parentWindow.gamePanel.dartboard, mercyThreshold = 7) { startingScore, dartsThrown ->
            when (startingScore)
            {
                61 -> AimDart(20, 2)
                21 -> AimDart(1, 1)
                20 -> if (dartsThrown == 6) AimDart(15, 2) else AimDart(10, 1)
                10 -> AimDart(5, 1)
                5  -> if (dartsThrown <= 12) AimDart(5, 0) else AimDart(1, 1)
                4  -> AimDart(2, 2)
                else -> AimDart(20, 3)
            }
        }

        val player = mockk<PlayerEntity>(relaxed = true)
        every { player.getModel() } returns aiModel
        every { player.rowId } returns UUID.randomUUID().toString()
        every { player.isAi() } returns true
        every { player.isHuman() } returns false

        parentWindow.gamePanel.startNewGame(listOf(player))
        awaitGameFinish(game)

        val expectedRounds = listOf(
            listOf(Dart(20, 3), Dart(20, 3), Dart(20, 3)),
            listOf(Dart(20, 3), Dart(20, 2), Dart(1, 1)),
            listOf(Dart(15, 2)),
            listOf(Dart(10, 1), Dart(5, 1), Dart(5, 0)),
            listOf(Dart(5, 0), Dart(5, 0), Dart(5, 0)),
            listOf(Dart(1, 1)),
            listOf(Dart(2, 2))
        )

        verifyState(parentWindow.gamePanel, listener, expectedRounds, scoreSuffix = " Darts", finalScore = 19)
    }
}
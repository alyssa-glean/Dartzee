package dartzee.screen.game.x01

import dartzee.game.state.X01PlayerState
import dartzee.helper.DEFAULT_X01_CONFIG
import dartzee.helper.makeDart
import dartzee.helper.makeX01PlayerState
import dartzee.helper.makeX01PlayerStateWithRounds
import dartzee.helper.makeX01Rounds
import dartzee.`object`.Dart
import dartzee.screen.game.AbstractGameStatisticsPanelTest
import dartzee.screen.game.getValueForRow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TestMatchStatisticsPanelX01 :
    AbstractGameStatisticsPanelTest<X01PlayerState, MatchStatisticsPanelX01>() {
    override fun factoryStatsPanel() = MatchStatisticsPanelX01(DEFAULT_X01_CONFIG.toJson())

    override fun makePlayerState() =
        makeX01PlayerState(completedRound = listOf(Dart(20, 1), Dart(5, 1), Dart(1, 1)))

    @Test
    fun `Should get the correct value for best finish`() {
        val finishOne = listOf(makeDart(17, 1), makeDart(20, 0), makeDart(20, 2))
        makeX01Rounds(57, finishOne)

        val finishTwo = listOf(makeDart(19, 2, startingScore = 38))
        val state = makeX01PlayerStateWithRounds(completedRounds = listOf(finishOne, finishTwo))

        val statsPanel = factoryStatsPanel()
        statsPanel.showStats(listOf(state))
        statsPanel.getValueForRow("Best Finish") shouldBe 57

        val finishThree = listOf(makeDart(18, 3), makeDart(20, 1), makeDart(13, 2))
        makeX01Rounds(100, finishThree)
        state.addCompletedRound(finishThree)
        statsPanel.showStats(listOf(state))
        statsPanel.getValueForRow("Best Finish") shouldBe 100
    }
}

package dartzee.ai

import dartzee.helper.AbstractTest
import dartzee.helper.ScoreAndSegmentType
import dartzee.helper.beastDartsModel
import dartzee.helper.insertPlayer
import dartzee.helper.predictableGolfModel
import dartzee.`object`.Dart
import dartzee.`object`.SegmentType
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TestDartsSimulationGolf: AbstractTest()
{
    @Test
    fun `Should simulate a game correctly for a perfect player`()
    {
        val model = beastDartsModel()
        val player = insertPlayer(model = model)

        val simulation = DartsSimulationGolf(player, model)
        val result = simulation.simulateGame(-1)

        result.finalScore shouldBe 18
        val darts = result.getAllDarts()
        darts.forEachIndexed { ix, drt ->
            drt.score shouldBe ix + 1
            drt.multiplier shouldBe 2
        }
    }

    @Test
    fun `Should pay attention to stop thresholds`()
    {
        val player = insertPlayer()

        val hmDartNoToStopThreshold = mutableMapOf(1 to 2, 2 to 3)
        val model = predictableGolfModel(hmDartNoToStopThreshold) { hole, _ ->
            when
            {
                hole == 1 -> ScoreAndSegmentType(1, SegmentType.TREBLE)
                hole == 2 -> ScoreAndSegmentType(2, SegmentType.INNER_SINGLE)
                hole == 3 -> ScoreAndSegmentType(3, SegmentType.DOUBLE)
                else -> ScoreAndSegmentType(hole, SegmentType.OUTER_SINGLE)
            }
        }

        val simulation = DartsSimulationGolf(player, model)
        val result = simulation.simulateGame(-1)

        //2 + 3 + 1 + (15*4)
        result.finalScore shouldBe 66
        val darts = result.getAllDarts()
        darts.shouldContainInOrder(
            Dart(1, 3),
            Dart(2, 1), Dart(2, 1),
            Dart(3, 2),
            Dart(4, 1), Dart(4, 1), Dart(4, 1))
    }
}
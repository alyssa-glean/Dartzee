package dartzee.ai

import dartzee.`object`.ColourWrapper
import dartzee.`object`.SegmentType
import dartzee.`object`.StatefulSegment
import dartzee.helper.AbstractTest
import dartzee.screen.Dartboard
import getPointForScore
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import java.awt.Point

class TestStrategyUtils: AbstractTest()
{
    @Test
    fun `Should aim at the average point for the relevant segment`()
    {
        val dartboard = FudgedDartboard()
        dartboard.paintDartboard()

        getPointForScore(AimDart(20, 3), dartboard) shouldBe Point(3, 4)
    }

    class FudgedDartboard : Dartboard(100, 100)
    {
        override fun paintDartboard(colourWrapper: ColourWrapper?, listen: Boolean)
        {
            super.paintDartboard(colourWrapper, listen)

            val segment = StatefulSegment(SegmentType.TREBLE, 20)
            segment.addPoint(Point(1, 7))
            segment.addPoint(Point(3, 3))
            segment.addPoint(Point(5, 2))

            hmSegmentKeyToSegment["20_${SegmentType.TREBLE}"] = segment
        }
    }
}
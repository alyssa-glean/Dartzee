package dartzee.dartzee

import com.github.alexburlton.swingtest.shouldMatchImage
import dartzee.dartzee.dart.DartzeeDartRuleOdd
import dartzee.helper.AbstractTest
import dartzee.screen.Dartboard
import dartzee.screen.game.dartzee.SegmentStatus
import dartzee.usingDartzeeDartboard
import dartzee.utils.DurationTimer
import dartzee.utils.getAllPossibleSegments
import io.kotlintest.matchers.numerics.shouldBeLessThan
import org.junit.Test
import java.awt.*
import javax.swing.ImageIcon
import javax.swing.JLabel

class TestDartzeeAimCalculator: AbstractTest()
{
    private val allNonMisses = getAllPossibleSegments().filter { !it.isMiss() }
    private val calculator = DartzeeAimCalculator()

    @Test
    fun `Should aim at the bullseye for a fully valid dartboard`()
    {
        val segmentStatus = SegmentStatus(allNonMisses, allNonMisses)
        verifyAim(segmentStatus, "All valid")
    }

    @Test
    fun `Should aim at the right place for all odd`()
    {
        val odd = allNonMisses.filter { DartzeeDartRuleOdd().isValidSegment(it) }
        val segmentStatus = SegmentStatus(odd, odd)
        verifyAim(segmentStatus, "Odd")
    }

    @Test
    fun `Should aim based on valid segments for if cautious`()
    {
        val twenties = allNonMisses.filter { it.score == 20 }
        val segmentStatus = SegmentStatus(twenties, allNonMisses)
        verifyAim(segmentStatus, "Score 20s - cautious", false)
    }

    @Test
    fun `Should aim based on scoring segments if aggressive`()
    {
        val twenties = allNonMisses.filter { it.score == 20 }
        val segmentStatus = SegmentStatus(twenties, allNonMisses)
        verifyAim(segmentStatus, "Score 20s - aggressive", true)
    }

    @Test
    fun `Should go on score for tie breakers`()
    {
        val trebles = allNonMisses.filter { it.getMultiplier() == 3 }
        val segmentStatus = SegmentStatus(trebles, trebles)
        verifyAim(segmentStatus, "Trebles")

        val treblesWithoutTwenty = trebles.filter { it.score != 20 }
        verifyAim(SegmentStatus(treblesWithoutTwenty, treblesWithoutTwenty), "Trebles (no 20)")
    }

    @Test
    fun `Should aim correctly if bullseye is missing`()
    {
        val nonBull = allNonMisses.filter { it.getTotal() != 50 }
        val segmentStatus = SegmentStatus(nonBull, nonBull)
        verifyAim(segmentStatus, "No bullseye")
    }

    @Test
    fun `Should aim correctly for some missing trebles`()
    {
        val segments = allNonMisses.filterNot { it.getMultiplier() == 3 && (it.score == 20 || it.score == 3) }
        val segmentStatus = SegmentStatus(segments, segments)
        verifyAim(segmentStatus, "Missing trebles")
    }

    @Test
    fun `Should be performant`()
    {
        usingDartzeeDartboard(400, 400) { dartboard ->
            val awkward = allNonMisses.filter { it.score != 25 }
            val segmentStatus = SegmentStatus(awkward, awkward)
            dartboard.refreshValidSegments(segmentStatus)

            val timer = DurationTimer()
            for (i in 1..10)
            {
                calculator.getPointToAimFor(dartboard, segmentStatus, true)
            }

            val timeElapsed = timer.getDuration()
            timeElapsed shouldBeLessThan 5000
        }
    }

    private fun verifyAim(segmentStatus: SegmentStatus, screenshotName: String, aggressive: Boolean = false)
    {
        usingDartzeeDartboard(400, 400) { dartboard ->
            dartboard.refreshValidSegments(segmentStatus)

            val pt = calculator.getPointToAimFor(dartboard, segmentStatus, aggressive)
            val lbl = dartboard.markPoints(listOf(pt))
            lbl.shouldMatchImage(screenshotName)
        }
    }
}

fun Dartboard.markPoints(points: List<Point>): JLabel
{
    val img = dartboardImage!!

    val g = img.graphics as Graphics2D
    g.color = Color.BLUE
    g.stroke = BasicStroke(3f)
    points.forEach { pt ->
        g.drawLine(pt.x - 5, pt.y - 5, pt.x + 5, pt.y + 5)
        g.drawLine(pt.x - 5, pt.y + 5, pt.x + 5, pt.y - 5)
    }

    val lbl = JLabel(ImageIcon(img))
    lbl.size = Dimension(500, 500)
    lbl.repaint()
    return lbl
}
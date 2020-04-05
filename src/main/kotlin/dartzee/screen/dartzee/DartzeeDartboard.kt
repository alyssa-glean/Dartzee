package dartzee.screen.dartzee

import dartzee.`object`.DartboardSegment
import dartzee.`object`.GREY_COLOUR_WRAPPER
import dartzee.screen.Dartboard
import dartzee.screen.game.dartzee.SegmentStatus
import dartzee.utils.getColourFromHashMap
import java.awt.Color

class DartzeeDartboard(width: Int = 400, height: Int = 400): Dartboard(width, height)
{
    var segmentStatus: SegmentStatus? = SegmentStatus(emptySet(), emptySet())

    fun refreshValidSegments(segmentStatus: SegmentStatus?)
    {
        this.segmentStatus = segmentStatus

        getAllSegments().forEach{
            colourSegment(it, false)
        }
    }

    override fun shouldActuallyHighlight(segment: DartboardSegment): Boolean {
        val status = segmentStatus
        return status == null || status.validSegments.contains(segment)
    }

    override fun getInitialColourForSegment(segment: DartboardSegment): Color
    {
        val status = segmentStatus
        val default = super.getInitialColourForSegment(segment)
        return when {
            status == null || segment.isMiss() -> default
            status.scoringSegments.contains(segment) -> default
            isValidSegment(status, segment) -> getColourFromHashMap(segment, GREY_COLOUR_WRAPPER)
            else -> Color.BLACK
        }
    }

    private fun isValidSegment(status: SegmentStatus, segment: DartboardSegment): Boolean
    {
        val validBecauseMiss = status.validSegments.any { it.isMiss() } && segment.isMiss()

        return status.validSegments.contains(segment) || validBecauseMiss
    }
}
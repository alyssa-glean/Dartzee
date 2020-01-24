package dartzee.screen.dartzee

import dartzee.`object`.DartboardSegment
import dartzee.screen.Dartboard
import java.awt.Color

class DartzeeDartboard(width: Int = 400, height: Int = 400): Dartboard(width, height)
{
    var validSegments = listOf<DartboardSegment>()

    fun refreshValidSegments(segments: List<DartboardSegment>)
    {
        this.validSegments = segments

        getAllSegments().forEach{
            colourSegment(it, false)
        }
    }

    override fun shouldActuallyHighlight(segment: DartboardSegment) = validSegments.contains(segment)

    override fun colourSegment(segment: DartboardSegment, col: Color)
    {
        if (!isValidSegment(segment))
        {
            val newCol = Color(col.red, col.green, col.blue, 20)
            super.colourSegment(segment, newCol)
        }
        else
        {
            super.colourSegment(segment, col)
        }
    }
    private fun isValidSegment(segment: DartboardSegment): Boolean
    {
        val validBecauseMiss =  validSegments.any { it.isMiss() } && segment.isMiss()

        return validSegments.contains(segment) || validBecauseMiss
    }
}
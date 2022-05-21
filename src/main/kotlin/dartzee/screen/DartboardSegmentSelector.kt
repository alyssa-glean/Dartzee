package dartzee.screen

import dartzee.`object`.ColourWrapper
import dartzee.`object`.DEFAULT_COLOUR_WRAPPER
import dartzee.`object`.DartboardSegment
import dartzee.`object`.StatefulSegment
import dartzee.utils.DartsColour
import dartzee.utils.getAllNonMissSegments
import dartzee.utils.getColourForSegment
import java.awt.Color
import java.awt.Point
import java.awt.event.MouseEvent

class DartboardSegmentSelector(width: Int = 500, height: Int = 500): Dartboard(width, height)
{
    var selectedSegments = mutableSetOf<DartboardSegment>()

    private var lastDraggedSegment: StatefulSegment? = null

    init
    {
        scoreLabelColor = Color.BLACK
        renderScoreLabels = true
    }

    fun initState(initialSelection: Set<DartboardSegment>)
    {
        initialSelection.forEach { dataSegment ->
            val mySegment = getSegment(dataSegment.score, dataSegment.type) ?: return

            selectedSegments.add(dataSegment)

            val col = getColourForSegment(dataSegment, DEFAULT_COLOUR_WRAPPER)
            colourSegment(mySegment, col)
        }
    }

    fun selectAll()
    {
        val unselectedSegments = getAllNonMissSegments() - selectedSegments
        toggleAll(unselectedSegments)
    }

    fun selectNone()
    {
        toggleAll(selectedSegments.toList())
    }

    private fun toggleAll(segments: List<DartboardSegment>)
    {
        segments.mapNotNull { getSegment(it.score, it.type) }.forEach(::toggleSegment)
    }


    fun paintDartboard()
    {
        val wireframe = ColourWrapper(DartsColour.TRANSPARENT)
        wireframe.edgeColour = Color.BLACK
        super.paintDartboard(wireframe, listen = true)
    }

    override fun dartThrown(pt: Point)
    {
        val segment = getSegmentForPoint(pt, false)
        toggleSegment(segment)
    }

    private fun toggleSegment(segment: StatefulSegment)
    {
        lastDraggedSegment = segment
        if (segment.isMiss())
        {
            return
        }

        val dataSegment = segment.toDataSegment()
        if (selectedSegments.contains(dataSegment))
        {
            selectedSegments.remove(dataSegment)

            colourSegment(segment, DartsColour.TRANSPARENT)
        }
        else
        {
            selectedSegments.add(dataSegment)

            val col = getColourForSegment(dataSegment, DEFAULT_COLOUR_WRAPPER)
            colourSegment(segment, col)
        }
    }

    override fun mouseMoved(arg0: MouseEvent)
    {
        //Do nothing
    }
    override fun mouseDragged(arg0: MouseEvent)
    {
        val segment = getSegmentForPoint(arg0.point, false)
        if (segment == lastDraggedSegment)
        {
            return
        }

        toggleSegment(segment)
    }

}
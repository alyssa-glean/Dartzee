package dartzee.screen

import dartzee.core.bean.paint
import dartzee.core.util.getParentWindow
import dartzee.core.util.runOnEventThreadBlocking
import dartzee.listener.DartboardListener
import dartzee.logging.CODE_RENDERED_DARTBOARD
import dartzee.logging.CODE_RENDER_ERROR
import dartzee.logging.KEY_DURATION
import dartzee.`object`.ColourWrapper
import dartzee.`object`.Dart
import dartzee.`object`.DartboardSegment
import dartzee.`object`.SegmentType
import dartzee.`object`.StatefulSegment
import dartzee.screen.game.DartsGameScreen
import dartzee.utils.AimPoint
import dartzee.utils.DartsColour
import dartzee.utils.DartsColour.DARTBOARD_BLACK
import dartzee.utils.DurationTimer
import dartzee.utils.InjectedThings.logger
import dartzee.utils.ResourceCache.BASE_FONT
import dartzee.utils.UPPER_BOUND_OUTSIDE_BOARD_RATIO
import dartzee.utils.computePointsForSegment
import dartzee.utils.convertForDestinationDartboard
import dartzee.utils.factorySegmentForPoint
import dartzee.utils.getAllNonMissSegments
import dartzee.utils.getAnglesForScore
import dartzee.utils.getColourForSegment
import dartzee.utils.getDartForSegment
import dartzee.utils.getPotentialAimPoints
import dartzee.utils.translatePoint
import java.awt.Canvas
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import javax.sound.sampled.Clip
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JLayeredPane
import javax.swing.SwingConstants
import kotlin.math.roundToInt

const val LAYER_DARTS = 2
const val LAYER_DODGY = 3
const val LAYER_SLIDER = 4

open class Dartboard(width: Int = 400, height: Int = 400): JLayeredPane(), MouseListener, MouseMotionListener
{
    protected val hmSegmentKeyToSegment = mutableMapOf<String, StatefulSegment>()

    private val dartLabels = mutableListOf<JLabel>()

    private val listeners: MutableList<DartboardListener> = mutableListOf()
    var renderDarts = false
    var centerPoint = Point(200, 200)
        private set

    var diameter = 360.0

    var scoreLabelColor: Color = Color.WHITE
    var renderScoreLabels = false

    private var dartCount = 0
    var simulation = false

    //Cached things
    private var lastHoveredSegment: StatefulSegment? = null
    private var colourWrapper: ColourWrapper? = null

    //For dodgy sounds/animations
    var latestClip: Clip? = null
    val dodgyLabel = JLabel("")

    var dartboardImage: BufferedImage? = null
    val dartboardLabel = JLabel()

    init
    {
        setSize(width, height)
        preferredSize = Dimension(width, height)
        dartboardLabel.setSize(width, height)
        layout = null
        dodgyLabel.name = "DodgyLabel"
        add(dartboardLabel, Integer.valueOf(-1))
    }

    fun addDartboardListener(listener: DartboardListener)
    {
        listeners.add(listener)
    }

    private fun repaintingSameSize() =
        dartboardLabel.width == width
                && dartboardLabel.height == height
                && hmSegmentKeyToSegment.isNotEmpty()

    open fun paintDartboard(colourWrapper: ColourWrapper? = null)
    {
        if (width <= 0 || height <= 0 || repaintingSameSize()) {
            return
        }

        val wasListening = isListening()
        val oldCenter = centerPoint
        val oldDiameter = diameter

        lastHoveredSegment = null
        stopListening()
        hmSegmentKeyToSegment.clear()

        val width = width
        val height = height

        val timer = DurationTimer()

        dartboardLabel.setSize(width, height)

        //Initialise/clear down variables
        this.colourWrapper = colourWrapper
        centerPoint = Point(width / 2, height / 2)
        diameter = 0.7 * width

        getAllNonMissSegments().forEach {
            val pts = computePointsForSegment(it, centerPoint, diameter)
            val segment = StatefulSegment(it.type, it.score)
            segment.points.addAll(pts)

            val segmentKey = "${segment.score}_${segment.type}"
            hmSegmentKeyToSegment[segmentKey] = segment
        }

        //Construct the segments, populated with their points. Cache pt -> segment.
        // getPointList(width, height).forEach { factoryAndCacheSegmentForPoint(it) }
        getAllSegments().forEach { it.computeEdgePoints() }

        paintDartboardImage()

        addScoreLabels()

        dartboardLabel.icon = ImageIcon(dartboardImage!!)
        dartboardLabel.repaint()

        if (wasListening) {
            ensureListening()
        }

        translateThrownDarts(oldCenter, oldDiameter)

        val duration = timer.getDuration()
        logger.info(CODE_RENDERED_DARTBOARD, "Rendered dartboard[$width, $height] in ${duration}ms",
            KEY_DURATION to duration)
    }

    private fun translateThrownDarts(oldCenter: Point, oldDiameter: Double)
    {
        val points = dartLabels.filter { it.isVisible }.map { it.location }
        val newPoints = points.map { convertForDestinationDartboard(it, oldCenter, oldDiameter, this) }

        clearDarts()
        newPoints.forEach(::addDart)
    }

    private fun paintDartboardImage()
    {
        val hmPointToColor = getAllSegments().flatMap { it.getColorMap(colourWrapper) }.toMap()

        dartboardImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        dartboardImage?.paint { hmPointToColor[it] }
    }

    private fun addScoreLabels()
    {
        if (!renderScoreLabels)
        {
            return
        }

        //Get the height we want for our labels, which is half the thickness of the outer band
        val radius = diameter / 2
        val outerRadius = UPPER_BOUND_OUTSIDE_BOARD_RATIO * radius
        val lblHeight = ((outerRadius - radius) / 2).roundToInt()

        val fontToUse = getFontForDartboardLabels(lblHeight)

        for (i in 1..20)
        {
            //Create a label with standard properties
            val lbl = JLabel("" + i)
            lbl.foreground = scoreLabelColor
            lbl.horizontalAlignment = SwingConstants.CENTER
            lbl.font = fontToUse

            //Work out the width for this label, based on the text
            val metrics = factoryFontMetrics(fontToUse)
            val lblWidth = metrics.stringWidth("" + i) + 5
            lbl.setSize(lblWidth, lblHeight)

            //Work out where to place the label
            val angle = getAnglesForScore(i).toList().average()
            val radiusForLabel = radius + lblHeight
            val avgPoint = translatePoint(centerPoint, radiusForLabel, angle)

            val lblX = avgPoint.getX().toInt() - lblWidth / 2
            val lblY = avgPoint.getY().toInt() - lblHeight / 2

            val g = dartboardImage!!.graphics as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.translate(lblX, lblY)
            lbl.paint(g)
        }
    }

    private fun getFontForDartboardLabels(lblHeight: Int): Font
    {
        //Start with a fontSize of 1
        var fontSize = 1f
        var font = BASE_FONT.deriveFont(Font.PLAIN, fontSize)

        //We're going to increment our test font 1 at a time, and keep checking its height
        var testFont = font
        var metrics = factoryFontMetrics(testFont)
        var fontHeight = metrics.height

        while (fontHeight < lblHeight - 2)
        {
            //The last iteration succeeded, so set our return value to be the font we tested.
            font = testFont

            //Create a new testFont, with incremented font size
            fontSize++
            testFont = BASE_FONT.deriveFont(Font.PLAIN, fontSize)

            //Get the updated font height
            metrics = factoryFontMetrics(testFont)
            fontHeight = metrics.height
        }

        return font
    }

    private fun factoryFontMetrics(font: Font): FontMetrics
    {
        //Use a new Canvas rather than going via graphics, as then this will work headless (e.g. from tests)
        return Canvas().getFontMetrics(font)
    }

    fun highlightDartboard(hoveredPoint: Point)
    {
        // We might not have told the dartboard to paint yet (it's invoked later onto the EDT)
        dartboardImage ?: return

        val hoveredSegment = getSegmentForPoint(hoveredPoint)
        if (hoveredSegment == lastHoveredSegment)
        {
            //Nothing to do
            return
        }

        lastHoveredSegment?.let { colourSegment(it, false) }

        lastHoveredSegment = hoveredSegment
        colourSegment(hoveredSegment, true)
    }

    open fun getInitialColourForSegment(segment: DartboardSegment) = getColourForSegment(segment, colourWrapper)

    fun colourSegment(segment: StatefulSegment, highlight: Boolean)
    {
        val dataSegment = segment.toDataSegment()
        if (segment.isMiss())
        {
            return
        }

        val colour = getInitialColourForSegment(dataSegment)
        val hoveredColour = if (highlight && shouldActuallyHighlight(dataSegment)) getHighlightedColour(colour) else colour

        colourSegment(segment, hoveredColour)
    }
    private fun getHighlightedColour(colour: Color): Color =
        if (colour == DARTBOARD_BLACK)
        {
            Color.DARK_GRAY
        }
        else
        {
            DartsColour.getDarkenedColour(colour)
        }

    open fun shouldActuallyHighlight(segment: DartboardSegment) = true

    fun colourSegment(segment: StatefulSegment, col: Color)
    {
        val pointsForCurrentSegment = segment.points
        val edgeColour = getEdgeColourForSegment(segment.toDataSegment())
        for (pt in pointsForCurrentSegment)
        {
            if (edgeColour != null && segment.isEdgePoint(pt))
            {
                colourPoint(pt, edgeColour)
            }
            else
            {
                colourPoint(pt, col)
            }
        }

        dartboardLabel.repaint()
    }

    open fun getEdgeColourForSegment(segment: DartboardSegment) = colourWrapper?.edgeColour

    private fun colourPoint(pt: Point, colour: Color)
    {
        val x = pt.getX().toInt()
        val y = pt.getY().toInt()

        val rgb = colour.rgb
        val currentRgb = dartboardImage!!.getRGB(x, y)

        if (rgb != currentRgb)
        {
            dartboardImage!!.setRGB(x, y, rgb)
        }
    }

    fun getAllSegments() = hmSegmentKeyToSegment.values.toList()

    fun getDataSegmentForPoint(pt: Point) = factorySegmentForPoint(pt, centerPoint, diameter)
    protected fun getSegmentForPoint(pt: Point, stackTrace: Boolean = true): StatefulSegment
    {
        val segment = getAllSegments().firstOrNull { it.containsPoint(pt) }
        if (segment != null)
        {
            return segment
        }

        if (stackTrace)
        {
            logger.error(CODE_RENDER_ERROR, "Couldn't find segment for point (" + pt.getX() + ", " + pt.getY() + ")."
                    + "Width = " + width + ", Height = " + height)
        }

        return factoryAndCacheSegmentForPoint(pt)
    }

    private fun factoryAndCacheSegmentForPoint(pt: Point): StatefulSegment
    {
        val newSegment = factorySegmentForPoint(pt, centerPoint, diameter)
        val segmentKey = "${newSegment.score}_${newSegment.type}"

        val segment = hmSegmentKeyToSegment.getOrPut(segmentKey) { StatefulSegment(newSegment.type, newSegment.score) }
        segment.addPoint(pt)
        return segment
    }

    /**
     * Public methods
     */
    fun getPointsForSegment(score: Int, type: SegmentType): Set<Point>
    {
        val segmentKey = score.toString() + "_" + type
        val segment = hmSegmentKeyToSegment[segmentKey]
        return segment?.points ?: setOf()
    }
    fun getSegment(score: Int, type: SegmentType): StatefulSegment? = hmSegmentKeyToSegment["${score}_$type"]

    fun isDouble(pt: Point): Boolean
    {
        val seg = getDataSegmentForPoint(pt)
        return seg.isDoubleExcludingBull()
    }

    fun getPotentialAimPoints() = getPotentialAimPoints(centerPoint, diameter)
    fun translateAimPoint(aimPoint: AimPoint) = AimPoint(centerPoint, diameter / 2, aimPoint.angle, aimPoint.ratio).point

    open fun dartThrown(pt: Point)
    {
        val dart = convertPointToDart(pt, true)

        if (renderDarts)
        {
            runOnEventThreadBlocking { addDart(pt) }
        }

        listeners.forEach {
            it.dartThrown(dart)
        }
    }

    fun addOverlay(pt: Point, overlay: Component)
    {
        add(overlay)
        setLayer(overlay, LAYER_SLIDER)
        overlay.location = pt
    }

    fun convertPointToDart(pt: Point, rationalise: Boolean): Dart
    {
        if (rationalise)
        {
            rationalisePoint(pt)
        }

        val segment = getDataSegmentForPoint(pt)
        return getDartForSegment(pt, segment)
    }

    fun rationalisePoint(pt: Point)
    {
        val x = pt.x.coerceIn(0, width - 1)
        val y = pt.y.coerceIn(0, height - 1)

        pt.setLocation(x, y)
    }

    fun ensureListening()
    {
        if (!isListening())
        {
            dartboardLabel.addMouseListener(this)
            dartboardLabel.addMouseMotionListener(this)
        }
    }

    fun stopListening()
    {
        if (isListening())
        {
            dartboardLabel.removeMouseListener(this)
            dartboardLabel.removeMouseMotionListener(this)
        }

        //Undo any colouring that there might have been
        lastHoveredSegment?.let { colourSegment(it, false) }
    }

    fun isListening() = dartboardLabel.mouseListeners.isNotEmpty()

    private fun addDart(pt: Point)
    {
        if (dartLabels.isEmpty())
        {
            for (i in 0..4)
            {
                val lbl = JLabel(DARTIMG)
                lbl.setSize(76, 80)
                lbl.isVisible = false
                add(lbl)

                dartLabels.add(lbl)
            }
        }

        val lbl = dartLabels[dartCount]
        lbl.location = pt
        lbl.isVisible = true
        dartCount++

        setLayer(lbl, LAYER_DARTS, 5-dartCount)

        revalidate()
        repaint()
    }

    fun clearDarts()
    {
        for (i in dartLabels.indices)
        {
            val dartLabel = dartLabels[i]
            dartLabel.isVisible = false
        }

        dartCount = 0
        revalidate()
        repaint()
    }

    override fun mouseMoved(arg0: MouseEvent)
    {
        if (getParentWindow()?.isFocused == true)
        {
            highlightDartboard(arg0.point)
        }
    }

    override fun mouseReleased(arg0: MouseEvent)
    {
        if (!suppressClickForGameWindow())
        {
            val pt = arg0.point
            dartThrown(pt)
        }
    }
    private fun suppressClickForGameWindow(): Boolean
    {
        val scrn = getParentWindow() as? DartsGameScreen ?: return false
        if (scrn.haveLostFocus)
        {
            scrn.haveLostFocus = false
            return true
        }

        return false
    }

    override fun mouseClicked(arg0: MouseEvent) {}
    override fun mousePressed(arg0: MouseEvent) {}
    override fun mouseDragged(arg0: MouseEvent) {}
    override fun mouseEntered(arg0: MouseEvent) {}
    override fun mouseExited(arg0: MouseEvent) {}

    fun factoryOverlay() = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    companion object
    {
        private val DARTIMG = ImageIcon(Dartboard::class.java.getResource("/dartImage.png"))
    }
}

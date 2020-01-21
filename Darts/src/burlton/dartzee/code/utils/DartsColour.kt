package burlton.dartzee.code.utils

import burlton.dartzee.code.core.util.Debug
import java.awt.Color
import java.awt.Component
import java.lang.Float.max

object DartsColour
{
    val TRANSPARENT = Color(0, 0, 0, 0)

    val DARTBOARD_RED: Color = Color.red
    val DARTBOARD_GREEN: Color = Color.green
    val DARTBOARD_BLACK: Color = Color.getHSBColor(0f, 0f, 0.1.toFloat())
    val DARTBOARD_WHITE: Color = Color.white

    val DARTBOARD_LIGHTEST_GREY: Color = Color.getHSBColor(0f, 0f, 0.9.toFloat())
    val DARTBOARD_LIGHTER_GREY: Color = Color.getHSBColor(0f, 0f, 0.75.toFloat())
    val DARTBOARD_LIGHT_GREY: Color = Color.getHSBColor(0f, 0f, 0.6.toFloat())

    val COLOUR_GOLD_TEXT: Color = Color.getHSBColor(5.toFloat() / 36, 1f, 0.8.toFloat())
    val COLOUR_SILVER_TEXT: Color = Color.GRAY.darker().darker()

    val COLOUR_BRONZE: Color = Color.getHSBColor(45.toFloat() / 360, 0.91.toFloat(), 0.49.toFloat())
    val COLOUR_BRONZE_TEXT: Color = COLOUR_BRONZE.darker().darker()

    val COLOUR_ACHIEVEMENT_ORANGE: Color = Color.getHSBColor(0.1f, 1f, 1f)

    val COLOUR_PASTEL_BLUE: Color = Color.getHSBColor(242.toFloat() / 360, 0.48.toFloat(), 0.8.toFloat())

    fun getDarkenedColour(colour: Color): Color = colour.darker().darker()

    fun getBrightenedColour(colour: Color): Color
    {
        var hsbValues = FloatArray(3)
        hsbValues = Color.RGBtoHSB(colour.red, colour.green, colour.blue, hsbValues)

        val decreasedSat = max(0f, (hsbValues[1] - 0.5).toFloat())
        return Color.getHSBColor(hsbValues[0], decreasedSat, hsbValues[2])
    }

    fun toPrefStr(colour: Color): String {
        val r = colour.red
        val g = colour.green
        val b = colour.blue
        val a = colour.alpha

        return "$r;$g;$b;$a"
    }

    fun getColorFromPrefStr(prefsStr: String, defaultColor: Color?) = if (prefsStr.isEmpty()) defaultColor else fromPrefStr(prefsStr)
    private fun fromPrefStr(prefStr: String) =
        try
        {
            val colours = prefStr.split(";").map{ it.toInt() }
            Color(colours[0], colours[1], colours[2], colours[3])
        }
        catch (t: Throwable)
        {
            Debug.stackTrace("Failed to reconstruct colour from string: $prefStr")
            null
        }


    fun setFgAndBgColoursForPosition(c: Component, finishPos: Int, defaultBg: Color? = null)
    {
        when (finishPos)
        {
            -1 -> setColors(c, defaultBg, null)
            1 -> setColors(c, Color.YELLOW, COLOUR_GOLD_TEXT)
            2 -> setColors(c, Color.GRAY, COLOUR_SILVER_TEXT)
            3 -> setColors(c, COLOUR_BRONZE, COLOUR_BRONZE_TEXT)
            else -> setColors(c, Color.BLACK, COLOUR_BRONZE)
        }
    }
    private fun setColors(c: Component, background: Color?, foreground: Color?)
    {
        c.background = background
        c.foreground = foreground
    }


    fun getScorerForegroundColour(totalScore: Double): Color
    {
        val hueFactor = PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_HUE_FACTOR)
        val fgBrightness = PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_FG_BRIGHTNESS)
        return getScorerColour(totalScore, hueFactor, fgBrightness)
    }

    fun getScorerBackgroundColour(totalScore: Double): Color
    {
        val hueFactor = PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_HUE_FACTOR)
        val bgBrightness = PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_BG_BRIGHTNESS)
        return getScorerColour(totalScore, hueFactor, bgBrightness)
    }

    fun getScorerColour(totalScore: Double, multiplier: Double, brightness: Double): Color
    {
        return getProportionalColour(totalScore, 180, multiplier, brightness)
    }

    fun getProportionalColour(value: Double, total: Int, multiplier: Double, brightness: Double): Color
    {
        val hue = (value * multiplier).toFloat() / total
        return Color.getHSBColor(hue, 1f, brightness.toFloat())
    }

    fun getProportionalColourRedToGreen(value: Double, total: Int, brightness: Double) = getProportionalColour(value, total, 0.4, brightness)
}

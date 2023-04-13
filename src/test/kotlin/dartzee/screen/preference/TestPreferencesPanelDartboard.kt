package dartzee.screen.preference

import com.github.alyssaburlton.swingtest.awaitCondition
import com.github.alyssaburlton.swingtest.getChild
import com.github.alyssaburlton.swingtest.shouldMatchImage
import dartzee.bean.PresentationDartboard
import dartzee.utils.*
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.JFrame

class TestPreferencesPanelDartboard: AbstractPreferencePanelTest<PreferencesPanelDartboard>()
{
    @Test
    @Tag("screenshot")
    fun `Dartboard should refresh when settings are changed`()
    {
        val frame = JFrame()
        frame.size = Dimension(800, 600)
        frame.layout = BorderLayout(0, 0)

        val panel = PreferencesPanelDartboard()
        panel.refresh(true)
        frame.add(panel, BorderLayout.CENTER)
        frame.isVisible = true

        awaitCondition {
            panel.getChild<PresentationDartboard>().isShowing &&
            panel.getChild<PresentationDartboard>().width > 0
        }

        setUiFieldValuesToNonDefaults(panel)

        awaitCondition { panel.getChild<PresentationDartboard>().width > 0 }
        val dartboard = panel.getChild<PresentationDartboard>()
        dartboard.shouldMatchImage("preferencesDartboard")
    }

    override fun factory() = PreferencesPanelDartboard()

    override fun getPreferencesAffected(): MutableList<String>
    {
        return mutableListOf(PREFERENCES_STRING_EVEN_SINGLE_COLOUR,
                PREFERENCES_STRING_EVEN_DOUBLE_COLOUR,
                PREFERENCES_STRING_EVEN_TREBLE_COLOUR,
                PREFERENCES_STRING_ODD_SINGLE_COLOUR,
                PREFERENCES_STRING_ODD_DOUBLE_COLOUR,
                PREFERENCES_STRING_ODD_TREBLE_COLOUR)
    }

    override fun checkUiFieldValuesAreDefaults(panel: PreferencesPanelDartboard)
    {
        panel.cpOddSingle.selectedColour shouldBe DartsColour.DARTBOARD_WHITE
        panel.cpOddDouble.selectedColour shouldBe DartsColour.DARTBOARD_GREEN
        panel.cpOddTreble.selectedColour shouldBe DartsColour.DARTBOARD_GREEN

        panel.cpEvenSingle.selectedColour shouldBe DartsColour.DARTBOARD_BLACK
        panel.cpEvenDouble.selectedColour shouldBe DartsColour.DARTBOARD_RED
        panel.cpEvenTreble.selectedColour shouldBe DartsColour.DARTBOARD_RED
    }

    override fun setUiFieldValuesToNonDefaults(panel: PreferencesPanelDartboard)
    {
        panel.cpOddSingle.updateSelectedColor(Color.BLUE)
        panel.cpOddDouble.updateSelectedColor(Color(200, 50, 128))
        panel.cpOddTreble.updateSelectedColor(Color.getHSBColor(0.9f, 0.8f, 1.0f))

        panel.cpEvenSingle.updateSelectedColor(Color.YELLOW)
        panel.cpEvenDouble.updateSelectedColor(Color.MAGENTA)
        panel.cpEvenTreble.updateSelectedColor(Color.CYAN)
    }

    override fun checkUiFieldValuesAreNonDefaults(panel: PreferencesPanelDartboard)
    {
        panel.cpOddSingle.selectedColour shouldBe Color.BLUE
        panel.cpOddDouble.selectedColour shouldBe Color(200, 50, 128)
        panel.cpOddTreble.selectedColour shouldBe Color.getHSBColor(0.9f, 0.8f, 1.0f)

        panel.cpEvenSingle.selectedColour shouldBe Color.YELLOW
        panel.cpEvenDouble.selectedColour shouldBe Color.MAGENTA
        panel.cpEvenTreble.selectedColour shouldBe Color.CYAN
    }

    override fun checkPreferencesAreSetToNonDefaults()
    {
        val evenSingleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_EVEN_SINGLE_COLOUR)
        val evenDoubleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_EVEN_DOUBLE_COLOUR)
        val evenTrebleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_EVEN_TREBLE_COLOUR)
        val oddSingleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_ODD_SINGLE_COLOUR)
        val oddDoubleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_ODD_DOUBLE_COLOUR)
        val oddTrebleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_ODD_TREBLE_COLOUR)

        DartsColour.getColorFromPrefStr(oddSingleStr) shouldBe Color.BLUE
        DartsColour.getColorFromPrefStr(oddDoubleStr) shouldBe Color(200, 50, 128)
        DartsColour.getColorFromPrefStr(oddTrebleStr) shouldBe Color.getHSBColor(0.9f, 0.8f, 1.0f)
        DartsColour.getColorFromPrefStr(evenSingleStr) shouldBe Color.YELLOW
        DartsColour.getColorFromPrefStr(evenDoubleStr) shouldBe Color.MAGENTA
        DartsColour.getColorFromPrefStr(evenTrebleStr) shouldBe Color.CYAN
    }
}
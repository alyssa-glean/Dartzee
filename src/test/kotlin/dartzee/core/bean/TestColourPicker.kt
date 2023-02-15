package dartzee.core.bean

import com.github.alexburlton.swingtest.doClick
import com.github.alexburlton.swingtest.doHover
import com.github.alexburlton.swingtest.doHoverAway
import dartzee.core.helper.getIconImage
import dartzee.core.util.InjectedDesktopCore
import dartzee.helper.AbstractTest
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Cursor

class TestColourPicker: AbstractTest()
{
    @Test
    fun `Should change the cursor to a hand on hover`()
    {
        val cp = ColourPicker()
        cp.doHover()
        cp.cursor.type shouldBe Cursor.HAND_CURSOR

        cp.doHoverAway()
        cp.cursor.type shouldBe Cursor.DEFAULT_CURSOR
    }

    @Test
    fun `Should support updating the current colour`()
    {
        val cp = ColourPicker()
        cp.updateSelectedColor(Color.RED)

        cp.selectedColour shouldBe Color.RED
        val img = cp.getIconImage()
        Color(img.getRGB(10, 10)) shouldBe Color.RED
    }

    @Test
    fun `Should update the colour on mouse click to whatever was selected in the dialog`()
    {
        val mockSelector = mockk<IColourSelector>(relaxed = true)
        every { mockSelector.selectColour(any()) } returns Color.BLUE

        InjectedDesktopCore.colourSelector = mockSelector

        val cp = ColourPicker()
        cp.updateSelectedColor(Color.RED)
        cp.doClick()

        verify { mockSelector.selectColour(Color.RED) }

        cp.selectedColour shouldBe Color.BLUE
        val img = cp.getIconImage()
        Color(img.getRGB(10, 10)) shouldBe Color.BLUE
    }

    @Test
    fun `Should notify its listener if a new colour is selected`()
    {
        val mockSelector = mockk<IColourSelector>(relaxed = true)
        every { mockSelector.selectColour(any()) } returns Color.BLUE
        InjectedDesktopCore.colourSelector = mockSelector

        val listener = mockk<ColourSelectionListener>(relaxed = true)

        val cp = ColourPicker()
        cp.addColourSelectionListener(listener)
        cp.doClick()

        verify { listener.colourSelected(Color.BLUE) }
    }
}
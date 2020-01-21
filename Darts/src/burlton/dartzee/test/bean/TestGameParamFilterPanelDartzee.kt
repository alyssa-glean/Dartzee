package burlton.dartzee.test.bean

import burlton.dartzee.test.core.helper.verifyNotCalled
import burlton.dartzee.code.bean.GameParamFilterPanelDartzee
import burlton.dartzee.test.helper.AbstractDartsTest
import burlton.dartzee.test.helper.insertDartzeeTemplate
import burlton.dartzee.code.core.bean.ComboBoxItem
import burlton.dartzee.code.core.bean.items
import burlton.dartzee.code.core.bean.selectedItemTyped
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.awt.event.ActionListener

class TestGameParamFilterPanelDartzee: AbstractDartsTest()
{
    @Test
    fun `Should populate empty if there are no templates set up`()
    {
        val panel = GameParamFilterPanelDartzee()

        panel.getOptions().shouldContainExactly(
            ComboBoxItem(null, "Custom"),
            ComboBoxItem(null, "-----", false),
            ComboBoxItem(null, "No templates configured", false))
    }

    @Test
    fun `Should always return empty gameParams if there are no templates set up`()
    {
        val panel = GameParamFilterPanelDartzee()

        panel.comboBox.selectedIndex = 0
        panel.getGameParams() shouldBe ""
        panel.getFilterDesc() shouldBe "custom games"

        panel.comboBox.selectedIndex = 1
        panel.getGameParams() shouldBe ""
        panel.getFilterDesc() shouldBe "custom games"

        panel.comboBox.selectedIndex = 2
        panel.getGameParams() shouldBe ""
        panel.getFilterDesc() shouldBe "custom games"
    }

    @Test
    fun `Should add templates below the divider if they exist`()
    {
        insertDartzeeTemplate(name = "Template A")
        insertDartzeeTemplate(name = "Template B")

        val panel = GameParamFilterPanelDartzee()

        panel.getOptions().map { it.visibleData }.shouldContainExactly(
            "Custom",
            "-----",
            "Template A",
            "Template B")
    }

    @Test
    fun `Selecting a template should populate its ID as the game params`()
    {
        val templateId = insertDartzeeTemplate(name = "Template A").rowId

        val panel = GameParamFilterPanelDartzee()
        panel.comboBox.selectedIndex = 2

        panel.getGameParams() shouldBe templateId
        panel.getFilterDesc() shouldBe "games for template 'Template A'"
    }

    @Test
    fun `Should support setting the selection by gameParams`()
    {
        val templateId = insertDartzeeTemplate(name = "Some Template").rowId

        val panel = GameParamFilterPanelDartzee()
        panel.comboBox.selectedItemTyped().visibleData shouldBe "Custom"

        panel.setGameParams(templateId)

        panel.comboBox.selectedItemTyped().visibleData shouldBe "Some Template"
    }

    @Test
    fun `Should enable and disable its comboBox correctly`()
    {
        val panel = GameParamFilterPanelDartzee()

        panel.enableChildren(false)
        panel.comboBox.isEnabled shouldBe false

        panel.enableChildren(true)
        panel.comboBox.isEnabled shouldBe true
    }

    @Test
    fun `Should add and remove action listeners on the combo box`()
    {
        val panel = GameParamFilterPanelDartzee()

        val listener = mockk<ActionListener>(relaxed = true)

        panel.addActionListener(listener)
        panel.comboBox.selectedIndex = 1

        verify { listener.actionPerformed(any()) }

        clearAllMocks()

        panel.removeActionListener(listener)
        panel.comboBox.selectedIndex = 0

        verifyNotCalled { listener.actionPerformed(any()) }
    }


    private fun GameParamFilterPanelDartzee.getOptions() = comboBox.items()
}
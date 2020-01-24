package dartzee.screen.preference

import dartzee.core.helper.verifyNotCalled
import dartzee.helper.AbstractTest
import dartzee.screen.Dartboard
import dartzee.screen.ScreenCache
import dartzee.screen.game.DartsGameScreen
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TestPreferencesDialog: AbstractTest()
{
    private var dialog = PreferencesDialog()
    private var mockPanel = mockk<AbstractPreferencesPanel>(relaxed = true)

    override fun beforeEachTest()
    {
        super.beforeEachTest()

        dialog = PreferencesDialog()
        mockPanel = mockk(relaxed = true)

        dialog.tabbedPane.add(mockPanel)
    }

    @Test
    fun `Should refresh panels on init`()
    {
        dialog.init()

        verify { mockPanel.refresh(false) }
    }

    @Test
    fun `Shouldn't call save if a panel is invalid`()
    {
        every { mockPanel.valid() } returns false

        dialog.btnOk.doClick()

        verify { mockPanel.valid() }
        verifyNotCalled { mockPanel.save() }

        dialog.tabbedPane.selectedComponent shouldBe mockPanel
    }

    @Test
    fun `Should call save if all panels are valid`()
    {
        every { mockPanel.valid() } returns true

        dialog.init()
        dialog.btnOk.doClick()

        verify { mockPanel.valid() }
        verify { mockPanel.save() }
    }

    @Test
    fun `Should not reset unselected panels when restore defaults is pressed`()
    {
        dialog.btnRestoreDefaults.doClick()

        verifyNotCalled { mockPanel.refresh(true) }
    }

    @Test
    fun `Should reset the selected panel when restore defaults is pressed`()
    {
        dialog.tabbedPane.selectedComponent = mockPanel
        dialog.btnRestoreDefaults.doClick()

        verify { mockPanel.refresh(true) }
    }

    @Test
    fun `Should reset cached application values when successfully okayed`()
    {
        every { mockPanel.valid() } returns true

        Dartboard.dartboardTemplate = mockk(relaxed = true)
        val mockGameScreen = mockk<DartsGameScreen>(relaxed = true)
        ScreenCache.addDartsGameScreen("1", mockGameScreen)

        dialog.init()
        dialog.btnOk.doClick()

        Dartboard.dartboardTemplate shouldBe null
        verify { mockGameScreen.fireAppearancePreferencesChanged() }
    }

    @Test
    fun `Should not call valid or save when cancelled`()
    {
        dialog.btnCancel.doClick()

        verifyNotCalled { mockPanel.valid() }
        verifyNotCalled { mockPanel.save() }
    }
}

package dartzee.core.screen

import dartzee.core.util.Debug
import dartzee.core.helper.getLogs
import dartzee.core.screen.SimpleDialog
import dartzee.helper.AbstractTest
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import org.junit.Test

class TestSimpleDialog: AbstractTest()
{
    var allowCancel = true

    @Test
    fun `Should show or hide the cancel button as appropriate`()
    {
        allowCancel = true
        val dlg = SimpleDialogTestExtension()
        dlg.btnCancel.isVisible shouldBe true

        allowCancel = false
        val dlg2 = SimpleDialogTestExtension()
        dlg2.btnCancel.isVisible shouldBe false
    }

    @Test
    fun `Pressing cancel should dispose the dialog by default`()
    {
        allowCancel = true

        val dlg = SimpleDialogTestExtension()
        dlg.isVisible = true

        dlg.btnCancel.doClick()

        dlg.isVisible shouldBe false
    }

    @Test
    fun `Pressing ok should do whatever has been implemented`()
    {
        val dlg = SimpleDialogTestExtension()

        dlg.btnOk.doClick()

        getLogs() shouldContain "Ok pressed"
    }


    inner class SimpleDialogTestExtension: SimpleDialog()
    {
        override fun okPressed()
        {
            Debug.append("Ok pressed")
        }

        override fun allowCancel() = allowCancel

    }

}
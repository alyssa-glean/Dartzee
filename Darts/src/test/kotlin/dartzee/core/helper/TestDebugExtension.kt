package dartzee.core.helper

import dartzee.core.util.DebugExtension
import dartzee.core.util.DialogUtil

class TestDebugExtension: DebugExtension
{
    override fun exceptionCaught(showError: Boolean)
    {
        if (showError)
        {
            DialogUtil.showError("Exception")
        }
    }

    override fun unableToEmailLogs(){}
    override fun sendEmail(title: String, message: String){}
}
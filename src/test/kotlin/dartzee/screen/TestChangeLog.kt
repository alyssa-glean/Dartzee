package dartzee.screen

import dartzee.helper.AbstractTest
import dartzee.utils.DARTS_VERSION_NUMBER
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class TestChangeLog: AbstractTest()
{
    @Test
    fun `Should include the current version number`()
    {
        val changeLog = ChangeLog()

        val contents = changeLog.textArea.text
        contents.lines().first() shouldContain DARTS_VERSION_NUMBER
    }
}
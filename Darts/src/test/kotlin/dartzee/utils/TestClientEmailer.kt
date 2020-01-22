package dartzee.utils

import dartzee.core.helper.exceptionLogged
import dartzee.core.helper.getLogs
import dartzee.`object`.DartsClient
import dartzee.utils.ClientEmailer
import dartzee.utils.ClientEmailer.TEMP_DIRECTORY
import dartzee.utils.LOG_FILENAME_PREFIX
import dartzee.helper.AbstractTest
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.string.shouldEndWith
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldBe
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class TestClientEmailer: AbstractTest()
{
    override fun afterEachTest()
    {
        super.afterEachTest()

        File(TEMP_DIRECTORY).deleteRecursively()
    }

    @Test
    fun `Should not attempt to send email with no log secret`()
    {
        DartsClient.logSecret = ""
        ClientEmailer.canSendEmail() shouldBe false

        DartsClient.logSecret = "foo"
        ClientEmailer.canSendEmail() shouldBe true
    }

    @Test
    fun `Should write to a file if unable to send email`()
    {
        DartsClient.logSecret = ""
        ClientEmailer.sendClientEmail("Title", "Body")

        verifyLogFile()
    }

    @Test
    fun `Should not send old logs if temp does not exist`()
    {
        ClientEmailer.tryToSendUnsentLogs()

        getLogs() shouldContain "$TEMP_DIRECTORY does not exist, no logs to resend"
    }

    @Test
    fun `Should not attempt to send logs of the wrong name`()
    {
        val tempDir = File(TEMP_DIRECTORY)
        tempDir.mkdirs()

        val fileName = "$tempDir/foo.txt"
        File(fileName).writeText("Hello")

        ClientEmailer.tryToSendUnsentLogs()

        getLogs() shouldContain "There are no logs to resend in $TEMP_DIRECTORY"
    }

    @Test
    fun `Should not attempt to send logs of the wrong format`()
    {
        val tempDir = File(ClientEmailer.TEMP_DIRECTORY)
        tempDir.mkdirs()

        val fileName = "$tempDir/${LOG_FILENAME_PREFIX}_${System.currentTimeMillis()}.xml"
        File(fileName).writeText("Hello")

        ClientEmailer.tryToSendUnsentLogs()

        getLogs() shouldContain "There are no logs to resend in $TEMP_DIRECTORY"
    }

    @Test
    fun `Should send email successfully`()
    {
        DartsClient.logSecret = System.getProperty("logSecret")

        ClientEmailer.sendClientEmail("Unit Test", "This is a test")
    }

    @Test
    fun `Should fail with incorrect password`()
    {
        DartsClient.logSecret = "foo"

        ClientEmailer.sendClientEmail("Title", "Body")

        exceptionLogged() shouldBe true
        getLogs() shouldContain "javax.mail.AuthenticationFailedException"
        dialogFactory.errorsShown.shouldBeEmpty()
        verifyLogFile()
    }

    fun verifyLogFile()
    {
        val files = File(TEMP_DIRECTORY).listFiles()!!

        files.size shouldBe 1
        val f = files.first()
        f.name shouldStartWith(LOG_FILENAME_PREFIX)
        f.name shouldEndWith(".txt")

        val logStr = f.readText(StandardCharsets.UTF_8)
        logStr shouldBe "Title\nBody"
    }
}
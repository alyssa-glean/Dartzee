package dartzee.helper

import dartzee.core.helper.TestMessageDialogFactory
import dartzee.core.util.DialogUtil
import dartzee.logging.LogDestinationSystemOut
import dartzee.logging.LogRecord
import dartzee.logging.Logger
import dartzee.logging.LoggingCode
import dartzee.logging.Severity
import dartzee.screen.ScreenCache
import dartzee.utils.DartsDatabaseUtil
import dartzee.utils.InjectedThings
import dartzee.utils.InjectedThings.mainDatabase
import io.kotest.assertions.fail
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import sun.awt.AppContext
import java.awt.Window
import javax.swing.SwingUtilities

private val logDestination = FakeLogDestination()
val logger = Logger(listOf(logDestination, LogDestinationSystemOut()))
private var checkedForExceptions = false

const val TEST_ROOT = "Test/"
const val TEST_DB_DIRECTORY = "Test/Databases"

@ExtendWith(BeforeAllTestsExtension::class)
abstract class AbstractTest
{
    val dialogFactory = TestMessageDialogFactory()

    @BeforeEach
    fun beforeEachTest()
    {
        ScreenCache.emptyCache()
        dialogFactory.reset()
        clearLogs()
        clearAllMocks()

        DialogUtil.init(dialogFactory)

        mainDatabase.localIdGenerator.hmLastAssignedIdByEntityName.clear()

        if (logDestination.haveRunInsert)
        {
            wipeDatabase()
            logDestination.haveRunInsert = false
        }

        InjectedThings.esDestination = mockk(relaxed = true)
        InjectedThings.dartzeeCalculator = FakeDartzeeCalculator()

        logger.loggingContext.clear()
    }

    @AfterEach
    fun afterEachTest()
    {
        if (!checkedForExceptions)
        {
            val errors = getErrorsLogged()
            if (errors.isNotEmpty())
            {
                fail("Unexpected error(s) were logged during test: ${errors.map { it.toJsonString() } }")
            }
            errorLogged() shouldBe false
        }

        val windows = Window.getWindows()
        if (windows.isNotEmpty())
        {
            SwingUtilities.invokeAndWait {
                val visibleWindows = windows.filter { it.isVisible }
                visibleWindows.forEach { it.dispose() }
            }

            AppContext.getAppContext().remove(Window::class.java)
        }

        checkedForExceptions = false
    }

    fun wipeDatabase()
    {
        DartsDatabaseUtil.getAllEntitiesIncludingVersion().forEach { wipeTable(it.getTableName()) }
    }

    fun getLastLog() = getLogRecords().last()

    fun verifyLog(code: LoggingCode, severity: Severity = Severity.INFO): LogRecord
    {
        val record = findLog(code, severity)
        record.shouldNotBeNull()

        if (severity == Severity.ERROR)
        {
            checkedForExceptions = true
        }

        return record
    }

    protected fun findLog(code: LoggingCode, severity: Severity = Severity.INFO) =
        getLogRecords().findLast { it.loggingCode == code && it.severity == severity }

    fun verifyNoLogs(code: LoggingCode)
    {
        getLogRecords().any { it.loggingCode == code } shouldBe false
    }

    fun errorLogged(): Boolean
    {
        checkedForExceptions = true
        return getErrorsLogged().isNotEmpty()
    }

    private fun getErrorsLogged() = getLogRecords().filter { it.severity == Severity.ERROR }

    fun getLogRecordsSoFar(): List<LogRecord>
    {
        return logDestination.logRecords.toList()
    }

    fun getLogRecords(): List<LogRecord>
    {
        logger.waitUntilLoggingFinished()
        return logDestination.logRecords.toList()
    }
    fun clearLogs()
    {
        logger.waitUntilLoggingFinished()
        logDestination.clear()
    }
}
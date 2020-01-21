package burlton.dartzee.test.helper

import burlton.dartzee.code.`object`.DartsClient
import burlton.dartzee.code.core.util.Debug
import burlton.dartzee.code.core.util.DialogUtil
import burlton.dartzee.code.db.LocalIdGenerator
import burlton.dartzee.code.utils.DartsDatabaseUtil
import burlton.dartzee.code.utils.InjectedThings
import burlton.dartzee.test.core.helper.TestDebugExtension
import burlton.dartzee.test.core.helper.TestMessageDialogFactory
import burlton.dartzee.test.core.helper.checkedForExceptions
import burlton.dartzee.test.core.helper.exceptionLogged
import burlton.dartzee.test.core.util.TestDebug
import io.kotlintest.shouldBe
import org.apache.derby.jdbc.EmbeddedDriver
import org.junit.After
import org.junit.Before
import java.sql.DriverManager
import javax.swing.UIManager

private const val DATABASE_NAME_TEST = "jdbc:derby:memory:Darts;create=true"
private var doneOneTimeSetup = false

abstract class AbstractTest
{
    private var doneClassSetup = false
    protected val dialogFactory = TestMessageDialogFactory()

    @Before
    fun oneTimeSetup()
    {
        if (!doneOneTimeSetup)
        {
            doOneTimeSetup()
            doneOneTimeSetup = true
        }

        if (!doneClassSetup)
        {
            doClassSetup()
            doneClassSetup = true
        }

        beforeEachTest()
    }

    private fun doOneTimeSetup()
    {
        Debug.initialise(TestDebug.SimpleDebugOutput())
        Debug.sendingEmails = false
        Debug.logToSystemOut = true

        Debug.debugExtension = TestDebugExtension()
        DialogUtil.init(dialogFactory)

        InjectedThings.dartzeeCalculator = FakeDartzeeCalculator()
        InjectedThings.verificationDartboardSize = 50

        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
        DartsClient.derbyDbName = DATABASE_NAME_TEST
        DriverManager.registerDriver(EmbeddedDriver())
        DartsDatabaseUtil.initialiseDatabase()
    }

    open fun doClassSetup()
    {
        Debug.initialise(TestDebug.SimpleDebugOutput())
        Debug.logToSystemOut = true
        DialogUtil.init(dialogFactory)
    }

    open fun beforeEachTest()
    {
        Debug.lastErrorMillis = -1
        Debug.initialise(TestDebug.SimpleDebugOutput())
        dialogFactory.reset()

        LocalIdGenerator.hmLastAssignedIdByTableName.clear()
        DartsDatabaseUtil.getAllEntities().forEach { wipeTable(it.getTableName()) }
    }

    @After
    open fun afterEachTest()
    {
        if (!checkedForExceptions)
        {
            exceptionLogged() shouldBe false
        }

        checkedForExceptions = false
    }
}
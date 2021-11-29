package dartzee.db

import dartzee.helper.*
import dartzee.logging.CODE_BULK_SQL
import dartzee.logging.CODE_SQL
import dartzee.logging.CODE_SQL_EXCEPTION
import dartzee.logging.Severity
import dartzee.utils.InjectedThings.mainDatabase
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldBeSortedWith
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.collections.shouldNotBeSortedWith
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class TestBulkInserter: AbstractTest()
{
    @Test
    fun `Should do nothing if passed no entities to insert`()
    {
        clearLogs()
        BulkInserter.insert()
        getLogRecords().shouldBeEmpty()
    }

    @Test
    fun `Should stack trace and do nothing if any entities are retrievedFromDb`()
    {
        val playerOne = PlayerEntity()
        val playerTwo = insertPlayer()

        BulkInserter.insert(playerOne, playerTwo)

        val log = verifyLog(CODE_SQL_EXCEPTION, Severity.ERROR)
        log.message shouldBe "Attempting to bulk insert Player entities, but some are already in the database"
        getCountFromTable("Player") shouldBe 1
    }

    @Test
    fun `Should log SQLExceptions if something goes wrong inserting entities`()
    {
        val playerOne = factoryPlayer("Pete")
        val playerTwo = factoryPlayer("Leah")

        playerOne.rowId = playerTwo.rowId

        BulkInserter.insert(playerOne, playerTwo)

        val log = verifyLog(CODE_SQL_EXCEPTION, Severity.ERROR)
        log.errorObject?.message shouldContain "duplicate key value"

        getCountFromTable("Player") shouldBe 0
    }

    @Test
    fun `Should insert the right number of rows per INSERT statement`()
    {
        val rows = prepareRows(80)

        checkInsertBatching(rows, 1, 80)
        checkInsertBatching(rows, 20, 4)
        checkInsertBatching(rows, 21, 4)
    }
    private fun checkInsertBatching(rows: List<GameEntity>, rowsPerInsert: Int, expectedNumberOfBatches: Int)
    {
        wipeTable(TableName.Game)
        clearLogs()

        BulkInserter.insert(rows, 1000, rowsPerInsert)

        getLogRecords() shouldHaveSize(expectedNumberOfBatches)
        getCountFromTable(TableName.Game) shouldBe rows.size
    }

    @Test
    fun `Should only run 1 thread for a small number of rows`()
    {
        val rows = prepareRows(50)

        BulkInserter.insert(rows, 50, 1)

        retrieveValues() shouldBeSortedWith{i: Int, j: Int -> i.compareTo(j)}
        getCountFromTable(TableName.Game) shouldBe 50
    }

    @Test
    fun `Should run multi-threaded if required`()
    {
        val rows = prepareRows(50)

        BulkInserter.insert(rows, 5, 1)

        retrieveValues() shouldNotBeSortedWith{i: Int, j: Int -> i.compareTo(j)}
        getCountFromTable(TableName.Game) shouldBe 50
    }

    @Test
    fun `Should temporarily suppress logging for a large number of rows`()
    {
        val rows = prepareRows(501)
        clearLogs()

        BulkInserter.insert(rows, 300, 50)

        getLogRecords().filter { it.loggingCode == CODE_SQL }.shouldBeEmpty()
        val log = getLogRecords().last { it.loggingCode == CODE_BULK_SQL }
        log.message shouldBe "Inserting 501 rows into InsertTest (2 threads @ 50 rows per insert)"
        getCountFromTable("InsertTest") shouldBe 501

        val moreRows = prepareRows(10)
        BulkInserter.insert(moreRows, 300, 50)

        val newLog = getLastLog()
        newLog.loggingCode shouldBe CODE_SQL
        newLog.message shouldContain "INSERT INTO InsertTest VALUES"
    }


    private fun retrieveValues(): List<Int>
    {
        val rows = mutableListOf<Int>()
        mainDatabase.executeQuery("SELECT RowId FROM InsertTest").use{ rs ->
            while (rs.next())
            {
                rows.add(rs.getInt(1))
            }
        }

        return rows
    }

    private fun prepareRows(numberToGenerate: Int) = (1..numberToGenerate).map { _ -> GameEntity().also { it.assignRowId() }}
}
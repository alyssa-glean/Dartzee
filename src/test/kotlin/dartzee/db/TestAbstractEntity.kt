package dartzee.db

import dartzee.core.util.getSqlDateNow
import dartzee.helper.AbstractTest
import dartzee.helper.getCountFromTable
import dartzee.helper.usingInMemoryDatabase
import dartzee.logging.exceptions.WrappedSqlException
import dartzee.utils.Database
import dartzee.utils.InjectedThings.mainDatabase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import java.sql.Timestamp
import java.util.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestAbstractEntity : AbstractTest() {
    @BeforeEach
    fun beforeEach() {
        FakeEntity().createTable()
    }

    @AfterEach
    fun afterEach() {
        mainDatabase.dropUnexpectedTables()
    }

    @Test
    fun `Should throw wrapped SQL exception if an error occurs while preparing insert statement`() {
        mainDatabase.dropTable("TestTable")

        val ex = shouldThrow<WrappedSqlException> { insertFakeEntity() }

        ex.sqlException.message shouldContain "Table/View 'TESTTABLE' does not exist"
        ex.sqlStatement shouldBe "INSERT INTO TestTable VALUES (?, ?, ?, ?)"
        ex.genericStatement shouldBe "INSERT INTO TestTable VALUES (?, ?, ?, ?)"
    }

    @Test
    fun `Should throw wrapped SQL exception with values if an error occurs whilst performing the insert`() {
        val rowId = UUID.randomUUID().toString()

        val dtCreation = Timestamp(100)
        val dtLastUpdate = Timestamp(10000)
        val ex =
            shouldThrow<WrappedSqlException> {
                insertFakeEntity(
                    rowId = rowId,
                    testString = "thisistoolong",
                    dtCreation = dtCreation,
                    dtLastUpdate = dtLastUpdate
                )
            }

        ex.sqlException.message shouldContain "truncation error"
        ex.sqlStatement shouldBe
            "INSERT INTO TestTable VALUES ('$rowId', '$dtCreation', '$dtLastUpdate', 'thisistoolong')"
        ex.genericStatement shouldBe "INSERT INTO TestTable VALUES (?, ?, ?, ?)"
    }

    @Test
    fun `Should throw a wrapped SQL exception with values if an error occurs whilst performing an update`() {
        val entity = insertFakeEntity()
        entity.testString = "thisistoolong"

        val ex = shouldThrow<WrappedSqlException> { entity.saveToDatabase() }

        ex.sqlException.message shouldContain "truncation error"
        ex.sqlStatement shouldBe
            "UPDATE TestTable SET DtCreation='${entity.dtCreation}', DtLastUpdate='${entity.dtLastUpdate}', TestString='thisistoolong' WHERE RowId='${entity.rowId}'"
        ex.genericStatement shouldBe
            "UPDATE TestTable SET DtCreation=?, DtLastUpdate=?, TestString=? WHERE RowId=?"
    }

    @Test
    fun `Should throw a wrapped SQL exception on retrieve`() {
        val dao = FakeEntity()

        val ex = shouldThrow<WrappedSqlException> { dao.retrieveEntities("butterfingers") }

        ex.sqlException.message shouldContain "BUTTERFINGERS"
        ex.sqlStatement shouldBe
            "SELECT RowId, DtCreation, DtLastUpdate, TestString FROM TestTable WHERE butterfingers"
    }

    @Test
    fun `Should retrieve all entities if passed a null date`() {
        val e1 = insertFakeEntity()
        val e2 = insertFakeEntity()

        val retrieved = FakeEntity().retrieveModifiedSince(null).map { it.rowId }
        retrieved.shouldContainExactly(e1.rowId, e2.rowId)
    }

    @Test
    fun `Should retrieve all entities modified since a particular date`() {
        insertFakeEntity(dtLastUpdate = Timestamp(500))
        insertFakeEntity(dtLastUpdate = Timestamp(1000))
        val e3 = insertFakeEntity(dtLastUpdate = Timestamp(1001))
        val e4 = insertFakeEntity(dtLastUpdate = Timestamp(2000))

        val retrieved = FakeEntity().retrieveModifiedSince(Timestamp(1000)).map { it.rowId }
        retrieved.shouldContainExactly(e3.rowId, e4.rowId)
    }

    @Test
    fun `Should sort by dtCreation`() {
        val e1 = insertFakeEntity(dtCreation = Timestamp(500))
        val e3 = insertFakeEntity(dtCreation = Timestamp(1000))
        val e2 = insertFakeEntity(dtCreation = Timestamp(750))

        val retrieved = FakeEntity().retrieveModifiedSince(null).map { it.rowId }
        retrieved.shouldContainExactly(e1.rowId, e2.rowId, e3.rowId)
    }

    @Test
    fun `Should insert into other database if row does not already exist`() {
        usingInMemoryDatabase { otherDatabase ->
            FakeEntity(otherDatabase).createTable()

            val entity = insertFakeEntity(testString = "carrot", dtLastUpdate = Timestamp(500))
            getCountFromTable("TestTable", mainDatabase) shouldBe 1
            getCountFromTable("TestTable", otherDatabase) shouldBe 0

            entity.mergeIntoDatabase(otherDatabase)
            getCountFromTable("TestTable", otherDatabase) shouldBe 1

            val otherEntity = FakeEntity(otherDatabase).retrieveForId(entity.rowId)!!
            otherEntity.testString shouldBe "carrot"
            otherEntity.dtLastUpdate shouldBe Timestamp(500)
        }
    }

    @Test
    fun `Should update other database if dtLastUpdate is more recent`() {
        usingInMemoryDatabase { otherDatabase ->
            FakeEntity(otherDatabase).createTable()

            val localEntity =
                insertFakeEntity(
                    testString = "carrot",
                    dtLastUpdate = Timestamp(500),
                    database = mainDatabase
                )
            insertFakeEntity(
                rowId = localEntity.rowId,
                testString = "banana",
                dtLastUpdate = Timestamp(499),
                database = otherDatabase
            )

            localEntity.mergeIntoDatabase(otherDatabase)
            getCountFromTable("TestTable", otherDatabase) shouldBe 1

            val otherEntity = FakeEntity(otherDatabase).retrieveForId(localEntity.rowId)!!
            otherEntity.testString shouldBe "carrot"
            otherEntity.dtLastUpdate shouldBe Timestamp(500)
        }
    }

    @Test
    fun `Should not update other database if dtLastUpdate is older`() {
        usingInMemoryDatabase { otherDatabase ->
            FakeEntity(otherDatabase).createTable()
            val localEntity =
                insertFakeEntity(
                    testString = "carrot",
                    dtLastUpdate = Timestamp(500),
                    database = mainDatabase
                )
            insertFakeEntity(
                rowId = localEntity.rowId,
                testString = "banana",
                dtLastUpdate = Timestamp(501),
                database = otherDatabase
            )

            localEntity.mergeIntoDatabase(otherDatabase)
            getCountFromTable("TestTable", otherDatabase) shouldBe 1

            val otherEntity = FakeEntity(otherDatabase).retrieveForId(localEntity.rowId)!!
            otherEntity.testString shouldBe "banana"
            otherEntity.dtLastUpdate shouldBe Timestamp(501)
        }
    }

    private fun insertFakeEntity(
        rowId: String = UUID.randomUUID().toString(),
        testString: String = "",
        dtCreation: Timestamp = getSqlDateNow(),
        dtLastUpdate: Timestamp = getSqlDateNow(),
        database: Database = mainDatabase
    ): FakeEntity {
        val entity = FakeEntity(database)
        entity.rowId = rowId
        entity.dtCreation = dtCreation
        entity.testString = testString
        entity.saveToDatabase(dtLastUpdate)
        return entity
    }
}

class FakeEntity(database: Database = mainDatabase) : AbstractEntity<FakeEntity>(database) {
    var testString = ""

    override fun getTableName(): EntityName {
        val result = mockk<EntityName>(relaxed = true)
        every { result.name } returns "TestTable"
        every { result.toString() } returns "TestTable"
        return result
    }

    override fun getCreateTableSqlSpecific() = "TestString VARCHAR(10) NOT NULL"
}

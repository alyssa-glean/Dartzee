package dartzee.db

import dartzee.core.helper.verifyNotCalled
import dartzee.helper.AbstractTest
import dartzee.helper.makeInMemoryDatabase
import dartzee.utils.Database
import dartzee.utils.DatabaseMigrations
import dartzee.utils.InjectedThings
import io.kotlintest.matchers.collections.shouldContainExactly
import io.mockk.mockk
import org.junit.Test

class TestDatabaseMigrations: AbstractTest()
{
    val mainDb = InjectedThings.mainDatabase

    override fun afterEachTest()
    {
        super.afterEachTest()
        InjectedThings.mainDatabase = mainDb
    }

    @Test
    fun `Conversions map should not have gaps`()
    {
        val supportedVersions = DatabaseMigrations.getConversionsMap().keys
        val min = supportedVersions.min()!!
        val max = supportedVersions.max()!!

        supportedVersions.shouldContainExactly((min..max).toSet())
    }

    @Test
    fun `Conversions should all run on the specified database`()
    {
        val mainDbMock = mockk<Database>(relaxed = true)
        InjectedThings.mainDatabase = mainDbMock

        val dbToRunOn = makeInMemoryDatabase()
        DatabaseMigrator(emptyMap()).migrateToLatest(dbToRunOn, "Test")

        val conversionFns = DatabaseMigrations.getConversionsMap().values.flatten()
        conversionFns.forEach {
            it(dbToRunOn)
        }

        //Conversions will likely fail since we've not set them up
        errorLogged()

        verifyNotCalled { mainDbMock.borrowConnection() }
    }
}
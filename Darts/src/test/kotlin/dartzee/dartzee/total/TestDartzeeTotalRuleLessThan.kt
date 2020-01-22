package dartzee.dartzee.total

import dartzee.dartzee.total.DartzeeTotalRuleLessThan
import dartzee.dartzee.AbstractDartzeeRuleTest
import io.kotlintest.shouldBe
import org.junit.Test

class TestDartzeeTotalRuleLessThan: AbstractDartzeeRuleTest<DartzeeTotalRuleLessThan>()
{
    override fun factory() = DartzeeTotalRuleLessThan()

    @Test
    fun `Total validation`()
    {
        val rule = DartzeeTotalRuleLessThan()
        rule.target = 55

        rule.isValidTotal(54) shouldBe true
        rule.isValidTotal(55) shouldBe false
        rule.isValidTotal(56) shouldBe false
    }

    @Test
    fun `Rule description`()
    {
        val rule = DartzeeTotalRuleLessThan()
        rule.target = 25

        rule.getDescription() shouldBe "< 25"
    }
}
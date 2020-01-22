package dartzee.screen.dartzee

import dartzee.core.helper.verifyNotCalled
import dartzee.bean.DartzeeDartRuleSelector
import dartzee.dartzee.DartzeeCalculator
import dartzee.dartzee.dart.*
import dartzee.dartzee.total.DartzeeTotalRuleEqualTo
import dartzee.dartzee.total.DartzeeTotalRuleOdd
import dartzee.dartzee.total.DartzeeTotalRulePrime
import dartzee.screen.dartzee.DartzeeRuleCreationDialog
import dartzee.screen.dartzee.DartzeeRuleVerificationPanel
import dartzee.utils.InjectedThings
import dartzee.flushEdt
import dartzee.helper.*
import dartzee.core.bean.selectByClass
import dartzee.core.util.getAllChildComponentsForType
import dartzee.core.helper.makeActionEvent
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldNotContain
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class TestDartzeeRuleAmendment: AbstractTest()
{
    @Test
    fun `Should adjust the dialog title appropriately`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.amendRule(makeDartzeeRuleDto())

        dlg.title shouldBe "Amend Dartzee Rule"
    }

    @Test
    fun `Should populate from a 'no darts' rule correctly`()
    {
        val rule = makeDartzeeRuleDto()

        val dlg = DartzeeRuleCreationDialog()
        dlg.amendRule(rule)

        dlg.rdbtnNoDarts.isSelected shouldBe true
    }

    @Test
    fun `Should populate from an 'at least one' rule correctly`()
    {
        val rule = makeDartzeeRuleDto(makeScoreRule(12))

        val dlg = DartzeeRuleCreationDialog()
        dlg.amendRule(rule)

        dlg.rdbtnAtLeastOne.isSelected shouldBe true
        val dartRule = dlg.targetSelector.getSelection()

        val totalRule = dartRule as DartzeeDartRuleScore
        totalRule.score shouldBe 12
    }

    @Test
    fun `Should populate from a 'three darts' rule correctly`()
    {
        val rule = makeDartzeeRuleDto(makeScoreRule(13), makeColourRule(red = true, green = true), DartzeeDartRuleOdd(), inOrder = false)

        val dlg = DartzeeRuleCreationDialog()
        dlg.amendRule(rule)

        dlg.rdbtnAllDarts.isSelected shouldBe true
        dlg.cbInOrder.isSelected shouldBe false

        val scoreRule = dlg.dartOneSelector.getSelection() as DartzeeDartRuleScore
        scoreRule.score shouldBe 13

        val colourRule = dlg.dartTwoSelector.getSelection() as DartzeeDartRuleColour
        colourRule.red shouldBe true
        colourRule.green shouldBe true

        val thirdRule = dlg.dartThreeSelector.getSelection()
        thirdRule.shouldBeInstanceOf<DartzeeDartRuleOdd>()
    }

    @Test
    fun `Should populate an in order rule correctly`()
    {
        val rule = makeDartzeeRuleDto(DartzeeDartRuleEven(), DartzeeDartRuleOdd(), DartzeeDartRuleEven(), inOrder = true)

        val dlg = DartzeeRuleCreationDialog()
        dlg.amendRule(rule)

        dlg.rdbtnAllDarts.isSelected shouldBe true
        dlg.cbInOrder.isSelected shouldBe true
    }

    @Test
    fun `Should populate no total rule correctly`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.amendRule(makeDartzeeRuleDto())

        dlg.totalSelector.cbDesc.isSelected shouldBe false
    }

    @Test
    fun `Should populate a total rule correctly`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.amendRule(makeDartzeeRuleDto(totalRule = makeTotalScoreRule<DartzeeTotalRuleEqualTo>(48)))

        dlg.totalSelector.cbDesc.isSelected shouldBe true
        val totalRule = dlg.totalSelector.getSelection() as DartzeeTotalRuleEqualTo
        totalRule.target shouldBe 48
    }

    @Test
    fun `Should update the rule description`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.amendRule(makeDartzeeRuleDto(DartzeeDartRuleOuter()))

        flushEdt()

        dlg.tfName.text shouldBe "Score Outers"
    }

    @Test
    fun `Should populate allow misses correctly`()
    {
        val dlg = DartzeeRuleCreationDialog()
        val allowMisses = makeDartzeeRuleDto(allowMisses = true)
        val disallowMisses = makeDartzeeRuleDto(allowMisses = false)

        dlg.amendRule(allowMisses)
        dlg.cbAllowMisses.isSelected shouldBe true

        dlg.amendRule(disallowMisses)
        dlg.cbAllowMisses.isSelected shouldBe false
    }

    @Test
    fun `Should replace the darzeeRule with new values if Ok is pressed`()
    {
        val dlg = DartzeeRuleCreationDialog()

        val rule = makeDartzeeRuleDto()
        dlg.amendRule(rule)

        dlg.rdbtnAtLeastOne.doClick()
        dlg.targetSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleOdd>()
        dlg.btnOk.doClick()

        val updatedRule = dlg.dartzeeRule!!
        updatedRule shouldNotBe rule
        updatedRule.dart1Rule.shouldBeInstanceOf<DartzeeDartRuleOdd>()
    }

    @Test
    fun `Should leave the old dartzeeRule alone if cancelled`()
    {
        val dlg = DartzeeRuleCreationDialog()

        val rule = makeDartzeeRuleDto()
        dlg.amendRule(rule)

        dlg.rdbtnAtLeastOne.doClick()
        dlg.targetSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleOdd>()
        dlg.btnCancel.doClick()

        val updatedRule = dlg.dartzeeRule!!
        updatedRule shouldBe rule
    }
}

class TestDartzeeRuleCreationDialogValidation: AbstractTest()
{
    override fun afterEachTest()
    {
        InjectedThings.dartzeeCalculator = FakeDartzeeCalculator()

        super.afterEachTest()
    }

    @Test
    fun `Should validate all three dart selectors for an all darts rule`()
    {
        val dlg = spyk<DartzeeRuleCreationDialog>()

        dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleColour>()
        dlg.btnOk.doClick()
        dialogFactory.errorsShown.shouldContainExactly("Dart 1: You must select at least one colour.")
        dlg.dartzeeRule shouldBe null
        verifyNotCalled { dlg.dispose() }

        dialogFactory.errorsShown.clear()
        dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleAny>()
        dlg.dartTwoSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleColour>()
        dlg.btnOk.doClick()
        dialogFactory.errorsShown.shouldContainExactly("Dart 2: You must select at least one colour.")
        dlg.dartzeeRule shouldBe null
        verifyNotCalled { dlg.dispose() }

        dialogFactory.errorsShown.clear()
        dlg.dartTwoSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleAny>()
        dlg.dartThreeSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleColour>()
        dlg.btnOk.doClick()
        dialogFactory.errorsShown.shouldContainExactly("Dart 3: You must select at least one colour.")
        dlg.dartzeeRule shouldBe null
        verifyNotCalled { dlg.dispose() }
    }

    @Test
    fun `Should validate the target selector for an 'at least one' dart rule`()
    {
        val dlg = spyk<DartzeeRuleCreationDialog>()
        dlg.rdbtnAtLeastOne.doClick()

        dlg.targetSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleColour>()
        dlg.btnOk.doClick()

        dialogFactory.errorsShown.shouldContainExactly("Target: You must select at least one colour.")
        dlg.dartzeeRule shouldBe null
        verifyNotCalled { dlg.dispose() }
    }

    @Test
    fun `Should detect impossible rules and not return a rule`()
    {
        InjectedThings.dartzeeCalculator = DartzeeCalculator()

        val dlg = spyk<DartzeeRuleCreationDialog>()
        dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleEven>()
        dlg.dartTwoSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleEven>()
        dlg.dartThreeSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleEven>()

        dlg.totalSelector.cbDesc.doClick()
        dlg.totalSelector.comboBoxRuleType.selectByClass<DartzeeTotalRuleOdd>()

        dlg.btnOk.doClick()

        dialogFactory.errorsShown.shouldContainExactly("This rule is impossible!")
        dlg.dartzeeRule shouldBe null
        verifyNotCalled { dlg.dispose() }
    }

    @Test
    fun `Should dispose if valid`()
    {
        val dlg = spyk<DartzeeRuleCreationDialog>()
        dlg.btnOk.doClick()

        dialogFactory.errorsShown.shouldBeEmpty()
        verify { dlg.dispose() }
    }
}

class TestDartzeeRuleCreationDialogDtoPopulation : AbstractTest()
{
    @Test
    fun `Should populate an 'at least one' rule correctly`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.rdbtnAtLeastOne.doClick()
        dlg.targetSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleOdd>()
        dlg.btnOk.doClick()

        val rule = dlg.dartzeeRule!!

        rule.dart1Rule!!.toDbString() shouldBe DartzeeDartRuleOdd().toDbString()
        rule.dart2Rule shouldBe null
        rule.dart3Rule shouldBe null
        rule.inOrder shouldBe false
    }

    @Test
    fun `Should populate an 'all darts' rule correctly`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.rdbtnAllDarts.isSelected = true
        dlg.cbInOrder.isSelected = false

        dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleInner>()
        dlg.dartTwoSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleOuter>()
        dlg.dartThreeSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleOdd>()
        dlg.btnOk.doClick()

        val rule = dlg.dartzeeRule!!

        rule.dart1Rule!!.toDbString() shouldBe DartzeeDartRuleInner().toDbString()
        rule.dart2Rule!!.toDbString() shouldBe DartzeeDartRuleOuter().toDbString()
        rule.dart3Rule!!.toDbString() shouldBe DartzeeDartRuleOdd().toDbString()
        rule.inOrder shouldBe false
    }

    @Test
    fun `Should populate in order correctly when checked`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.btnOk.doClick()

        val rule = dlg.dartzeeRule!!
        rule.inOrder shouldBe true
    }

    @Test
    fun `Should populate allowMisses correctly when checked`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.cbAllowMisses.isSelected = true

        dlg.btnOk.doClick()

        val rule = dlg.dartzeeRule!!
        rule.allowMisses shouldBe true
    }

    @Test
    fun `Should populate a 'no darts' rule correctly`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.rdbtnNoDarts.doClick()
        dlg.btnOk.doClick()

        val rule = dlg.dartzeeRule!!
        rule.dart1Rule shouldBe null
    }

    @Test
    fun `Should populate the total rule correctly`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.totalSelector.cbDesc.doClick()
        dlg.totalSelector.comboBoxRuleType.selectByClass<DartzeeTotalRulePrime>()
        dlg.btnOk.doClick()

        val rule = dlg.dartzeeRule!!
        rule.totalRule!!.toDbString() shouldBe DartzeeTotalRulePrime().toDbString()
    }
}

class TestDartzeeRuleCreationDialogInteraction : AbstractTest()
{
    override fun afterEachTest()
    {
        InjectedThings.dartzeeCalculator = FakeDartzeeCalculator()

        super.afterEachTest()
    }

    @Test
    fun `Should not return a rule when cancelled`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.btnCancel.doClick()
        dlg.dartzeeRule shouldBe null
    }

    @Test
    fun `Should toggle the rule selectors based on radio selection`()
    {
        val dlg = DartzeeRuleCreationDialog()

        var children = getAllChildComponentsForType(dlg, DartzeeDartRuleSelector::class.java)
        children.shouldContainExactlyInAnyOrder(dlg.dartOneSelector, dlg.dartTwoSelector, dlg.dartThreeSelector)
        children.shouldNotContain(dlg.targetSelector)

        dlg.rdbtnAtLeastOne.doClick()
        children = getAllChildComponentsForType(dlg, DartzeeDartRuleSelector::class.java)
        children.shouldContainExactly(dlg.targetSelector)

        dlg.rdbtnAllDarts.doClick()
        children = getAllChildComponentsForType(dlg, DartzeeDartRuleSelector::class.java)
        children.shouldContainExactlyInAnyOrder(dlg.dartOneSelector, dlg.dartTwoSelector, dlg.dartThreeSelector)
        children.shouldNotContain(dlg.targetSelector)

        dlg.rdbtnNoDarts.doClick()
        children = getAllChildComponentsForType(dlg, DartzeeDartRuleSelector::class.java)
        children.shouldBeEmpty()
    }

    @Test
    fun `Should update the verification panel on initialisation`()
    {
        val verificationPanel = mockk<DartzeeRuleVerificationPanel>(relaxed = true)

        val dlg = DartzeeRuleCreationDialog(verificationPanel)
        flushEdt()

        verify { verificationPanel.updateRule(dlg.constructRuleFromComponents()) }
    }

    @Test
    fun `Should update the verification panel when things change`()
    {
        val verificationPanel = mockk<DartzeeRuleVerificationPanel>(relaxed = true)

        val dlg = DartzeeRuleCreationDialog(verificationPanel)
        flushEdt()
        clearAllMocks()

        dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleOuter>()
        flushEdt()

        verify { verificationPanel.updateRule(dlg.constructRuleFromComponents()) }
    }

    @Test
    fun `Should update the rule description when combo boxes change`()
    {
        val dlg = DartzeeRuleCreationDialog()
        dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleInner>()
        dlg.dartTwoSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleOuter>()
        dlg.dartThreeSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleOdd>()

        flushEdt()

        dlg.tfName.text shouldBe "Inner → Outer → Odd"

        dlg.totalSelector.cbDesc.doClick()
        dlg.totalSelector.comboBoxRuleType.selectByClass<DartzeeTotalRuleOdd>()

        flushEdt()

        dlg.tfName.text shouldBe "Inner → Outer → Odd, Total is odd"
    }

    @Test
    fun `Should update rule difficulty when the rule changes`()
    {
        //Need a real calculator for this to actually change
        InjectedThings.dartzeeCalculator = DartzeeCalculator()

        val dlg = DartzeeRuleCreationDialog()
        flushEdt()
        dlg.lblDifficulty.text shouldBe "Very Easy"

        dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleCustom>()
        flushEdt()

        dlg.lblDifficulty.text shouldBe "Impossible"
    }

    @Test
    fun `Should update the rule description when score config changes`()
    {
        val dlg = DartzeeRuleCreationDialog()

        val scoreRule = dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleScore>()!!
        flushEdt()

        dlg.tfName.text shouldBe "20 → Any → Any"

        scoreRule.spinner.value = 15
        flushEdt()

        dlg.tfName.text shouldBe "15 → Any → Any"
    }

    @Test
    fun `Should update the rule description when custom config changes`()
    {
        val dlg = DartzeeRuleCreationDialog()

        val customRule = dlg.dartOneSelector.comboBoxRuleType.selectByClass<DartzeeDartRuleCustom>()!!
        flushEdt()

        dlg.tfName.text shouldBe "Custom → Any → Any"

        customRule.tfName.text = "Foo"
        customRule.actionPerformed(makeActionEvent(customRule.tfName))
        flushEdt()

        dlg.tfName.text shouldBe "Foo → Any → Any"
    }
}
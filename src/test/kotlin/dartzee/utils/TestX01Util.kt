package dartzee.utils

import dartzee.`object`.Dart
import dartzee.ai.AbstractDartsModel
import dartzee.helper.AbstractTest
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import org.junit.Test

class TestX01Util: AbstractTest()
{
    @Test
    fun testIsBust()
    {
        isBust(-5, Dart(2, 2)).shouldBeTrue()
        isBust(-5, Dart(2, 2)).shouldBeTrue()
        isBust(-5, Dart(2, 0)).shouldBeTrue()
        isBust(-8, Dart(4, 2)).shouldBeTrue()

        isBust(0, Dart(10, 1)).shouldBeTrue()
        isBust(0, Dart(20, 3)).shouldBeTrue()

        isBust(1, Dart(20, 2)).shouldBeTrue()

        isBust(0, Dart(20, 2)).shouldBeFalse()
        isBust(0, Dart(25, 2)).shouldBeFalse()

        isBust(20, Dart(20, 2)).shouldBeFalse()
        isBust(20, Dart(20, 1)).shouldBeFalse()
    }

    @Test
    fun testShouldStopForMercyRule()
    {
        val model = AbstractDartsModel.factoryForType(AbstractDartsModel.TYPE_NORMAL_DISTRIBUTION)!!
        model.mercyThreshold = 19

        shouldStopForMercyRule(model, 19, 16).shouldBeFalse()
        shouldStopForMercyRule(model, 17, 16).shouldBeTrue()
        shouldStopForMercyRule(model, 15, 8).shouldBeTrue()
        shouldStopForMercyRule(model, 16, 8).shouldBeFalse()
        shouldStopForMercyRule(model, 17, 13).shouldBeFalse()
        shouldStopForMercyRule(model, 17, 17).shouldBeFalse()

        model.mercyThreshold = -1

        shouldStopForMercyRule(model, 19, 16).shouldBeFalse()
        shouldStopForMercyRule(model, 17, 16).shouldBeFalse()
        shouldStopForMercyRule(model, 15, 8).shouldBeFalse()
        shouldStopForMercyRule(model, 16, 8).shouldBeFalse()
        shouldStopForMercyRule(model, 17, 13).shouldBeFalse()
        shouldStopForMercyRule(model, 17, 17).shouldBeFalse()
    }

    @Test
    fun testIsCheckoutDart()
    {
        assertCheckout(52, false)
        assertCheckout(50, true)
        assertCheckout(45, false)
        assertCheckout(42, false)
        assertCheckout(41, false)
        assertCheckout(40, true)
        assertCheckout(35, false)
        assertCheckout(2, true)
    }

    private fun assertCheckout(startingScore: Int, expected: Boolean)
    {
        val drt = Dart(20, 2)
        drt.startingScore = startingScore

        isCheckoutDart(drt) shouldBe expected
    }

    @Test
    fun testIsFinishRound()
    {
        val d = Dart(20, 1)
        d.startingScore = 20

        val round = mutableListOf(Dart(2, 1), d)

        isFinishRound(round).shouldBeFalse()
        d.multiplier = 2
        isFinishRound(round).shouldBeFalse()
        d.score = 10
        isFinishRound(round).shouldBeTrue()
    }

    @Test
    fun testGetScoringDartsNull()
    {
        val result = getScoringDarts(null, 20)

        result.shouldBeEmpty()
    }

    @Test
    fun testGetScoringDarts()
    {
        val d1 = Dart(20, 1)
        val d2 = Dart(20, 1)
        val d3 = Dart(20, 1)
        d1.startingScore = 51
        d2.startingScore = 50
        d3.startingScore = 49

        val list = mutableListOf(d1, d2, d3)

        val result = getScoringDarts(list, 50)
        result.shouldContainExactly(d1)
    }

    @Test
    fun testCalculateThreeDartAverage()
    {
        val d1 = Dart(20, 1)
        val d2 = Dart(20, 2)
        val d3 = Dart(10, 0)
        val d4 = Dart(5, 3)

        d1.startingScore = 100
        d2.startingScore = 100
        d3.startingScore = 80
        d4.startingScore = 100

        val list = mutableListOf(d1, d2, d3, d4)
        val result = calculateThreeDartAverage(list, 70)
        val resultTwo = calculateThreeDartAverage(list, 90) //The miss should be excluded
        val resultThree = calculateThreeDartAverage(list, 200) //Test an empty list

        result shouldBe 56.25
        resultTwo shouldBe 75.0
        resultThree shouldBe -1.0
    }

    @Test
    fun testSumScore()
    {
        val d1 = Dart(20, 2)
        val d2 = Dart(13, 0)
        val d3 = Dart(11, 1)

        val list = mutableListOf(d1, d2, d3)

        sumScore(list) shouldBe 51
    }

    @Test
    fun testIsShanghai()
    {
        val tooShort = mutableListOf(Dart(20, 3), Dart(20, 3))
        val miss = mutableListOf(Dart(20, 3), Dart(20, 3), Dart(20, 0))
        val wrongSum = mutableListOf(Dart(20, 1), Dart(20, 3), Dart(20, 3))
        val allDoubles = mutableListOf(Dart(20, 2), Dart(20, 2), Dart(20, 2))
        val correct = mutableListOf(Dart(20, 1), Dart(20, 2), Dart(20, 3))
        val correctDifferentOrder = mutableListOf(Dart(20, 2), Dart(20, 3), Dart(20, 1))

        isShanghai(tooShort).shouldBeFalse()
        isShanghai(miss).shouldBeFalse()
        isShanghai(wrongSum).shouldBeFalse()
        isShanghai(allDoubles).shouldBeFalse()

        isShanghai(correct).shouldBeTrue()
        isShanghai(correctDifferentOrder).shouldBeTrue()
    }

    @Test
    fun testGetSortedDartStr()
    {
        val listOne = mutableListOf(Dart(2, 3), Dart(3, 2), Dart(20, 1))
        val listTwo = mutableListOf(Dart(1, 1), Dart(7, 1), Dart(5, 1))
        val listThree = mutableListOf(Dart(20, 3), Dart(20, 3), Dart(20, 3))
        val listFour = mutableListOf(Dart(25, 2), Dart(20, 3), Dart(20, 0))

        getSortedDartStr(listOne) shouldBe "20, T2, D3"
        getSortedDartStr(listTwo) shouldBe "7, 5, 1"
        getSortedDartStr(listThree) shouldBe "T20, T20, T20"
        getSortedDartStr(listFour) shouldBe "T20, D25, 0"
    }

    @Test
    fun testIsNearMissDouble()
    {
        val nonCheckoutDart = Dart(16, 2)
        nonCheckoutDart.startingScore = 48

        val hitBullseye = Dart(25, 2)
        hitBullseye.startingScore = 50

        val missedBullseye = Dart(19, 1)
        missedBullseye.startingScore = 50

        val nearMissBullseye = Dart(25, 1)
        nearMissBullseye.startingScore = 50

        val nonAdjacentDoubleTop = Dart(12, 2)
        nonAdjacentDoubleTop.startingScore = 40

        val nonDoubleDoubleTop = Dart(5, 1)
        nonDoubleDoubleTop.startingScore = 40

        val nearMissDoubleTop = Dart(5, 2)
        nearMissDoubleTop.startingScore = 40

        isNearMissDouble(nonCheckoutDart).shouldBeFalse()
        isNearMissDouble(hitBullseye).shouldBeFalse()
        isNearMissDouble(missedBullseye).shouldBeFalse()
        isNearMissDouble(nearMissBullseye).shouldBeTrue()

        isNearMissDouble(nonAdjacentDoubleTop).shouldBeFalse()
        isNearMissDouble(nonDoubleDoubleTop).shouldBeFalse()
        isNearMissDouble(nearMissDoubleTop).shouldBeTrue()
    }
}
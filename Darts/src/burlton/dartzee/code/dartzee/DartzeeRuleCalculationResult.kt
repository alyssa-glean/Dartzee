package burlton.dartzee.code.dartzee

import burlton.dartzee.code.core.util.*
import burlton.dartzee.code.`object`.DartboardSegment
import burlton.dartzee.code.utils.DartsColour
import kotlin.math.sqrt

enum class DartzeeRuleDifficulty(val desc: String)
{
    IMPOSSIBLE("Impossible"),
    INSANE("Insane"),
    VERY_HARD("Very Hard"),
    HARD("Hard"),
    MODERATE("Moderate"),
    EASY("Easy"),
    VERY_EASY("Very Easy")
}

val INVALID_CALCULATION_RESULT = DartzeeRuleCalculationResult(listOf(), 0, 0, 0.0, 1.0)

data class DartzeeRuleCalculationResult(val validSegments: List<DartboardSegment>,
                                        val validCombinations: Int,
                                        val allCombinations: Int,
                                        val validCombinationProbability: Double,
                                        val allCombinationsProbability: Double)
{
    val percentage = MathsUtil.getPercentage(validCombinationProbability, allCombinationsProbability)

    fun getCombinationsDesc() = "$validCombinations combinations (success%: $percentage%)"

    fun getDifficultyDesc() = getDifficulty().desc

    fun getForeground() = DartsColour.getProportionalColourRedToGreen(sqrt(percentage), 10, 1.0)
    fun getBackground() = DartsColour.getProportionalColourRedToGreen(sqrt(percentage), 10, 0.5)

    private fun getDifficulty() = when
    {
        validCombinations == 0 -> DartzeeRuleDifficulty.IMPOSSIBLE
        percentage > 40 -> DartzeeRuleDifficulty.VERY_EASY
        percentage > 25 -> DartzeeRuleDifficulty.EASY
        percentage > 10 -> DartzeeRuleDifficulty.MODERATE
        percentage > 5 -> DartzeeRuleDifficulty.HARD
        percentage > 1 -> DartzeeRuleDifficulty.VERY_HARD
        else -> DartzeeRuleDifficulty.INSANE
    }

    fun toDbString(): String
    {
        val doc = XmlUtil.factoryNewDocument()
        val root = doc.createRootElement("CalculationResult")

        root.setAttributeAny("ValidCombinations", validCombinations)
        root.setAttributeAny("AllCombinations", allCombinations)
        root.setAttributeAny("ValidCombinationProbability", validCombinationProbability)
        root.setAttributeAny("AllCombinationsProbability", allCombinationsProbability)
        root.writeList(validSegments.map { it.scoreAndType }, "ValidSegments")

        return doc.toXmlString()
    }

    companion object
    {
        fun fromDbString(dbString: String): DartzeeRuleCalculationResult
        {
            val doc = dbString.toXmlDoc()!!
            val root = doc.documentElement

            val validCombinations = root.getAttributeInt("ValidCombinations")
            val allCombinations = root.getAttributeInt("AllCombinations")
            val validCombinationProbability = root.getAttributeDouble("ValidCombinationProbability")
            val allCombinationsProbability = root.getAttributeDouble("AllCombinationsProbability")
            val validSegments = root.readList("ValidSegments").map{ DartboardSegment(it) }

            return DartzeeRuleCalculationResult(validSegments, validCombinations, allCombinations, validCombinationProbability, allCombinationsProbability)
        }
    }
}
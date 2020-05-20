package dartzee.`object`

import dartzee.core.util.DateStatics
import dartzee.db.CLOCK_TYPE_DOUBLES
import dartzee.db.CLOCK_TYPE_TREBLES
import java.awt.Point
import java.sql.Timestamp

open class Dart constructor(
        var score: Int,
        var multiplier: Int,
        var pt: Point? = null,
        var segmentType: SegmentType = SegmentType.MISS)
{
    var ordinal = -1

    //What the player's "score" was before throwing this dart.
    //Means what you'd think for X01
    //For Round the Clock, this'll be what they were to aim for
    var startingScore = -1

    //Never set on the DB. Used for in-game stats, and is just set to the round number.
    private var golfHole = -1
    var participantId: String = ""
    var gameId: String = ""
    var dtThrown: Timestamp = DateStatics.END_OF_TIME

    /**
     * Helpers
     */
    fun isDouble() = multiplier == 2
    fun isTriple() = multiplier == 3
    fun getTotal() = score * multiplier
    fun getGolfScore() = getGolfScore(golfHole)
    fun getX() = pt?.x
    fun getY() = pt?.y

    fun getHitScore(): Int
    {
        return when(multiplier)
        {
            0 -> 0
            else -> score
        }
    }

    fun getRendered(): String
    {
        if (multiplier == 0)
        {
            return "0"
        }

        var ret = ""
        if (isDouble())
        {
            ret += "D"
        }
        else if (isTriple())
        {
            ret += "T"
        }

        ret += score

        return ret
    }

    fun getSegmentTypeToAimAt(): SegmentType
    {
        return when (multiplier)
        {
            1 -> SegmentType.OUTER_SINGLE
            2 -> SegmentType.DOUBLE
            else -> SegmentType.TREBLE
        }
    }

    fun getGolfScore(target: Int): Int
    {
        return if (score != target)
        {
            5
        } else segmentType.getGolfScore()

    }

    fun setGolfHole(hole: Int)
    {
        this.golfHole = hole
    }

    override fun hashCode(): Int
    {
        val prime = 31
        var result = 1
        result = prime * result + multiplier
        result = prime * result + ordinal
        result = prime * result + score
        return result
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is Dart)
            return false
        if (multiplier != other.multiplier)
            return false
        if (ordinal != other.ordinal)
            return false
        return (score == other.score)
    }

    override fun toString() = getRendered()

    fun hitClockTarget(clockType: String?): Boolean
    {
        return when (clockType)
        {
            CLOCK_TYPE_DOUBLES -> score == startingScore && isDouble()
            CLOCK_TYPE_TREBLES -> score == startingScore && isTriple()
            else -> score == startingScore && multiplier > 0
        }
    }

    operator fun compareTo(other: Dart): Int
    {
        //If there's a strict inequality in total, then it's simple
        if (getTotal() > other.getTotal())
        {
            return 1
        }

        if (getTotal() < other.getTotal())
        {
            return -1
        }

        //Totals are equal. So now look at the multiplier.
        //I.e. T12 > D18 even though the totals are both 36.
        if (multiplier > other.multiplier)
        {
            return 1
        }

        return if (other.multiplier > multiplier)
        {
            -1
        }
        //Same total and same multiplier, so must be an equivalent dart
        else 0
    }
}

class DartNotThrown : Dart(-1, -1)

/**
 * Static methods
 */
fun factorySingle(score: Int): Dart
{
    return Dart(score, 1)
}

fun factoryDouble(score: Int): Dart
{
    return Dart(score, 2)
}
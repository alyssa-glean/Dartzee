package burlton.dartzee.code.achievements

import burlton.desktopcore.code.util.Debug
import burlton.dartzee.code.db.AchievementEntity
import burlton.dartzee.code.db.PlayerEntity
import burlton.dartzee.code.utils.DartsColour
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.dartzee.code.utils.ResourceCache
import burlton.dartzee.code.core.bean.paint
import burlton.desktopcore.code.util.DateStatics.Companion.START_OF_TIME
import burlton.desktopcore.code.util.formatAsDate
import java.awt.Color
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.table.DefaultTableModel

abstract class AbstractAchievement
{
    abstract val name : String
    abstract val desc : String
    abstract val achievementRef : Int
    abstract val redThreshold : Int
    abstract val orangeThreshold : Int
    abstract val yellowThreshold : Int
    abstract val greenThreshold : Int
    abstract val blueThreshold : Int
    abstract val pinkThreshold : Int
    abstract val maxValue : Int
    abstract val gameType: Int

    var attainedValue = -1
    var gameIdEarned = ""
    var localGameIdEarned = -1L
    var dtLatestUpdate = START_OF_TIME
    var player : PlayerEntity? = null

    var tmBreakdown : DefaultTableModel? = null

    fun runConversion(players : List<PlayerEntity>)
    {
        val keys = players.joinToString { p -> "'${p.rowId}'"}

        val sb = StringBuilder()
        sb.append(" DELETE FROM Achievement")
        sb.append(" WHERE AchievementRef = $achievementRef")
        if (!keys.isEmpty())
        {
            sb.append(" AND PlayerId IN ($keys)" )
        }

        if (!DatabaseUtil.executeUpdate("" + sb))
        {
            return
        }

        populateForConversion(keys)
    }

    abstract fun populateForConversion(playerIds : String)
    abstract fun getIconURL() : URL

    /**
     * Basic init will be the same for most achievements - get the value from the single row
     */
    open fun initialiseFromDb(achievementRows : List<AchievementEntity>, player: PlayerEntity?)
    {
        if (achievementRows.isEmpty())
        {
            return
        }

        if (achievementRows.size > 1)
        {
            Debug.stackTrace("Got ${achievementRows.size} rows (expected 1) for achievement $achievementRef and player ${achievementRows.first().playerId}")
        }

        val achievementRow = achievementRows.first()
        attainedValue = achievementRow.achievementCounter
        gameIdEarned = achievementRow.gameIdEarned
        localGameIdEarned = achievementRow.localGameIdEarned
        dtLatestUpdate = achievementRow.dtLastUpdate

        this.player = player
    }

    fun getScore() : Int
    {
        val color = getColor(false)
        return when (color)
        {
            Color.MAGENTA -> 6
            Color.CYAN -> 5
            Color.GREEN -> 4
            Color.YELLOW -> 3
            DartsColour.COLOUR_ACHIEVEMENT_ORANGE -> 2
            Color.RED -> 1
            else -> 0
        }
    }

    fun getColor(highlighted : Boolean) : Color
    {
        val col = if (isDecreasing())
        {
            when (attainedValue)
            {
                -1 -> Color.GRAY
                in redThreshold+1..Int.MAX_VALUE -> Color.GRAY
                in orangeThreshold+1 until redThreshold+1 -> Color.RED
                in yellowThreshold+1 until orangeThreshold+1 -> DartsColour.COLOUR_ACHIEVEMENT_ORANGE
                in greenThreshold+1 until yellowThreshold+1 -> Color.YELLOW
                in blueThreshold+1 until greenThreshold+1 -> Color.GREEN
                in pinkThreshold+1 until blueThreshold+1 -> Color.CYAN
                else -> Color.MAGENTA
            }
        }
        else
        {
            when (attainedValue)
            {
                in Int.MIN_VALUE until redThreshold -> Color.GRAY
                in redThreshold until orangeThreshold -> Color.RED
                in orangeThreshold until yellowThreshold -> DartsColour.COLOUR_ACHIEVEMENT_ORANGE
                in yellowThreshold until greenThreshold -> Color.YELLOW
                in greenThreshold until blueThreshold -> Color.GREEN
                in blueThreshold until pinkThreshold -> Color.CYAN
                else -> Color.MAGENTA
            }
        }

        if (highlighted
          && !isLocked())
        {
            return col.darker()
        }

        return col
    }

    fun getAngle() : Double
    {
        return getAngle(attainedValue)
    }
    fun getAngle(attainedValue : Int) : Double
    {
        if (attainedValue == -1)
        {
            return 0.0
        }

        return if (!isDecreasing())
        {
            360 * attainedValue.toDouble() / maxValue
        }
        else
        {
            val denom = redThreshold - maxValue + 1
            val num = Math.max(redThreshold - attainedValue + 1, 0)

            360 * num / denom.toDouble()
        }
    }

    fun isLocked() : Boolean
    {
        if (attainedValue == -1)
        {
            return true
        }

        return if (isDecreasing())
        {
            attainedValue > redThreshold
        }
        else
        {
            attainedValue < redThreshold
        }
    }

    fun isClickable(): Boolean
    {
        return !gameIdEarned.isEmpty()
          || tmBreakdown != null
    }

    fun getIcon(highlighted : Boolean) : BufferedImage?
    {
        var iconURL = getIconURL()
        if (isLocked())
        {
            iconURL = ResourceCache.URL_ACHIEVEMENT_LOCKED
        }

        val bufferedImage = ImageIO.read(iconURL)
        changeIconColor(bufferedImage, getColor(highlighted).darker())

        return bufferedImage
    }
    protected open fun changeIconColor(img : BufferedImage, newColor: Color)
    {
        img.paint {
            val current = Color(img.getRGB(it.x, it.y), true)
            if (current == Color.BLACK) newColor else current
        }
    }

    override fun toString() = name

    open fun isUnbounded() = false

    fun getProgressDesc() : String
    {
        var progressStr = "$attainedValue"
        if (!isUnbounded())
        {
            progressStr += "/$maxValue"
        }

        return progressStr
    }

    open fun isDecreasing() = false

    fun getExtraDetails() : String
    {
        var ret = if (this is AbstractAchievementRowPerGame)
        {
            "Last updated on ${dtLatestUpdate.formatAsDate()}"
        }
        else
        {
            "Earned on ${dtLatestUpdate.formatAsDate()}"
        }

        if (!gameIdEarned.isEmpty())
        {
            ret += " in Game #$localGameIdEarned"
        }

        return ret
    }

    open fun retrieveAllRows(): List<AchievementEntity>
    {
        return AchievementEntity().retrieveEntities("AchievementRef = $achievementRef")
    }
}

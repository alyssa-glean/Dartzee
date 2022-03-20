package dartzee.achievements.golf

import dartzee.achievements.*
import dartzee.db.AchievementEntity
import dartzee.game.GameType
import dartzee.utils.Database
import dartzee.utils.ResourceCache
import java.net.URL

class AchievementGolfPointsRisked : AbstractMultiRowAchievement()
{
    override val name = "Gambler"
    override val desc = "Total number of points risked (by continuing to throw) in Golf"
    override val gameType = GameType.GOLF

    override val achievementType = AchievementType.GOLF_POINTS_RISKED
    override val redThreshold = 5
    override val orangeThreshold = 10
    override val yellowThreshold = 25
    override val greenThreshold = 50
    override val blueThreshold = 100
    override val pinkThreshold = 200
    override val maxValue = 200

    override fun getIconURL() = ResourceCache.URL_ACHIEVEMENT_POINTS_RISKED
    override fun isUnbounded() = true

    override fun getBreakdownColumns() = listOf("Game", "Round", "Points risked", "Date Achieved")
    override fun getBreakdownRow(a: AchievementEntity) = arrayOf<Any>(a.localGameIdEarned, a.achievementDetail.toInt(), a.achievementCounter, a.dtAchieved)
    override fun useCounter() = true

    private fun buildPointsRiskedSql(): String
    {
        val sb = StringBuilder()
        sb.append("5 - CASE")
        sb.append(getGolfSegmentCases())
        sb.append(" END")

        return sb.toString()
    }

    override fun populateForConversion(playerIds: List<String>, database: Database)
    {
        val sb = StringBuilder()

        sb.append(" SELECT pt.PlayerId, pt.GameId, drt.RoundNumber, SUM(${buildPointsRiskedSql()}) AS PointsRisked, MAX(drt.DtCreation) AS DtAchieved")
        sb.append(" FROM Dart drt, Participant pt, Game g")
        sb.append(" WHERE drt.ParticipantId = pt.RowId")
        sb.append(" AND drt.PlayerId = pt.PlayerId")
        sb.append(" AND pt.GameId = g.RowId")
        sb.append(" AND g.GameType = '${GameType.GOLF}'")
        sb.append(" AND drt.RoundNumber = drt.Score")
        sb.append(" AND drt.Multiplier > 0")
        appendPlayerSql(sb, playerIds)
        sb.append(" AND EXISTS (")
        sb.append("     SELECT 1")
        sb.append("     FROM Dart drtOther")
        sb.append("     WHERE drtOther.ParticipantId = drt.ParticipantId")
        sb.append("     AND drtOther.PlayerId = drt.PlayerId")
        sb.append("     AND drtOther.RoundNumber = drt.RoundNumber")
        sb.append("     AND drtOther.Ordinal > drt.Ordinal)")
        sb.append(" GROUP BY pt.PlayerId, pt.GameId, drt.RoundNumber")

        database.executeQuery(sb).use { rs ->
            bulkInsertFromResultSet(rs,
                database,
                achievementType,
                achievementCounterFn = { rs.getInt("PointsRisked") },
                achievementDetailFn = { rs.getInt("RoundNumber").toString() }
            )
        }
    }
}
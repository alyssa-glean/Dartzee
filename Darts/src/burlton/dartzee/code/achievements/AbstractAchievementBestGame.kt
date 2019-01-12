package burlton.dartzee.code.achievements

import burlton.core.code.util.Debug
import burlton.dartzee.code.db.AchievementEntity
import burlton.dartzee.code.utils.DatabaseUtil
import java.sql.SQLException

abstract class AbstractAchievementBestGame : AbstractAchievement()
{
    abstract val gameType: Int
    abstract val gameParams: String

    override fun populateForConversion(playerIds: String)
    {
        val sb = StringBuilder()
        sb.append(" SELECT pt.PlayerId, g.RowId AS GameId, pt.FinalScore, pt.DtFinished")
        sb.append(" FROM Participant pt, Game g")
        sb.append(" WHERE pt.GameId = g.RowId")
        sb.append(" AND g.GameType = $gameType")
        sb.append(" AND pt.FinalScore > -1")

        if (!playerIds.isEmpty())
        {
            sb.append(" AND pt.PlayerId IN ($playerIds)")
        }

        if (!gameParams.isEmpty())
        {
            sb.append(" AND g.GameParams = '$gameParams'")
        }

        sb.append(" AND NOT EXISTS (")
        sb.append("     SELECT 1")
        sb.append("     FROM Participant pt2, Game g2")
        sb.append("     WHERE pt2.GameId = g2.RowID")
        sb.append("     AND g2.GameType = g.GameType")
        sb.append("     AND pt2.PlayerId = pt.PlayerId")
        sb.append("     AND pt2.FinalScore > -1")
        sb.append("     AND (pt2.FinalScore < pt.FinalScore OR (pt2.FinalScore = pt.FinalScore AND pt2.DtFinished < pt.DtFinished))")
        sb.append(")")

        try
        {
            DatabaseUtil.executeQuery(sb).use { rs ->
                while (rs.next())
                {
                    val playerId = rs.getLong("PlayerId")
                    val gameId = rs.getLong("GameId")
                    val dtFinished = rs.getTimestamp("DtFinished")
                    val score = rs.getInt("FinalScore")

                    AchievementEntity.factoryAndSave(achievementRef, playerId, gameId, score, "", dtFinished)
                }
            }
        }
        catch (sqle: SQLException)
        {
            Debug.logSqlException(sb.toString(), sqle)
        }
    }

    override fun isDecreasing(): Boolean
    {
        return true
    }
}
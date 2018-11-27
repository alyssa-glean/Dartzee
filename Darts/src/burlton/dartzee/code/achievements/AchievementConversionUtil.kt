package burlton.dartzee.code.achievements

import burlton.core.code.util.Debug
import burlton.dartzee.code.db.AchievementEntity
import burlton.dartzee.code.db.GameEntity
import burlton.dartzee.code.screen.stats.overall.OverallStatsScreen
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.desktopcore.code.util.DateUtil
import java.sql.SQLException

fun runAchievementConversion()
{

}


fun unlockThreeDartAchievement(dtColumn: String, lastDartWhereSql: String, achievementRef: Int)
{
    val tempTable = DatabaseUtil.createTempTable("PlayerFinishes", "PlayerId INT, GameId INT, DtAchieved TIMESTAMP, Score INT")
            ?: return

    var sb = StringBuilder()
    sb.append("INSERT INTO $tempTable")
    sb.append(" SELECT p.RowId, pt.GameId, $dtColumn, ${OverallStatsScreen.TOTAL_ROUND_SCORE_SQL_STR}")
    sb.append(" FROM Dart drtFirst, Dart drtLast, Round rnd, Participant pt, Player p, Game g")
    sb.append(" WHERE drtFirst.RoundId = rnd.RowId")
    sb.append(" AND drtLast.RoundId = rnd.RowId")
    sb.append(" AND drtFirst.Ordinal = 1")
    sb.append(" AND rnd.ParticipantId = pt.RowId")
    sb.append(" AND pt.PlayerId = p.RowId")
    sb.append(" AND pt.DtFinished < ${DateUtil.getEndOfTimeSqlString()}")
    sb.append(" AND $lastDartWhereSql")
    sb.append(" AND pt.GameId = g.RowId")
    sb.append(" AND g.GameType = " + GameEntity.GAME_TYPE_X01)

    if (!DatabaseUtil.executeUpdate("" + sb))
    {
        DatabaseUtil.dropTable(tempTable)
        return
    }

    sb = StringBuilder()
    sb.append(" SELECT PlayerId, GameId, DtAchieved, Score")
    sb.append(" FROM $tempTable zz1")
    sb.append(" WHERE NOT EXISTS (")
    sb.append(" 	SELECT 1")
    sb.append(" 	FROM $tempTable zz2")
    sb.append(" 	WHERE zz2.PlayerId = zz1.PlayerId")
    sb.append(" 	AND (zz2.Score > zz1.Score OR (zz2.Score = zz1.Score AND zz2.GameId < zz1.GameId))")
    sb.append(" )")
    sb.append(" ORDER BY PlayerId")

    try
    {
        DatabaseUtil.executeQuery(sb).use { rs ->
            while (rs.next())
            {
                val playerId = rs.getLong("PlayerId")
                val gameId = rs.getLong("GameId")
                val dtAchieved = rs.getTimestamp("DtAchieved")
                val score = rs.getInt("Score")

                AchievementEntity.factoryAndSave(achievementRef, playerId, gameId, score, dtAchieved)
            }
        }
    }
    catch (sqle: SQLException)
    {
        Debug.logSqlException(sb.toString(), sqle)
    }
    finally
    {
        DatabaseUtil.dropTable(tempTable)
    }
}
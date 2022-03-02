package dartzee.achievements.golf

import dartzee.`object`.SegmentType
import dartzee.achievements.AchievementType
import dartzee.achievements.AbstractMultiRowAchievement
import dartzee.achievements.appendPlayerSql
import dartzee.achievements.bulkInsertFromResultSet
import dartzee.db.AchievementEntity
import dartzee.game.GameType
import dartzee.utils.Database
import dartzee.utils.ResourceCache
import java.net.URL

class AchievementGolfCourseMaster : AbstractMultiRowAchievement()
{
    override val name = "Course Master"
    override val desc = "Unique holes where a hole-in-one has been achieved"
    override val achievementType = AchievementType.GOLF_COURSE_MASTER
    override val redThreshold = 1
    override val orangeThreshold = 3
    override val yellowThreshold = 6
    override val greenThreshold = 10
    override val blueThreshold = 14
    override val pinkThreshold = 18
    override val maxValue = 18
    override val gameType = GameType.GOLF

    override fun getIconURL() = ResourceCache.URL_ACHIEVEMENT_GOLF_COURSE_MASTER

    override fun getBreakdownColumns() = listOf("Hole", "Game", "Date Achieved")
    override fun getBreakdownRow(a: AchievementEntity) = arrayOf<Any>(a.achievementDetail.toInt(), a.localGameIdEarned, a.dtAchieved)
    override fun isUnbounded() = false

    override fun populateForConversion(playerIds: List<String>, database: Database)
    {
        val tempTable = database.createTempTable("PlayerHolesInOne", "PlayerId VARCHAR(36), Score INT, GameId VARCHAR(36), DtAchieved TIMESTAMP")
                ?: return

        var sb = StringBuilder()

        sb.append(" INSERT INTO $tempTable")
        sb.append(" SELECT pt.PlayerId, d.Score, g.RowId, d.DtCreation")
        sb.append(" FROM Dart d, Participant pt, Game g")
        sb.append(" WHERE d.SegmentType = '${SegmentType.DOUBLE}'")
        sb.append(" AND d.RoundNumber = d.Score")
        sb.append(" AND d.ParticipantId = pt.RowId")
        sb.append(" AND d.PlayerId = pt.PlayerId")
        sb.append(" AND pt.GameId = g.RowId")
        sb.append(" AND g.GameType = '${GameType.GOLF}'")
        appendPlayerSql(sb, playerIds, "pt")

        if (!database.executeUpdate(sb)) return

        sb = StringBuilder()
        sb.append(" SELECT PlayerId, Score, GameId, DtAchieved")
        sb.append(" FROM $tempTable zz1")
        sb.append(" WHERE NOT EXISTS (")
        sb.append("     SELECT 1")
        sb.append("     FROM $tempTable zz2")
        sb.append("     WHERE zz1.PlayerId = zz2.PlayerId")
        sb.append("     AND zz1.Score = zz2.Score")
        sb.append("     AND zz2.DtAchieved < zz1.DtAchieved")
        sb.append(")")

        database.executeQuery(sb).use { rs ->
            bulkInsertFromResultSet(rs, database, achievementType, achievementDetailFn = { rs.getInt("Score").toString() })
        }
    }
}
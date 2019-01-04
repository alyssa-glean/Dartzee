package burlton.dartzee.code.achievements

import burlton.core.code.util.Debug
import burlton.dartzee.code.db.AchievementEntity
import burlton.dartzee.code.db.GameEntity
import burlton.dartzee.code.db.PlayerEntity
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.desktopcore.code.screen.ProgressDialog
import java.sql.SQLException
import kotlin.streams.toList

fun convertEmptyAchievements()
{
    val emptyAchievements = getAllAchievements().filter{a -> !rowsExistForAchievement(a)}.toMutableList()

    if (!emptyAchievements.isEmpty())
    {
        runConversionsWithProgressBar(emptyAchievements, mutableListOf())
    }
}

fun runConversionsWithProgressBar(achievements: MutableList<AbstractAchievement>, players: MutableList<PlayerEntity>)
{
    val r = Runnable { runConversionsInOtherThread(achievements, players)}
    val t = Thread(r, "Conversion thread")
    t.start()
}

private fun runConversionsInOtherThread(achievements: MutableList<AbstractAchievement>, players: MutableList<PlayerEntity>)
{
    val dlg = ProgressDialog.factory("Populating Achievements", "achievements remaining", achievements.size)
    dlg.setVisibleLater()

    achievements.forEach{
        it.runConversion(players)
        dlg.incrementProgressLater()
    }

    dlg.disposeLater()
}

fun rowsExistForAchievement(achievement: AbstractAchievement) : Boolean
{
    val sql = "SELECT COUNT(1) FROM Achievement WHERE AchievementRef = ${achievement.achievementRef}"
    val count = DatabaseUtil.executeQueryAggregate(sql)

    return count > 0
}


fun getAllAchievements() : MutableList<AbstractAchievement>
{
    return mutableListOf(AchievementX01BestFinish(),
                         AchievementX01BestThreeDarts(),
                         AchievementX01CheckoutCompleteness(),
                         AchievementX01HighestBust(),
                         AchievementGolfPointsRisked(),
                         AchievementX01GamesWon(),
                         AchievementGolfGamesWon(),
                         AchievementClockGamesWon(),
                         AchievementX01BestGame(),
                         AchievementGolfBestGame(),
                         AchievementClockBestGame(),
                         AchievementClockBruceyBonuses())
}

fun getAchievementForRef(achievementRef : Int) : AbstractAchievement?
{
    for (achievement in getAllAchievements())
    {
        if (achievement.achievementRef == achievementRef)
        {
            return achievement
        }
    }

    Debug.stackTrace("No achievement found for AchievementRef [$achievementRef]")
    return null
}

fun getBestGameAchievement(gameType : Int) : AbstractAchievementBestGame?
{
    val ref = getAllAchievements().find {it is AbstractAchievementBestGame && it.gameType == gameType}
    if (ref == null)
    {
        Debug.stackTrace("No best game achievement found for GameType [$gameType]")
    }

    return ref as AbstractAchievementBestGame
}

fun getWinAchievementRef(gameType : Int) : Int
{
    val ref = getAllAchievements().find {it is AbstractAchievementGamesWon && it.gameType == gameType}?.achievementRef
    if (ref == null)
    {
        Debug.stackTrace("No total wins achievement found for GameType [$gameType]")
    }

    return ref ?: -1
}

fun unlockThreeDartAchievement(playerSql : String, dtColumn: String, lastDartWhereSql: String,
                               achievementScoreSql : String, achievementRef: Int)
{
    val tempTable = DatabaseUtil.createTempTable("PlayerFinishes", "PlayerId INT, GameId INT, DtAchieved TIMESTAMP, Score INT")
            ?: return

    var sb = StringBuilder()
    sb.append("INSERT INTO $tempTable")
    sb.append(" SELECT p.RowId, pt.GameId, $dtColumn, $achievementScoreSql")
    sb.append(" FROM Dart drtFirst, Dart drtLast, Round rnd, Participant pt, Player p, Game g")
    sb.append(" WHERE drtFirst.RoundId = rnd.RowId")
    sb.append(" AND drtLast.RoundId = rnd.RowId")
    sb.append(" AND drtFirst.Ordinal = 1")
    sb.append(" AND rnd.ParticipantId = pt.RowId")
    sb.append(" AND pt.PlayerId = p.RowId")
    if (!playerSql.isEmpty())
    {
        sb.append(" AND pt.PlayerId IN ($playerSql)")
    }
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
    sb.append(" 	AND (zz2.Score > zz1.Score OR (zz2.Score = zz1.Score AND zz2.DtAchieved < zz1.DtAchieved))")
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

fun insertForCheckoutCompleteness(playerId: Long, gameId: Long, counter: Int)
{
    val achievementRef = ACHIEVEMENT_REF_X01_CHECKOUT_COMPLETENESS
    val whereSql = "PlayerId = $playerId AND AchievementRef = $achievementRef"

    val achievementRows = AchievementEntity().retrieveEntities(whereSql)
    val hitDoubles = achievementRows.stream().mapToInt{it.achievementCounter}.toList()
    if (!hitDoubles.contains(counter))
    {
        AchievementEntity.factoryAndSave(achievementRef, playerId, gameId, counter)

        val template = AchievementX01CheckoutCompleteness()
        val arrayList = ArrayList(hitDoubles)
        arrayList.add(counter)

        template.hitDoubles = arrayList

        AchievementEntity.triggerAchievementUnlock(achievementRows.size, achievementRows.size + 1, template, playerId, gameId)
    }
}
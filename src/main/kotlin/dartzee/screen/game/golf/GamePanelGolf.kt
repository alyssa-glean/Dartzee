package dartzee.screen.game.golf

import dartzee.achievements.ACHIEVEMENT_REF_GOLF_COURSE_MASTER
import dartzee.achievements.ACHIEVEMENT_REF_GOLF_POINTS_RISKED
import dartzee.achievements.retrieveAchievementForDetail
import dartzee.ai.DartsAiModel
import dartzee.core.util.doGolfMiss
import dartzee.db.AchievementEntity
import dartzee.db.GameEntity
import dartzee.db.ParticipantEntity
import dartzee.game.state.GolfPlayerState
import dartzee.screen.Dartboard
import dartzee.screen.game.AbstractDartsGameScreen
import dartzee.screen.game.GamePanelFixedLength
import dartzee.screen.game.scorer.DartsScorerGolf

open class GamePanelGolf(parent: AbstractDartsGameScreen, game: GameEntity, totalPlayers: Int) :
        GamePanelFixedLength<DartsScorerGolf, Dartboard, GolfPlayerState>(parent, game, totalPlayers)
{
    //Number of rounds - 9 holes or 18?
    override val totalRounds = Integer.parseInt(game.gameParams)

    override fun factoryDartboard() = Dartboard()
    override fun factoryState(pt: ParticipantEntity) = GolfPlayerState(pt)

    private fun getScoreForMostRecentDart() : Int
    {
        val lastDart = getDartsThrown().last()

        val targetHole = currentRoundNumber
        return lastDart.getGolfScore(targetHole)
    }

    override fun doAiTurn(model: DartsAiModel)
    {
        val targetHole = currentRoundNumber
        val dartNo = dartsThrownCount() + 1
        model.throwGolfDart(targetHole, dartNo, dartboard)
    }

    override fun shouldStopAfterDartThrown(): Boolean
    {
        val dartsThrownCount = dartsThrownCount()
        if (dartsThrownCount == 3)
        {
            return true
        }

        val score = getScoreForMostRecentDart()
        if (getCurrentPlayerState().isHuman())
        {
            return score == 1
        }
        else
        {
            val model = getCurrentPlayerStrategy()
            val stopThreshold = model.getStopThresholdForDartNo(dartsThrownCount)

            return score <= stopThreshold
        }
    }

    override fun saveDartsAndProceed()
    {
        unlockAchievements()
        commitRound()

        finishRound()
    }

    fun unlockAchievements()
    {
        val lastDart = getDartsThrown().last()
        val dartsRisked = getDartsThrown() - lastDart
        val pointsRisked = dartsRisked.map { 5 - it.getGolfScore(currentRoundNumber) }.sum()

        if (pointsRisked > 0)
        {
            AchievementEntity.insertAchievementWithCounter(ACHIEVEMENT_REF_GOLF_POINTS_RISKED, getCurrentPlayerId(), gameEntity.rowId, "$currentRoundNumber", pointsRisked)
        }

        if (lastDart.getGolfScore(currentRoundNumber) == 1
         && retrieveAchievementForDetail(ACHIEVEMENT_REF_GOLF_COURSE_MASTER, getCurrentPlayerId(), "$currentRoundNumber") == null)
        {
            AchievementEntity.insertAchievement(ACHIEVEMENT_REF_GOLF_COURSE_MASTER, getCurrentPlayerId(), getGameId(), "$currentRoundNumber")
        }
    }

    override fun factoryScorer() = DartsScorerGolf()

    override fun shouldAIStop() = false

    override fun doMissAnimation()
    {
        dartboard.doGolfMiss()
    }

    override fun factoryStatsPanel(gameParams: String) = GameStatisticsPanelGolf()
}

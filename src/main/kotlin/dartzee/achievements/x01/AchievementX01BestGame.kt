package dartzee.achievements.x01

import dartzee.achievements.AbstractAchievementBestGame
import dartzee.achievements.AchievementType
import dartzee.game.GameType
import dartzee.utils.ResourceCache

class AchievementX01BestGame : AbstractAchievementBestGame()
{
    override val achievementType = AchievementType.X01_BEST_GAME
    override val name = "Leg-up"
    override val desc = "Best game of 501"
    override val gameType = GameType.X01
    override val gameParams = "501"

    override val redThreshold = 99
    override val orangeThreshold = 60
    override val yellowThreshold = 42
    override val greenThreshold = 30
    override val blueThreshold = 24
    override val pinkThreshold = 12
    override val maxValue = 9

    override fun getIconURL() = ResourceCache.URL_ACHIEVEMENT_X01_BEST_GAME
}
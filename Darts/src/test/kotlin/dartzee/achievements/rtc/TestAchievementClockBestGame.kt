package dartzee.achievements.rtc

import dartzee.achievements.rtc.AchievementClockBestGame
import dartzee.db.CLOCK_TYPE_STANDARD
import dartzee.db.GAME_TYPE_ROUND_THE_CLOCK
import dartzee.achievements.TestAbstractAchievementBestGame
import io.kotlintest.shouldBe
import org.junit.Test

class TestAchievementClockBestGame: TestAbstractAchievementBestGame<AchievementClockBestGame>()
{
    override fun factoryAchievement() = AchievementClockBestGame()

    @Test
    fun `Should only count standard games of RTC`()
    {
        val achievement = factoryAchievement()

        achievement.gameType shouldBe GAME_TYPE_ROUND_THE_CLOCK
        achievement.gameParams shouldBe CLOCK_TYPE_STANDARD
    }
}
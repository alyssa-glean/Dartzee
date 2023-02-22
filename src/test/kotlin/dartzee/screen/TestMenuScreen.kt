package dartzee.screen

import com.github.alyssaburlton.swingtest.clickChild
import dartzee.helper.AbstractTest
import dartzee.screen.dartzee.DartzeeTemplateSetupScreen
import dartzee.screen.player.PlayerManagementScreen
import dartzee.screen.preference.PreferencesScreen
import dartzee.screen.reporting.ReportingSetupScreen
import dartzee.screen.stats.overall.LeaderboardsScreen
import dartzee.screen.sync.SyncManagementScreen
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import javax.swing.JButton

class TestMenuScreen: AbstractTest()
{
    @Test
    fun `Should go to Sync Management screen`()
    {
        val scrn = MenuScreen()
        scrn.clickChild<JButton>(text = "Sync Setup")
        ScreenCache.currentScreen().shouldBeInstanceOf<SyncManagementScreen>()
    }

    @Test
    fun `Should go to the game setup screen`()
    {
        val scrn = MenuScreen()
        scrn.clickChild<JButton>(text = "New Game")
        ScreenCache.currentScreen().shouldBeInstanceOf<GameSetupScreen>()
    }

    @Test
    fun `Should go to the player management screen`()
    {
        val scrn = MenuScreen()
        scrn.clickChild<JButton>(text = "Manage Players")
        ScreenCache.currentScreen().shouldBeInstanceOf<PlayerManagementScreen>()
    }

    @Test
    fun `Should go to the reporting setup screen`()
    {
        val scrn = MenuScreen()
        scrn.clickChild<JButton>(text = "Game Report")
        ScreenCache.currentScreen().shouldBeInstanceOf<ReportingSetupScreen>()
    }

    @Test
    fun `Should go to the leaderboards screen`()
    {
        val scrn = MenuScreen()
        scrn.clickChild<JButton>(text = "Leaderboards")
        ScreenCache.currentScreen().shouldBeInstanceOf<LeaderboardsScreen>()
    }

    @Test
    fun `Should go to the utilities screen`()
    {
        val scrn = MenuScreen()
        scrn.clickChild<JButton>(text = "Utilities")
        ScreenCache.currentScreen().shouldBeInstanceOf<UtilitiesScreen>()
    }

    @Test
    fun `Should go to the preferences screen`()
    {
        val scrn = MenuScreen()
        scrn.clickChild<JButton>(text = "Preferences")
        ScreenCache.currentScreen().shouldBeInstanceOf<PreferencesScreen>()
    }

    @Test
    fun `Should go to the dartzee template setup screen`()
    {
        val scrn = MenuScreen()
        scrn.clickChild<JButton>(text = "Dartzee")
        ScreenCache.currentScreen().shouldBeInstanceOf<DartzeeTemplateSetupScreen>()
    }
}
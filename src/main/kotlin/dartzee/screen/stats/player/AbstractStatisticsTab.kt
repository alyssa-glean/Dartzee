package dartzee.screen.stats.player

import dartzee.core.util.containsComponent
import dartzee.screen.ScreenCache
import dartzee.stats.GameWrapper
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.GridLayout
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JPanel

abstract class AbstractStatisticsTab : JPanel(), PropertyChangeListener
{
    protected var filteredGames = listOf<GameWrapper>()
    protected var filteredGamesOther = listOf<GameWrapper>()

    init
    {
        preferredSize = Dimension(500, 150)
    }

    abstract fun populateStats()
    fun includeOtherComparison() = filteredGamesOther.isNotEmpty()

    /**
     * For the tabs that are a simple grid layout showing two tables.
     */
    protected fun setOtherComponentVisibility(container: Container, otherComponent: Component)
    {
        if (container.layout !is GridLayout)
        {
            throw Exception("Calling method with inappropriate layout: $layout")
        }

        if (!includeOtherComparison())
        {
            container.layout = GridLayout(0, 1, 0, 0)
            container.remove(otherComponent)
        }
        else if (!container.containsComponent(otherComponent))
        {
            container.layout = GridLayout(0, 2, 0, 0)
            container.add(otherComponent)
        }

        repaint()
    }

    /**
     * Helpers
     */
    fun getDistinctGameParams() = filteredGames.map{ it.gameParams }.distinct()
    fun getGameType() = ScreenCache.get<PlayerStatisticsScreen>().gameType

    /**
     * Gets / sets
     */
    fun setFilteredGames(filteredGames: List<GameWrapper>, filteredGamesOther: List<GameWrapper>)
    {
        this.filteredGames = filteredGames
        this.filteredGamesOther = filteredGamesOther
    }

    /**
     * PropertyChangeListener
     */
    override fun propertyChange(arg0: PropertyChangeEvent)
    {
        val propertyName = arg0.propertyName
        if (propertyName == "value")
        {
            populateStats()
        }
    }
}

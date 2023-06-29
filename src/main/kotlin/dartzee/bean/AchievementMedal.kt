package dartzee.bean

import dartzee.achievements.AbstractAchievement
import dartzee.screen.ScreenCache
import dartzee.screen.stats.player.PlayerAchievementBreakdown
import dartzee.screen.stats.player.PlayerAchievementsScreen
import dartzee.utils.DartsColour
import dartzee.utils.InjectedThings.gameLauncher
import dartzee.utils.ResourceCache
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel

const val SIZE = 164

class AchievementMedal(val achievement : AbstractAchievement, private val hoveringEnabled: Boolean = true): JComponent(), IMouseListener
{
    private val angle = achievement.getAngle()
    private var highlighted = false

    init
    {
        preferredSize = Dimension(SIZE, SIZE)

        addMouseListener(this)
        addMouseMotionListener(this)
    }

    override fun paint(g: Graphics?)
    {
        if (g is Graphics2D)
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            //Draw the track
            g.color = Color.DARK_GRAY.brighter()
            g.fillArc(0, 0, SIZE, SIZE, 0, 360)

            //Mark the levels
            markThreshold(g, Color.MAGENTA, achievement.pinkThreshold)
            markThreshold(g, Color.CYAN, achievement.blueThreshold)
            markThreshold(g, Color.GREEN, achievement.greenThreshold)
            markThreshold(g, Color.YELLOW, achievement.yellowThreshold)
            markThreshold(g, DartsColour.COLOUR_ACHIEVEMENT_ORANGE, achievement.orangeThreshold)
            markThreshold(g, Color.RED, achievement.redThreshold)

            //Draw the actual progress
            g.color = achievement.getColor(highlighted).darker()
            g.fillArc(0, 0, SIZE, SIZE, 90, -angle.toInt())

            //Inner circle
            g.color = achievement.getColor(highlighted)

            g.fillArc(15, 15, SIZE-30, SIZE-30, 0, 360)

            val icon = achievement.getIcon(highlighted)

            var y = 30
            if (achievement.isLocked())
            {
                y = (SIZE / 2) - 40
            }

            icon?.let {
                val x = (SIZE / 2) - (icon.width / 2)
                g.drawImage(icon, null, x, y)
            }

            if (!achievement.isLocked())
            {
                val label = JLabel(achievement.getProgressDesc())
                label.setSize(SIZE, 25)
                label.font = ResourceCache.BASE_FONT.deriveFont(Font.PLAIN, 24f)
                label.horizontalAlignment = JLabel.CENTER
                label.foreground = achievement.getColor(highlighted).darker()

                g.translate(0, 100)
                label.paint(g)
            }
        }
    }

    private fun markThreshold(g : Graphics2D, color : Color, threshold : Int)
    {
        g.color = color
        val thresholdAngle = achievement.getAngle(threshold)
        g.fillArc(0, 0, SIZE, SIZE, 90 - thresholdAngle.toInt(), 3)
    }

    private fun updateForMouseOver(e : MouseEvent)
    {
        if (!hoveringEnabled)
        {
            return
        }

        val pt = e.point
        highlighted = pt.distance(Point(SIZE/2, SIZE/2)) < SIZE/2

        val currentScreen = ScreenCache.currentScreen()
        if (currentScreen is PlayerAchievementsScreen)
        {
            currentScreen.toggleAchievementDesc(highlighted, achievement)
        }

        cursor = if (highlighted && achievement.isClickable())
        {
            Cursor(Cursor.HAND_CURSOR)
        }
        else
        {
            Cursor(Cursor.DEFAULT_CURSOR)
        }

        repaint()
    }

    override fun mouseReleased(e: MouseEvent)
    {
        if (achievement.tmBreakdown != null)
        {
            val scrn = ScreenCache.get<PlayerAchievementBreakdown>()
            scrn.setState(achievement)

            ScreenCache.switch(scrn)
        }
        else if (achievement.gameIdEarned.isNotEmpty())
        {
            gameLauncher.loadAndDisplayGame(achievement.gameIdEarned)
        }
    }

    override fun mouseEntered(e: MouseEvent)
    {
        updateForMouseOver(e)
    }

    override fun mouseExited(e: MouseEvent)
    {
        updateForMouseOver(e)
    }

    override fun mouseMoved(e: MouseEvent)
    {
        updateForMouseOver(e)
    }
}

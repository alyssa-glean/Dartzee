package burlton.dartzee.code.screen.game.scorer

import burlton.dartzee.code.core.util.Debug
import burlton.dartzee.code.`object`.Dart
import burlton.dartzee.code.achievements.AbstractAchievement
import burlton.dartzee.code.bean.AchievementMedal
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.BevelBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder

const val SCORER_WIDTH = 210

abstract class DartsScorer : AbstractScorer() {

    private val overlays = mutableListOf<AchievementOverlay>()

    init
    {
        preferredSize = Dimension(SCORER_WIDTH, 600)
        panelAvatar.border = EmptyBorder(5, 30, 5, 30)
    }


    /**
     * Add a dart to the scorer.
     */
    open fun addDart(drt: Dart)
    {
        var rowCount = model.rowCount
        if (shouldAddRow(rowCount))
        {
            val row = makeEmptyRow()
            addRow(row)
            rowCount++
        }

        addDartToRow(rowCount - 1, drt)

        updatePlayerResult()
    }

    private fun shouldAddRow(rowCount: Int): Boolean
    {
        if (rowCount == 0) {
            return true
        }

        return rowIsComplete(rowCount - 1)
    }

    /**
     * Default method, overridden by Round the Clock
     */
    open fun getNumberOfColumnsForAddingNewDart() = getNumberOfColumns() - 1

    private fun addDartToRow(rowNumber: Int, drt: Dart)
    {
        for (i in 0 until getNumberOfColumnsForAddingNewDart())
        {
            val currentVal = model.getValueAt(rowNumber, i)
            if (currentVal == null)
            {
                model.setValueAt(drt, rowNumber, i)
                repaint()
                return
            }
        }

        Debug.stackTrace("Trying to add dart to row $rowNumber but it's already full.")
    }

    fun achievementUnlocked(achievement: AbstractAchievement)
    {
        val overlay = AchievementOverlay(achievement)

        overlays.add(overlay)

        //Let's just only ever have one thing at a time on display. Actually layering them sometimes worked but
        //sometimes caused weird bollucks when things happened close together
        layeredPane.removeAll()
        layeredPane.add(overlay, BorderLayout.CENTER)
        layeredPane.revalidate()
        layeredPane.repaint()
    }


    /**
     * Default Methods
     */
    open fun confirmCurrentRound() {}
    open fun updatePlayerResult() {}

    /**
     * Abstract Methods
     */
    abstract fun getTotalScore(): Int
    abstract fun rowIsComplete(rowNumber: Int): Boolean

    open fun clearRound(roundNumber: Int)
    {
        if (roundNumber > model.rowCount)
        {
            return
        }

        val row = roundNumber - 1
        model.removeRow(row)
    }

    fun getRowCount() = model.rowCount
    fun getValueAt(row: Int, col: Int): Any? = model.getValueAt(row, col)

    private inner class AchievementOverlay(achievement: AbstractAchievement) : JPanel(), ActionListener, MouseListener
    {
        private val btnClose = JButton("X")
        private val fillColor = achievement.getColor(false).brighter()
        private val borderColor = fillColor.darker()

        init
        {
            layout = BorderLayout(0, 0)
            border = LineBorder(borderColor, 6)

            background = fillColor
            val panelNorth = JPanel()
            panelNorth.background = fillColor

            add(panelNorth, BorderLayout.NORTH)
            val fl = FlowLayout()
            fl.alignment = FlowLayout.TRAILING
            panelNorth.layout = fl

            panelNorth.add(btnClose)

            btnClose.font = Font("Trebuchet MS", Font.BOLD, 16)
            btnClose.preferredSize = Dimension(40, 40)
            btnClose.background = fillColor
            btnClose.foreground = borderColor.darker()
            btnClose.isContentAreaFilled = false
            btnClose.border = BevelBorder(BevelBorder.RAISED, borderColor, borderColor.darker())

            val panelCenter = JPanel()
            add(panelCenter, BorderLayout.CENTER)
            panelCenter.layout = MigLayout("", "[grow]", "[][][][]")
            panelCenter.background = fillColor

            val medal = AchievementMedal(achievement)
            medal.hoveringEnabled = false
            medal.preferredSize = Dimension(175, 200)
            panelCenter.add(medal, "cell 0 2, alignx center")

            val lblAchievement = factoryTextLabel("Achievement")
            panelCenter.add(lblAchievement, "cell 0 0")

            val lblUnlocked = factoryTextLabel("Unlocked!")
            lblUnlocked.preferredSize = Dimension(200, 50)
            lblUnlocked.verticalAlignment = JLabel.TOP
            panelCenter.add(lblUnlocked, "cell 0 1")

            val lbName = factoryTextLabel(achievement.name, 20)
            panelCenter.add(lbName, "cell 0 3")

            btnClose.addMouseListener(this)
            btnClose.addActionListener(this)
        }

        private fun factoryTextLabel(text: String, fontSize: Int = 24) : JLabel
        {
            val lbl = JLabel(text)
            lbl.background = fillColor
            lbl.foreground = borderColor.darker()
            lbl.horizontalAlignment = JLabel.CENTER
            lbl.font = Font("Trebuchet MS", Font.BOLD, fontSize)
            lbl.preferredSize = Dimension(200, 30)

            return lbl
        }

        override fun actionPerformed(e: ActionEvent)
        {
            layeredPane.removeAll()
            overlays.remove(this)

            //If there are more overlays stacked 'beneath', show the next one of them now
            if (!overlays.isEmpty())
            {
                layeredPane.add(overlays.last(), BorderLayout.CENTER)
            }
            else
            {
                layeredPane.add(tableScores)
            }

            layeredPane.revalidate()
            layeredPane.repaint()
            revalidate()
            repaint()
        }


        override fun mousePressed(e: MouseEvent?)
        {
            btnClose.foreground = borderColor.darker().darker()
            btnClose.background = fillColor.darker()
            btnClose.border = BevelBorder(BevelBorder.LOWERED, borderColor.darker(), borderColor.darker().darker())
            btnClose.repaint()
        }

        override fun mouseReleased(e: MouseEvent?)
        {
            btnClose.background = fillColor
            btnClose.foreground = borderColor.darker()
            btnClose.border = BevelBorder(BevelBorder.RAISED, borderColor, borderColor.darker())
            btnClose.repaint()
        }

        override fun mouseClicked(e: MouseEvent?) {}

        override fun mouseEntered(e: MouseEvent?) {}
        override fun mouseExited(e: MouseEvent?) {}
    }

}

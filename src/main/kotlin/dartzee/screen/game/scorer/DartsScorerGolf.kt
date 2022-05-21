package dartzee.screen.game.scorer

import dartzee.`object`.Dart
import dartzee.game.state.GolfPlayerState
import dartzee.game.state.IWrappedParticipant
import dartzee.utils.PREFERENCES_DOUBLE_BG_BRIGHTNESS
import dartzee.utils.PREFERENCES_DOUBLE_FG_BRIGHTNESS
import dartzee.utils.PreferenceUtil
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.border.Border
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableCellRenderer

class DartsScorerGolf(participant: IWrappedParticipant, private val showGameId: Boolean = false) : AbstractDartsScorer<GolfPlayerState>(participant)
{
    var fudgeFactor = 0 //For when we're displaying only a back 9, we need to shift everything up

    override fun getNumberOfColumns() = if (showGameId) 6 else 5

    override fun initImpl()
    {
        for (i in 0..SCORE_COLUMN)
        {
            tableScores.setRenderer(i, DartRenderer(showGameId))
        }

        if (showGameId)
        {
            tableScores.setLinkColumnIndex(tableScores.columnCount - 1)
        }
    }

    override fun stateChangedImpl(state: GolfPlayerState)
    {
        setScoreAndFinishingPosition(state)

        state.completedRounds.forEachIndexed { ix, round ->
            val roundNumber = ix + 1

            addDartRound(round)

            val score = state.getScoreForRound(roundNumber)
            model.setValueAt(score, model.rowCount - 1, SCORE_COLUMN)

            if (roundNumber == 9 || roundNumber == 18)
            {
                val totalRow = arrayOf<Any?>(null, null, null, null, state.getCumulativeScoreForRound(roundNumber))
                addRow(totalRow)
            }
        }

        if (state.currentRound.isNotEmpty())
        {
            addDartRound(state.currentRound)
        }
    }

    fun addGameIds(localGameIds: List<Long>)
    {
        localGameIds.forEachIndexed { ix, gameId ->
            val row = if (ix >= 9) ix + 1 else ix
            model.setValueAt(gameId, row, GAME_ID_COLUMN)
        }
    }

    override fun makeEmptyRow(): Array<Any?>
    {
        val emptyRow = super.makeEmptyRow()

        //Set the first column to be the round number
        val rowCount = model.rowCount
        emptyRow[0] = getTargetForRowNumber(rowCount)

        return emptyRow
    }

    fun setTableForeground(color: Color)
    {
        tableScores.tableForeground = color
        lblResult.foreground = color
    }

    /**
     * Static methods
     */
    private fun getTargetForRowNumber(row: Int): Int
    {
        if (row < ROUNDS_HALFWAY)
        {
            //Row 0 is 1, etc.
            return row + fudgeFactor + 1
        }

        if (row > ROUNDS_HALFWAY)
        {
            //We have an extra subtotal row to consider
            return row + fudgeFactor
        }

        throw Exception("Trying to get round target for the subtotal row")
    }

    /**
     * Inner Classes
     */
    private class DartRenderer(private val showGameId: Boolean) : DefaultTableCellRenderer()
    {
        override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component
        {
            val newValue = getReplacementValue(table, value, row)
            val cell = super.getTableCellRendererComponent(table, newValue, isSelected, hasFocus, row, column) as JComponent

            horizontalAlignment = SwingConstants.CENTER
            font = Font("Trebuchet MS", Font.BOLD, 15)

            val border = getBorderForCell(row, column)
            cell.border = border

            if (column == 0 || newValue == null || isScoreRow(row))
            {
                foreground = null
                background = null
            }
            else
            {
                val score = newValue as Int

                val bgBrightness = PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_BG_BRIGHTNESS)
                val fgBrightness = PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_FG_BRIGHTNESS)

                foreground = getScorerColour(score, fgBrightness)
                background = getScorerColour(score, bgBrightness)
            }

            return this
        }

        fun getReplacementValue(table: JTable?, obj: Any?, row: Int): Any?
        {
            if (obj == null)
            {
                return null
            }

            if (obj !is Dart)
            {
                return obj
            }

            val target = table!!.getValueAt(row, 0) as Int
            return obj.getGolfScore(target)
        }

        private fun getBorderForCell(row: Int, col: Int): Border
        {
            var top = 0
            var bottom = 0
            var left = 0
            var right = 0

            if (isScoreRow(row))
            {
                top = 2
                bottom = 2
            }

            if (col == 1)
            {
                left = 2
            }

            if (col == 3)
            {
                right = 2
            }

            if (showGameId && col == 4)
            {
                right = 2
            }

            return MatteBorder(top, left, bottom, right, Color.BLACK)
        }
    }

    companion object
    {
        private const val ROUNDS_HALFWAY = 9
        private const val ROUNDS_FULL = 18
        private const val SCORE_COLUMN = 4
        private const val GAME_ID_COLUMN = 5

        fun isScoreRow(row: Int): Boolean
        {
            return row == ROUNDS_HALFWAY || row == ROUNDS_FULL + 1
        }

        fun getScorerColour(score: Int, brightness: Double): Color
        {
            val hue = when(score)
            {
                4 -> 0.1f
                3 -> 0.2f
                2 -> 0.3f
                1 -> 0.5f
                else -> 0f
            }

            return Color.getHSBColor(hue, 1f, brightness.toFloat())
        }
    }

}

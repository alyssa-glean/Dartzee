package dartzee.screen.game.scorer

import dartzee.bean.PlayerAvatar
import dartzee.bean.ScrollTableDartsGame
import dartzee.core.util.TableUtil.DefaultModel
import dartzee.db.PlayerEntity
import dartzee.utils.DartsColour
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

abstract class AbstractScorer : JPanel()
{
    var human = false
    var finishPos = -1
    var playerId = ""

    val model = DefaultModel()

    val lblName = JLabel()
    protected val layeredPane = JPanel()
    val tableScores = ScrollTableDartsGame()
    val lblResult = JLabel("X Darts (D20)")
    protected val panelNorth = JPanel()
    val lblAvatar = PlayerAvatar()
    protected val panelAvatar = JPanel()
    protected val panelSouth = JPanel()

    val playerName: String
        get() = lblName.text

    init
    {
        layout = BorderLayout(0, 0)
        preferredSize = Dimension(180, 600)

        layeredPane.layout = BorderLayout(0, 0)

        layeredPane.add(tableScores, BorderLayout.CENTER)
        add(layeredPane, BorderLayout.CENTER)
        add(panelNorth, BorderLayout.NORTH)
        panelNorth.layout = BorderLayout(0, 0)
        panelNorth.add(lblName, BorderLayout.NORTH)
        lblName.horizontalAlignment = SwingConstants.CENTER
        lblName.font = Font("Trebuchet MS", Font.PLAIN, 16)
        panelAvatar.border = EmptyBorder(5, 15, 5, 15)
        panelNorth.add(panelAvatar, BorderLayout.CENTER)
        panelAvatar.layout = BorderLayout(0, 0)
        panelAvatar.add(lblAvatar, BorderLayout.NORTH)
        add(panelSouth, BorderLayout.SOUTH)
        panelSouth.layout = BorderLayout(0, 0)
        panelSouth.add(lblResult)
        lblResult.isOpaque = true
        lblResult.font = Font("Trebuchet MS", Font.PLAIN, 22)
        lblResult.horizontalAlignment = SwingConstants.CENTER

        tableScores.setFillsViewportHeight(false)
        tableScores.setShowRowCount(false)
        tableScores.disableSorting()

        lblAvatar.readOnly = true
    }

    abstract fun getNumberOfColumns(): Int
    abstract fun initImpl(gameParams: String)

    fun init(player: PlayerEntity?, gameParams: String?)
    {
        lblName.isVisible = player != null
        lblAvatar.isVisible = player != null
        panelAvatar.isVisible = player != null

        //Sometimes we pass in a null player for the purpose of showing stats
        if (player != null)
        {
            val playerName = player.name
            lblName.text = playerName
            lblAvatar.init(player, false)
            playerId = player.rowId

            human = player.strategy == -1
        }

        lblResult.text = ""

        //TableModel
        tableScores.setRowHeight(25)
        for (i in 0 until getNumberOfColumns())
        {
            model.addColumn("")
        }
        tableScores.model = model

        gameParams?.let { initImpl(gameParams) }
    }

    fun addRow(row: Array<*>)
    {
        model.addRow(row)
        tableScores.scrollToBottom()
    }

    open fun makeEmptyRow() = arrayOfNulls<Any>(getNumberOfColumns())

    fun reset()
    {
        human = false
        lblName.text = ""
    }

    fun canBeAssigned() = isVisible && playerName.isEmpty()

    fun setSelected(selected: Boolean)
    {
        lblName.foreground = if (selected) Color.RED else Color.BLACK
        lblAvatar.setSelected(selected)
    }

    fun updateResultColourForPosition(pos: Int)
    {
        DartsColour.setFgAndBgColoursForPosition(lblResult, pos)
    }
}

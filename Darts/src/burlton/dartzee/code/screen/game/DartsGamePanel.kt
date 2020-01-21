package burlton.dartzee.code.screen.game

import burlton.dartzee.code.core.obj.HashMapList
import burlton.dartzee.code.core.util.Debug
import burlton.dartzee.code.`object`.Dart
import burlton.dartzee.code.achievements.AbstractAchievement
import burlton.dartzee.code.achievements.getBestGameAchievement
import burlton.dartzee.code.achievements.getWinAchievementRef
import burlton.dartzee.code.ai.AbstractDartsModel
import burlton.dartzee.code.bean.SliderAiSpeed
import burlton.dartzee.code.db.*
import burlton.dartzee.code.listener.DartboardListener
import burlton.dartzee.code.screen.Dartboard
import burlton.dartzee.code.screen.dartzee.DartzeeRuleCarousel
import burlton.dartzee.code.screen.dartzee.DartzeeRuleSummaryPanel
import burlton.dartzee.code.screen.game.scorer.DartsScorer
import burlton.dartzee.code.stats.PlayerSummaryStats
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.dartzee.code.utils.PREFERENCES_INT_AI_SPEED
import burlton.dartzee.code.utils.PreferenceUtil
import burlton.dartzee.code.core.util.DialogUtil
import burlton.dartzee.code.core.util.getSqlDateNow
import burlton.dartzee.code.core.util.isEndOfTime
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.sql.SQLException
import java.util.*
import javax.swing.*

abstract class DartsGamePanel<S : DartsScorer, D: Dartboard>(parent: AbstractDartsGameScreen, val gameEntity: GameEntity) :
        PanelWithScorers<S>(),
        DartboardListener,
        ActionListener,
        MouseListener
{
    protected var hmPlayerNumberToParticipant = mutableMapOf<Int, ParticipantEntity>()
    protected var hmPlayerNumberToDartsScorer = mutableMapOf<Int, S>()
    protected var hmPlayerNumberToLastRoundNumber = HashMap<Int, Int>()

    protected var totalPlayers = -1

    protected var parentWindow: AbstractDartsGameScreen? = null
    var gameTitle = ""

    //If this tab is displaying as part of a loaded match, but this game still needs loading, this will be set.
    var pendingLoad = false

    //Transitive things
    var currentPlayerNumber = 0
    var activeScorer: S = factoryScorer()
    protected var dartsThrown = ArrayList<Dart>()
    protected var currentRoundNumber = -1

    //For AI turns
    protected var cpuThread: Thread? = null

    /**
     * Screen stuff
     */
    val dartboard = factoryDartboard()
    protected val statsPanel: GameStatisticsPanel? = factoryStatsPanel()

    private val panelSouth = JPanel()
    protected val slider = SliderAiSpeed(true)
    private val panelButtons = JPanel()
    val btnConfirm = JButton("")
    val btnReset = JButton("")
    private val btnStats = JToggleButton("")
    private val btnSlider = JToggleButton("")

    private fun getLastRoundNumber() =  hmPlayerNumberToLastRoundNumber[currentPlayerNumber] ?: 0
    private fun getPlayersDesc() = if (totalPlayers == 1) "practice game" else "$totalPlayers players"
    protected fun getActiveCount() = hmPlayerNumberToParticipant.values.count{ it.isActive() }

    fun getGameId() = gameEntity.rowId

    open fun getFinishingPositionFromPlayersRemaining(): Int
    {
        if (totalPlayers == 1)
        {
            return -1
        }

        return totalPlayers - getActiveCount() + 1
    }

    protected fun getCurrentPlayerStrategy(): AbstractDartsModel?
    {
        val participant = hmPlayerNumberToParticipant[currentPlayerNumber]!!
        if (!participant.isAi())
        {
            Debug.stackTrace("Trying to get current strategy for human player: $participant")
            return null
        }

        return participant.getModel()
    }

    protected fun getCurrentPlayerId() = hmPlayerNumberToParticipant[currentPlayerNumber]!!.playerId

    private fun getOrderedParticipants() = hmPlayerNumberToParticipant.entries.sortedBy { it.key }.map { it.value }

    init
    {

        this.parentWindow = parent

        panelCenter.add(dartboard, BorderLayout.CENTER)
        dartboard.addDartboardListener(this)
        panelCenter.add(panelSouth, BorderLayout.SOUTH)
        panelSouth.layout = BorderLayout(0, 0)
        slider.value = 1000
        slider.size = Dimension(100, 200)
        slider.preferredSize = Dimension(40, 200)
        panelSouth.add(panelButtons, BorderLayout.SOUTH)
        btnConfirm.preferredSize = Dimension(80, 80)
        btnConfirm.icon = ImageIcon(javaClass.getResource("/buttons/Confirm.png"))
        btnConfirm.toolTipText = "Confirm round"
        panelButtons.add(btnConfirm)
        btnReset.preferredSize = Dimension(80, 80)
        btnReset.icon = ImageIcon(javaClass.getResource("/buttons/Reset.png"))
        btnReset.toolTipText = "Reset round"
        panelButtons.add(btnReset)
        btnStats.toolTipText = "View stats"
        btnStats.preferredSize = Dimension(80, 80)
        btnStats.icon = ImageIcon(javaClass.getResource("/buttons/stats_large.png"))

        panelButtons.add(btnStats)
        btnSlider.icon = ImageIcon(javaClass.getResource("/buttons/aiSpeed.png"))
        btnSlider.toolTipText = "AI throw speed"
        btnSlider.preferredSize = Dimension(80, 80)

        slider.orientation = SwingConstants.VERTICAL
        slider.isVisible = false

        panelButtons.add(btnSlider)

        btnConfirm.addActionListener(this)
        btnReset.addActionListener(this)
        btnStats.addActionListener(this)
        btnSlider.addActionListener(this)

        addMouseListener(this)

        if (statsPanel == null)
        {
            btnStats.isVisible = false
        }

        dartboard.renderScoreLabels = true
    }


    /**
     * Abstract methods
     */
    abstract fun doAiTurn(model: AbstractDartsModel)

    abstract fun loadDartsForParticipant(playerNumber: Int, hmRoundToDarts: HashMapList<Int, Dart>, totalRounds: Int)
    abstract fun updateVariablesForNewRound()
    abstract fun resetRoundVariables()
    abstract fun updateVariablesForDartThrown(dart: Dart)
    abstract fun shouldStopAfterDartThrown(): Boolean
    abstract fun shouldAIStop(): Boolean
    abstract fun saveDartsAndProceed()
    abstract fun factoryStatsPanel(): GameStatisticsPanel?
    abstract fun factoryDartboard(): D

    /**
     * Regular methods
     */
    fun startNewGame(players: List<PlayerEntity>)
    {
        players.forEachIndexed { ix, player ->
            val gameId = gameEntity.rowId
            val participant = ParticipantEntity.factoryAndSave(gameId, player, ix)
            addParticipant(ix, participant)

            assignScorer(player, ix)
        }

        initForAi(hasAi())
        dartboard.paintDartboardCached()

        nextTurn()
    }

    protected fun nextTurn()
    {
        activeScorer = hmPlayerNumberToDartsScorer[currentPlayerNumber]!!
        selectScorer(activeScorer)

        dartsThrown.clear()

        updateVariablesForNewRound()

        val lastRoundForThisPlayer = getLastRoundNumber()

        //Create a new round for this player
        val newRoundNo = lastRoundForThisPlayer + 1
        currentRoundNumber = newRoundNo
        hmPlayerNumberToLastRoundNumber[currentPlayerNumber] = newRoundNo

        Debug.appendBanner(activeScorer.playerName + ": Round " + newRoundNo, VERBOSE_LOGGING)

        btnReset.isEnabled = false
        btnConfirm.isEnabled = false

        btnStats.isEnabled = newRoundNo > 1

        readyForThrow()
    }

    private fun selectScorer(selectedScorer: S?)
    {
        for (scorer in scorersOrdered)
        {
            scorer.setSelected(false)
        }

        selectedScorer!!.setSelected(true)
    }

    private fun assignScorer(player: PlayerEntity, playerNumber: Int)
    {
        assignScorer(player, hmPlayerNumberToDartsScorer, playerNumber, gameEntity.gameParams)
    }

    private fun initForAi(hasAi: Boolean)
    {
        dartboard.addOverlay(Point(329, 350), slider)
        btnSlider.isVisible = hasAi

        val defaultSpd = PreferenceUtil.getIntValue(PREFERENCES_INT_AI_SPEED)
        slider.value = defaultSpd
    }


    fun initBasic(totalPlayers: Int)
    {
        this.totalPlayers = totalPlayers

        val gameNo = gameEntity.localId
        val gameDesc = gameEntity.getTypeDesc()
        gameTitle = "Game #$gameNo ($gameDesc - ${getPlayersDesc()})"

        if (statsPanel != null)
        {
            statsPanel.gameParams = gameEntity.gameParams
        }

        initScorers(totalPlayers)
    }

    fun loadGameInCatch()
    {
        try
        {
            loadGame()
        }
        catch (t: Throwable)
        {
            Debug.stackTrace(t)
            DialogUtil.showError("Failed to load Game #${gameEntity.localId}")
        }
    }

    /**
     * Called when loading up a match for the tabs that aren't visible. Just do enough so that we can generate the match
     * summary, and set a flag to say this tab needs to do a proper load if selected.
     */
    fun preLoad()
    {
        val gameId = gameEntity.rowId
        loadParticipants(gameId)

        pendingLoad = true
    }

    fun loadGame()
    {
        pendingLoad = false

        val gameId = gameEntity.rowId

        //Get the participants, sorted by Ordinal. Assign their scorers.
        loadParticipants(gameId)
        loadScoresAndCurrentPlayer(gameId)

        //Paint the dartboard
        dartboard.paintDartboardCached()

        //If the game is over, do some extra stuff to sort the screen out
        val dtFinish = gameEntity.dtFinish
        if (!isEndOfTime(dtFinish))
        {
            setGameReadOnly()
        }
        else
        {
            nextTurn()
        }
    }

    protected open fun setGameReadOnly()
    {
        dartboard.stopListening()

        if (getActiveCount() == 0)
        {
            btnSlider.isVisible = false
            btnConfirm.isVisible = false
            btnReset.isVisible = false
        }
        else
        {
            slider.isEnabled = false
            btnConfirm.isEnabled = false
            btnReset.isEnabled = false
        }

        //Default to showing the stats panel for completed games, if applicable
        if (btnStats.isVisible)
        {
            btnStats.isSelected = true
            viewStats()
        }

        updateScorersWithFinishingPositions()
    }

    protected fun updateScorersWithFinishingPositions()
    {
        hmPlayerNumberToDartsScorer.keys.forEach {
            val scorer = hmPlayerNumberToDartsScorer[it]
            val pt = hmPlayerNumberToParticipant[it]

            scorer!!.updateResultColourForPosition(pt!!.finishingPosition)
        }
    }

    /**
     * Retrieve the ordered participants and assign their scorers
     */
    private fun loadParticipants(gameId: String)
    {
        //We may have already done this in the preLoad
        if (!hmPlayerNumberToParticipant.isEmpty())
        {
            return
        }

        val whereSql = "GameId = '$gameId' ORDER BY Ordinal ASC"
        val participants = ParticipantEntity().retrieveEntities(whereSql)

        for (i in participants.indices)
        {
            val pt = participants[i]
            addParticipant(i, pt)

            val player = pt.getPlayer()
            assignScorer(player, i)
        }

        initForAi(hasAi())
    }

    /**
     * Populate the scorers and populate the current player by:
     *
     * - Finding the Max(RoundNumber) for this game
     * - Finding how many players have already completed this round, X.
     * - CurrentPlayerNumber = X % totalPlayers
     */
    private fun loadScoresAndCurrentPlayer(gameId: String)
    {
        var maxRounds = 0

        for (i in 0 until totalPlayers)
        {
            val pt = hmPlayerNumberToParticipant[i]!!
            val sql = ("SELECT drt.RoundNumber, drt.Score, drt.Multiplier, drt.PosX, drt.PosY, drt.SegmentType, drt.StartingScore"
                    + " FROM Dart drt"
                    + " WHERE drt.ParticipantId = '" + pt.rowId + "'"
                    + " AND drt.PlayerId = '" + pt.playerId + "'"
                    + " ORDER BY drt.RoundNumber, drt.Ordinal")

            val hmRoundToDarts = HashMapList<Int, Dart>()
            var lastRound = 0

            try
            {
                DatabaseUtil.executeQuery(sql).use { rs ->
                    while (rs.next())
                    {
                        val roundNumber = rs.getInt("RoundNumber")
                        val score = rs.getInt("Score")
                        val multiplier = rs.getInt("Multiplier")
                        val posX = rs.getInt("PosX")
                        val posY = rs.getInt("PosY")
                        val segmentType = rs.getInt("SegmentType")
                        val startingScore = rs.getInt("StartingScore")

                        val drt = Dart(score, multiplier, Point(posX, posY), segmentType)
                        drt.startingScore = startingScore

                        hmRoundToDarts.putInList(roundNumber, drt)

                        lastRound = roundNumber
                    }
                }
            }
            catch (sqle: SQLException)
            {
                Debug.logSqlException(sql, sqle)
                throw sqle
            }

            loadDartsForParticipant(i, hmRoundToDarts, lastRound)

            hmPlayerNumberToLastRoundNumber[i] = lastRound

            maxRounds = Math.max(maxRounds, lastRound)
        }

        setCurrentPlayer(maxRounds, gameId)
    }

    /**
     * 1) Get the MAX(Ordinal) of the person who's played the maxRounds, i.e. the last player to have a turn.
     * 2) Call into getNextPlayer(), which takes into account inactive players.
     */
    private fun setCurrentPlayer(maxRounds: Int, gameId: String)
    {
        if (maxRounds == 0)
        {
            //The game literally hasn't started yet. No one has completed a round.
            Debug.append("MaxRounds = 0, so setting CurrentPlayerNumber = 0 as game hasn't started.")
            currentPlayerNumber = 0
            return
        }

        val sb = StringBuilder()

        sb.append("SELECT MAX(pt.Ordinal) ")
        sb.append(" FROM Dart drt, Participant pt")
        sb.append(" WHERE drt.ParticipantId = pt.RowId")
        sb.append(" AND drt.PlayerId = pt.PlayerId")
        sb.append(" AND drt.RoundNumber = ")
        sb.append(maxRounds)
        sb.append(" AND pt.GameId = '")
        sb.append(gameId)
        sb.append("'")

        val lastPlayerNumber = DatabaseUtil.executeQueryAggregate(sb)
        currentPlayerNumber = getNextPlayerNumber(lastPlayerNumber)

        Debug.append("MaxRounds = $maxRounds, CurrentPlayerNumber = $currentPlayerNumber")
    }

    fun allPlayersFinished()
    {
        Debug.append("All players now finished.", VERBOSE_LOGGING)

        if (!gameEntity.isFinished())
        {
            gameEntity.dtFinish = getSqlDateNow()
            gameEntity.saveToDatabase()
        }

        dartboard.stopListening()

        val participants = hmPlayerNumberToParticipant.values
        for (pt in participants)
        {
            val playerId = pt.playerId
            PlayerSummaryStats.resetPlayerStats(playerId, gameEntity.gameType)
        }
    }

    /**
     * Should I stop throwing?
     *
     * Default behaviour for if window has been closed, with extensible hook (e.g. in X01 where an AI can be paused).
     */
    private fun shouldAiStopThrowing(): Boolean
    {
        if (!parentWindow!!.isVisible)
        {
            Debug.append("Game window has been closed, stopping throwing.")
            return true
        }

        return shouldAIStop()
    }

    protected fun getNextPlayerNumber(currentPlayerNumber: Int): Int
    {
        if (getActiveCount() == 0)
        {
            return currentPlayerNumber
        }

        var candidate = (currentPlayerNumber + 1) % totalPlayers
        while (!isActive(candidate))
        {
            candidate = (candidate + 1) % totalPlayers
        }

        return candidate
    }

    private fun hasAi() = hmPlayerNumberToParticipant.values.any { it.isAi() }

    private fun isActive(playerNumber: Int): Boolean
    {
        val participant = hmPlayerNumberToParticipant[playerNumber]
        return participant!!.isActive()
    }

    fun fireAppearancePreferencesChanged()
    {
        for (scorer in scorersOrdered)
        {
            scorer.repaint()
        }
    }

    protected open fun handlePlayerFinish(): Int
    {
        val participant = hmPlayerNumberToParticipant[currentPlayerNumber]!!

        val finishingPosition = getFinishingPositionFromPlayersRemaining()
        val numberOfDarts = activeScorer.getTotalScore()

        participant.finishingPosition = finishingPosition
        participant.finalScore = numberOfDarts
        participant.dtFinished = getSqlDateNow()
        participant.saveToDatabase()

        val playerId = participant.playerId
        PlayerSummaryStats.resetPlayerStats(playerId, gameEntity.gameType)

        updateAchievementsForFinish(playerId, finishingPosition, numberOfDarts)

        return finishingPosition
    }

    open fun updateAchievementsForFinish(playerId: String, finishingPosition: Int, score: Int)
    {
        if (finishingPosition == 1)
        {
            val achievementRef = getWinAchievementRef(gameEntity.gameType)
            AchievementEntity.incrementAchievement(achievementRef, playerId, gameEntity.rowId, 1)
        }

        //Update the 'best game' achievement
        val aa = getBestGameAchievement(gameEntity.gameType) ?: return
        val gameParams = aa.gameParams
        if (gameParams == gameEntity.gameParams)
        {
            AchievementEntity.updateAchievement(aa.achievementRef, playerId, gameEntity.rowId, score)
        }
    }

    override fun dartThrown(dart: Dart)
    {
        Debug.append("Hit $dart", VERBOSE_LOGGING)

        dartsThrown.add(dart)
        activeScorer.addDart(dart)

        //We've clicked on the dartboard, so dismiss the slider
        if (activeScorer.human)
        {
            dismissSlider()
        }

        //If there are any specific variables we need to update (e.g. current score for X01), do it now
        updateVariablesForDartThrown(dart)

        doAnimations(dart)

        //Enable both of these
        btnReset.isEnabled = activeScorer.human
        if (!mustContinueThrowing())
        {
            btnConfirm.isEnabled = activeScorer.human
        }

        //If we've thrown three or should stop for other reasons (bust in X01), then stop throwing
        if (shouldStopAfterDartThrown())
        {
            stopThrowing()
        }
        else
        {
            //Fine, just carry on
            readyForThrow()
        }
    }

    private fun doAnimations(dart: Dart)
    {
        if (dart.multiplier == 0 && shouldAnimateMiss(dart))
        {
            doMissAnimation()
        }
        else if (dart.getTotal() == 50)
        {
            dartboard.doBull()
        }
    }

    protected open fun shouldAnimateMiss(dart: Dart): Boolean
    {
        return true
    }


    protected open fun doMissAnimation()
    {
        dartboard.doBadMiss()
    }

    protected fun stopThrowing()
    {
        if (activeScorer.human)
        {
            dartboard.stopListening()
        }
        else
        {
            Thread.sleep(slider.value.toLong())

            // If we've been told to pause then we're going to do a reset and not save anything
            if (!shouldAiStopThrowing())
            {
                SwingUtilities.invokeLater { confirmRound() }
            }
        }
    }

    private fun confirmRound()
    {
        btnConfirm.isEnabled = false
        btnReset.isEnabled = false

        dartboard.clearDarts()
        activeScorer.confirmCurrentRound()

        saveDartsAndProceed()
    }

    protected fun resetRound()
    {
        resetRoundVariables()

        dartboard.clearDarts()
        activeScorer.clearRound(currentRoundNumber)
        activeScorer.updatePlayerResult()
        dartsThrown.clear()

        //If we're resetting, disable the buttons
        btnConfirm.isEnabled = false
        btnReset.isEnabled = false

        //Might need to re-enable the dartboard for listening if we're a human player
        val human = activeScorer.human
        dartboard.listen(human)
    }

    /**
     * Loop through the darts thrown, saving them to the database.
     */
    protected fun saveDartsToDatabase()
    {
        val pt = hmPlayerNumberToParticipant[currentPlayerNumber]!!
        val darts = ArrayList<DartEntity>()
        for (i in dartsThrown.indices)
        {
            val dart = dartsThrown[i]
            darts.add(DartEntity.factory(dart, pt.playerId, pt.rowId, currentRoundNumber, i + 1, dart.startingScore))
        }

        BulkInserter.insert(darts)
    }

    open fun readyForThrow()
    {
        if (activeScorer.human)
        {
            //Human player
            dartboard.ensureListening()
        }
        else
        {
            //AI
            dartboard.stopListening()

            cpuThread = Thread(DelayedOpponentTurn(), "Cpu-Thread-" + gameEntity.localId)
            cpuThread!!.start()
        }
    }

    protected open fun mustContinueThrowing() = false

    override fun actionPerformed(arg0: ActionEvent)
    {
        val source = arg0.source
        if (source !== btnSlider)
        {
            btnSlider.isSelected = false
            slider.isVisible = false
        }

        when (source)
        {
            btnReset -> {
                Debug.append("Reset pressed.")
                resetRound()
                readyForThrow()
            }
            btnConfirm -> confirmRound()
            btnStats -> viewStats()
            btnSlider -> toggleSlider()
        }
    }

    private fun toggleSlider()
    {
        slider.isVisible = btnSlider.isSelected

        if (btnStats.isSelected)
        {
            btnStats.isSelected = false
            viewStats()
        }
    }

    private fun viewStats()
    {
        if (btnStats.isSelected)
        {
            panelCenter.remove(dartboard)
            panelCenter.add(statsPanel, BorderLayout.CENTER)

            statsPanel!!.showStats(getOrderedParticipants())
        }
        else
        {
            panelCenter.remove(statsPanel)
            panelCenter.add(dartboard, BorderLayout.CENTER)
        }

        panelCenter.revalidate()
        panelCenter.repaint()
    }

    private fun addParticipant(playerNumber: Int, participant: ParticipantEntity)
    {
        hmPlayerNumberToParticipant[playerNumber] = participant

        if (parentWindow is DartsMatchScreen)
        {
            (parentWindow as DartsMatchScreen).addParticipant(gameEntity.localId, participant)
        }

    }

    fun achievementUnlocked(playerId: String, achievement: AbstractAchievement)
    {
        scorersOrdered.find { it.playerId === playerId }?.achievementUnlocked(achievement)
    }

    private fun dismissSlider()
    {
        btnSlider.isSelected = false
        toggleSlider()
    }

    fun disableInputButtons()
    {
        btnConfirm.isEnabled = false
        btnReset.isEnabled = false
    }

    /**
     * MouseListener
     */
    override fun mouseClicked(e: MouseEvent)
    {
        if (e.source !== slider)
        {
            dismissSlider()
        }
    }
    override fun mouseEntered(e: MouseEvent){}
    override fun mouseExited(e: MouseEvent){}
    override fun mousePressed(e: MouseEvent){}
    override fun mouseReleased(e: MouseEvent){}

    internal inner class DelayedOpponentTurn : Runnable
    {
        override fun run()
        {
            Thread.sleep(slider.value.toLong())

            if (shouldAiStopThrowing())
            {
                return
            }

            val model = getCurrentPlayerStrategy()!!
            doAiTurn(model)
        }
    }

    companion object
    {
        const val VERBOSE_LOGGING = false

        fun factory(parent: AbstractDartsGameScreen, game: GameEntity): DartsGamePanel<out DartsScorer, out Dartboard>
        {
            return when (game.gameType)
            {
                GAME_TYPE_X01 -> GamePanelX01(parent, game)
                GAME_TYPE_GOLF -> GamePanelGolf(parent, game)
                GAME_TYPE_ROUND_THE_CLOCK -> GamePanelRoundTheClock(parent, game)
                GAME_TYPE_DARTZEE -> constructGamePanelDartzee(parent, game)
                else -> GamePanelX01(parent, game)
            }
        }

        private fun constructGamePanelDartzee(parent: AbstractDartsGameScreen, game: GameEntity): GamePanelDartzee
        {
            val dtos = DartzeeRuleEntity().retrieveForGame(game.rowId).map { it.toDto() }
            val summaryPanel = DartzeeRuleSummaryPanel(DartzeeRuleCarousel(dtos))

            return GamePanelDartzee(parent, game, dtos, summaryPanel)
        }
    }
}

package dartzee.screen.game

import dartzee.`object`.Dart
import dartzee.`object`.SegmentType
import dartzee.achievements.AbstractAchievement
import dartzee.achievements.getBestGameAchievement
import dartzee.achievements.getWinAchievementRef
import dartzee.ai.DartsAiModel
import dartzee.bean.SliderAiSpeed
import dartzee.core.obj.HashMapList
import dartzee.core.util.*
import dartzee.db.*
import dartzee.game.GameType
import dartzee.game.state.AbstractPlayerState
import dartzee.listener.DartboardListener
import dartzee.screen.Dartboard
import dartzee.screen.game.dartzee.DartzeeRuleCarousel
import dartzee.screen.game.dartzee.DartzeeRuleSummaryPanel
import dartzee.screen.game.dartzee.GamePanelDartzee
import dartzee.screen.game.golf.GamePanelGolf
import dartzee.screen.game.rtc.GamePanelRoundTheClock
import dartzee.screen.game.scorer.AbstractDartsScorer
import dartzee.screen.game.x01.GamePanelX01
import dartzee.utils.InjectedThings.logger
import dartzee.utils.InjectedThings.mainDatabase
import dartzee.utils.PREFERENCES_INT_AI_SPEED
import dartzee.utils.PreferenceUtil
import dartzee.utils.ResourceCache.ICON_STATS_LARGE
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.sql.SQLException
import javax.swing.*

abstract class DartsGamePanel<S : AbstractDartsScorer<PlayerState>, D: Dartboard, PlayerState: AbstractPlayerState<PlayerState>>(
        protected val parentWindow: AbstractDartsGameScreen,
        val gameEntity: GameEntity,
        protected val totalPlayers: Int) : PanelWithScorers<S>(), DartboardListener, ActionListener, MouseListener
{
    private val hmPlayerNumberToState = mutableMapOf<Int, PlayerState>()
    private val hmPlayerNumberToScorer = mutableMapOf<Int, S>()

    val gameTitle = makeGameTitle()

    //Transitive things
    var currentPlayerNumber = 0
    protected var currentRoundNumber = -1

    //For AI turns
    protected var cpuThread: Thread? = null

    /**
     * Screen stuff
     */
    val dartboard = factoryDartboard()
    private val statsPanel = factoryStatsPanel(gameEntity.gameParams)

    private val panelSouth = JPanel()
    protected val slider = SliderAiSpeed(true)
    private val panelButtons = JPanel()
    val btnConfirm = JButton("")
    val btnReset = JButton("")
    private val btnStats = JToggleButton("")
    private val btnSlider = JToggleButton("")

    private fun getPlayersDesc() = if (totalPlayers == 1) "practice game" else "$totalPlayers players"
    protected fun getActiveCount() = getParticipants().count{ it.isActive() }

    fun getGameId() = gameEntity.rowId

    open fun getFinishingPositionFromPlayersRemaining(): Int
    {
        if (totalPlayers == 1)
        {
            return -1
        }

        return totalPlayers - getActiveCount() + 1
    }

    protected fun getCurrentPlayerStrategy(): DartsAiModel
    {
        val participant = getCurrentParticipant()
        return participant.getModel()
    }

    /**
     * Stuff that will ultimately get refactored off into a GameState thingy
     */
    fun getPlayerStates() = hmPlayerNumberToState.getSortedValues()
    protected fun getParticipants() = hmPlayerNumberToState.entries.sortedBy { it.key }.map { it.value.pt }
    protected fun getCurrentPlayerId() = getCurrentParticipant().playerId
    protected fun getCurrentPlayerState() = getPlayerState(currentPlayerNumber)
    protected fun getPlayerState(playerNumber: Int) = hmPlayerNumberToState[playerNumber]!!
    protected fun getParticipant(playerNumber: Int) = getPlayerState(playerNumber).pt
    private fun getCurrentParticipant() = getCurrentPlayerState().pt
    fun getDartsThrown() = getCurrentPlayerState().currentRound
    fun dartsThrownCount() = getDartsThrown().size

    protected fun addState(playerNumber: Int, state: PlayerState, scorer: S) {
        hmPlayerNumberToState[playerNumber] = state
        hmPlayerNumberToScorer[playerNumber] = scorer
    }

    protected fun getCurrentScorer() = hmPlayerNumberToScorer.getValue(currentPlayerNumber)
    protected fun getPlayerNumberForScorer(scorer: S): Int = scorersOrdered.indexOf(scorer)

    init
    {
        panelCenter.add(dartboard, BorderLayout.CENTER)
        dartboard.addDartboardListener(this)
        dartboard.renderDarts = true
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
        btnStats.icon = ICON_STATS_LARGE

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

        dartboard.renderScoreLabels = true

        initScorers(totalPlayers)
    }


    /**
     * Abstract methods
     */
    abstract fun factoryState(pt: ParticipantEntity): PlayerState
    abstract fun doAiTurn(model: DartsAiModel)

    abstract fun shouldStopAfterDartThrown(): Boolean
    abstract fun shouldAIStop(): Boolean
    abstract fun saveDartsAndProceed()
    abstract fun factoryStatsPanel(gameParams: String): AbstractGameStatisticsPanel<PlayerState>
    abstract fun factoryDartboard(): D

    open fun updateVariablesForDartThrown(dart: Dart) {}

    /**
     * Regular methods
     */
    fun startNewGame(players: List<PlayerEntity>)
    {
        players.forEachIndexed { ix, player ->
            val gameId = gameEntity.rowId
            val participant = ParticipantEntity.factoryAndSave(gameId, player, ix)
            addParticipant(participant)

            val scorer = assignScorer(player)
            val state = factoryState(participant)
            state.addListener(scorer)
            addState(ix, state, scorer)
        }

        initForAi(hasAi())
        dartboard.paintDartboardCached()

        nextTurn()
    }

    protected fun nextTurn()
    {
        updateActivePlayer()

        //Create a new round for this player
        currentRoundNumber = getCurrentPlayerState().currentRoundNumber()

        btnReset.isEnabled = false
        btnConfirm.isEnabled = false

        btnStats.isEnabled = currentRoundNumber > 1

        readyForThrow()
    }

    protected fun updateActivePlayer()
    {
        hmPlayerNumberToState.values.forEach {
            it.updateActive(it == getCurrentPlayerState())
        }
    }

    private fun initForAi(hasAi: Boolean)
    {
        dartboard.addOverlay(Point(329, 350), slider)
        btnSlider.isVisible = hasAi

        val defaultSpd = PreferenceUtil.getIntValue(PREFERENCES_INT_AI_SPEED)
        slider.value = defaultSpd
    }

    private fun makeGameTitle(): String
    {
        val gameNo = gameEntity.localId
        val gameDesc = gameEntity.getTypeDesc()
        return "Game #$gameNo ($gameDesc - ${getPlayersDesc()})"
    }

    fun loadGame()
    {
        val gameId = gameEntity.rowId

        //Get the participants, sorted by Ordinal. Assign their scorers.
        loadParticipants(gameId)
        loadScoresAndCurrentPlayer(gameId)

        //Paint the dartboard - always do this, in case of resuming with stats open
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
    }

    /**
     * Retrieve the ordered participants and assign their scorers
     */
    private fun loadParticipants(gameId: String)
    {
        val whereSql = "GameId = '$gameId' ORDER BY Ordinal ASC"
        val participants = ParticipantEntity().retrieveEntities(whereSql)

        for (i in participants.indices)
        {
            val pt = participants[i]
            addParticipant(pt)

            val scorer = assignScorer(pt.getPlayer())
            val state = factoryState(pt)
            state.addListener(scorer)
            addState(i, state, scorer)
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
            val pt = getParticipant(i)
            val sql = ("SELECT drt.RoundNumber, drt.Score, drt.Multiplier, drt.PosX, drt.PosY, drt.SegmentType, drt.StartingScore"
                    + " FROM Dart drt"
                    + " WHERE drt.ParticipantId = '" + pt.rowId + "'"
                    + " AND drt.PlayerId = '" + pt.playerId + "'"
                    + " ORDER BY drt.RoundNumber, drt.Ordinal")

            val hmRoundToDarts = HashMapList<Int, Dart>()
            var lastRound = 0

            try
            {
                mainDatabase.executeQuery(sql).use { rs ->
                    while (rs.next())
                    {
                        val roundNumber = rs.getInt("RoundNumber")
                        val score = rs.getInt("Score")
                        val multiplier = rs.getInt("Multiplier")
                        val posX = rs.getInt("PosX")
                        val posY = rs.getInt("PosY")
                        val segmentType = SegmentType.valueOf(rs.getString("SegmentType"))
                        val startingScore = rs.getInt("StartingScore")

                        val drt = Dart(score, multiplier, Point(posX, posY), segmentType)
                        drt.startingScore = startingScore
                        drt.roundNumber = roundNumber

                        hmRoundToDarts.putInList(roundNumber, drt)

                        lastRound = roundNumber
                    }
                }
            }
            catch (sqle: SQLException)
            {
                logger.logSqlException(sql, sql, sqle)
                throw sqle
            }

            val state = getPlayerState(i)
            hmRoundToDarts.getSortedValues().forEach {
                state.addLoadedRound(it)
            }

            loadAdditionalEntities(state)

            maxRounds = maxOf(maxRounds, lastRound)
        }

        setCurrentPlayer(maxRounds, gameId)
    }
    protected open fun loadAdditionalEntities(state: PlayerState) {}

    /**
     * 1) Get the MAX(Ordinal) of the person who's played the maxRounds, i.e. the last player to have a turn.
     * 2) Call into getNextPlayer(), which takes into account inactive players.
     */
    private fun setCurrentPlayer(maxRounds: Int, gameId: String)
    {
        if (maxRounds == 0)
        {
            //The game literally hasn't started yet. No one has completed a round.
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

        val lastPlayerNumber = mainDatabase.executeQueryAggregate(sb)
        currentPlayerNumber = getNextPlayerNumber(lastPlayerNumber)
    }

    fun allPlayersFinished()
    {
        if (!gameEntity.isFinished())
        {
            gameEntity.dtFinish = getSqlDateNow()
            gameEntity.saveToDatabase()
        }

        dartboard.stopListening()
    }



    /**
     * Should I stop throwing?
     *
     * Default behaviour for if window has been closed, with extensible hook (e.g. in X01 where an AI can be paused).
     */
    private fun shouldAiStopThrowing(): Boolean
    {
        if (!parentWindow.isVisible)
        {
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

    private fun hasAi() = getParticipants().any { it.isAi() }

    private fun isActive(playerNumber: Int) = getParticipant(playerNumber).isActive()

    fun fireAppearancePreferencesChanged()
    {
        for (scorer in scorersOrdered)
        {
            scorer.repaint()
        }
    }

    protected fun handlePlayerFinish(): Int
    {
        val state = getCurrentPlayerState()
        val finishingPosition = getFinishingPositionFromPlayersRemaining()
        val numberOfDarts = state.getScoreSoFar()
        state.participantFinished(finishingPosition, numberOfDarts)

        updateAchievementsForFinish(getCurrentPlayerId(), finishingPosition, numberOfDarts)

        return finishingPosition
    }

    open fun updateAchievementsForFinish(playerId: String, finishingPosition: Int, score: Int)
    {
        if (finishingPosition == 1)
        {
            val achievementRef = getWinAchievementRef(gameEntity.gameType)
            AchievementEntity.insertAchievement(achievementRef, playerId, gameEntity.rowId, "$score")
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
        dart.roundNumber = currentRoundNumber

        getCurrentPlayerState().dartThrown(dart)

        //If there are any specific variables we need to update (e.g. current score for X01), do it now
        updateVariablesForDartThrown(dart)

        //We've clicked on the dartboard, so dismiss the slider
        if (getCurrentPlayerState().isHuman())
        {
            dismissSlider()
        }

        doAnimations(dart)

        //Enable both of these
        btnReset.isEnabled = getCurrentPlayerState().isHuman()
        if (!mustContinueThrowing())
        {
            btnConfirm.isEnabled = getCurrentPlayerState().isHuman()
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
        if (getCurrentPlayerState().isHuman())
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

        saveDartsAndProceed()
    }

    protected fun resetRound()
    {
        dartboard.clearDarts()
        getCurrentPlayerState().resetRound()

        //If we're resetting, disable the buttons
        btnConfirm.isEnabled = false
        btnReset.isEnabled = false

        //Might need to re-enable the dartboard for listening if we're a human player
        val human = getCurrentPlayerState().isHuman()
        dartboard.listen(human)
    }

    /**
     * Commit round to current player state and the database
     */
    protected fun commitRound()
    {
        getCurrentPlayerState().commitRound()
    }

    open fun readyForThrow()
    {
        if (getCurrentPlayerState().isHuman())
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

            statsPanel.showStats(getPlayerStates())
        }
        else
        {
            panelCenter.remove(statsPanel)
            panelCenter.add(dartboard, BorderLayout.CENTER)
        }

        panelCenter.revalidate()
        panelCenter.repaint()
    }

    private fun addParticipant(participant: ParticipantEntity)
    {
        if (parentWindow is DartsMatchScreen<*>)
        {
            parentWindow.addParticipant(gameEntity.localId, participant)
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

            val model = getCurrentPlayerStrategy()
            doAiTurn(model)
        }
    }

    companion object
    {
        fun factory(parent: AbstractDartsGameScreen, game: GameEntity, totalPlayers: Int) =
            when (game.gameType)
            {
                GameType.X01 -> GamePanelX01(parent, game, totalPlayers)
                GameType.GOLF -> GamePanelGolf(parent, game, totalPlayers)
                GameType.ROUND_THE_CLOCK -> GamePanelRoundTheClock(parent, game, totalPlayers)
                GameType.DARTZEE -> constructGamePanelDartzee(parent, game, totalPlayers)
            }

        fun constructGamePanelDartzee(parent: AbstractDartsGameScreen, game: GameEntity, totalPlayers: Int): GamePanelDartzee
        {
            val dtos = DartzeeRuleEntity().retrieveForGame(game.rowId).map { it.toDto() }
            val summaryPanel = DartzeeRuleSummaryPanel(DartzeeRuleCarousel(dtos))

            return GamePanelDartzee(parent, game, totalPlayers, dtos, summaryPanel)
        }
    }
}

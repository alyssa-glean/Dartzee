package dartzee.screen.ai

import dartzee.core.util.Debug
import dartzee.`object`.DartsClient
import dartzee.ai.AbstractDartsModel
import dartzee.ai.AbstractDartsSimulation
import dartzee.ai.DartsSimulationGolf
import dartzee.ai.DartsSimulationX01
import dartzee.db.*
import dartzee.screen.Dartboard
import dartzee.screen.ScreenCache
import dartzee.screen.stats.player.PlayerStatisticsScreen
import dartzee.stats.GameWrapper
import dartzee.core.bean.NumberField
import dartzee.core.bean.RadioButtonPanel
import dartzee.core.screen.ProgressDialog
import dartzee.core.screen.SimpleDialog
import dartzee.core.util.DialogUtil
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Window
import javax.swing.*

class AISimulationSetup constructor(private val player: PlayerEntity,
                                    private var model: AbstractDartsModel? = null,
                                    private val modal: Boolean = false) : SimpleDialog()
{
    private val panelCenter = JPanel()
    private val lblGameMode = JLabel("Game Mode")
    private val panel_1 = RadioButtonPanel()
    private val rdbtn501 = JRadioButton("501")
    private val rdbtnGolfHoles = JRadioButton("Golf (18 Holes)")
    private val lblNumberOfGames = JLabel("Number of games")
    private val nfNumberOfGames = NumberField(100, 100000)

    init
    {
        title = "Simulation Options"
        setSize(400, 160)
        setLocationRelativeTo(ScreenCache.mainScreen)
        isModal = true

        nfNumberOfGames.columns = 10
        contentPane.add(panelCenter, BorderLayout.CENTER)
        panelCenter.layout = MigLayout("", "[][]", "[][]")
        panelCenter.add(lblGameMode, "cell 0 0")
        val flowLayout = panel_1.layout as FlowLayout
        flowLayout.alignment = FlowLayout.LEFT


        panelCenter.add(panel_1, "cell 1 0,grow")

        panel_1.add(rdbtn501)

        panel_1.add(rdbtnGolfHoles)

        panelCenter.add(lblNumberOfGames, "cell 0 1,alignx trailing")

        panelCenter.add(nfNumberOfGames, "cell 1 1,growx")
        nfNumberOfGames.value = 1000
    }

    override fun okPressed()
    {
        //Do the simulation...
        if (model == null)
        {
            model = player.getModel()
        }

        val dartboard = Dartboard(500, 500)
        dartboard.setSimulation(true) //Don't do animations etc
        dartboard.paintDartboard()

        val sim = factorySimulationForSelection(dartboard)
        runSimulationInSeparateThread(sim)
    }

    private fun factorySimulationForSelection(dartboard: Dartboard): AbstractDartsSimulation
    {
        return when (panel_1.selection)
        {
            rdbtn501 -> DartsSimulationX01(dartboard, player, model!!)
            else -> DartsSimulationGolf(dartboard, player, model!!)
        }
    }

    private fun runSimulationInSeparateThread(sim: AbstractDartsSimulation)
    {
        val hmGameIdToWrapper = mutableMapOf<Long, GameWrapper>()
        val numberOfGames = nfNumberOfGames.getNumber()
        val simulationRunnable = Runnable { runSimulation(sim, hmGameIdToWrapper, numberOfGames) }

        val t = Thread(simulationRunnable, "Simulation Thread")
        t.start()
    }

    private fun runSimulation(sim: AbstractDartsSimulation, hmGameIdToWrapper: MutableMap<Long, GameWrapper>, numberOfGames: Int)
    {
        val startTime = System.currentTimeMillis()
        val dialog = ProgressDialog.factory("Simulating games...", "games remaining", numberOfGames)
        dialog.showCancel(true)
        dialog.setVisibleLater()

        Debug.appendBanner("Starting simulation for $numberOfGames games")

        for (i in 1..numberOfGames)
        {
            try
            {
                val wrapper = sim.simulateGame((-i).toLong())
                hmGameIdToWrapper[java.lang.Long.valueOf((-i).toLong())] = wrapper
                dialog.incrementProgressLater()

                Debug.logProgress(i.toLong(), numberOfGames.toLong(), 10)

                if (dialog.cancelPressed())
                {
                    Debug.append("Simulation Cancelled")
                    hmGameIdToWrapper.clear()
                    dialog.disposeLater()
                    return
                }
            }
            catch (t: Throwable)
            {
                hmGameIdToWrapper.clear()
                dialog.disposeLater()
                Debug.stackTrace(t)
                DialogUtil.showErrorLater("A serious problem has occurred with the simulation.")
            }

        }

        val totalTime = System.currentTimeMillis() - startTime
        Debug.appendBanner("Simulation complete - Took $totalTime millis")
        dialog.disposeLater()

        if (!hmGameIdToWrapper.isEmpty())
        {
            //The simulation finished successfully, so show it
            SwingUtilities.invokeLater { simulationFinished(hmGameIdToWrapper, sim.gameType) }
        }
    }

    private fun simulationFinished(hmGameIdToWrapper: Map<Long, GameWrapper>, gameType: Int)
    {
        if (DartsClient.devMode)
        {
            val ans = DialogUtil.showQuestion("Save real entities?")
            handleSavingEntities(ans == JOptionPane.YES_OPTION, hmGameIdToWrapper)
        }

        val title = "Simulation Results - ${player.name} (${hmGameIdToWrapper.size} games)"
        val parentWindow = getParentWindowForResults(title)
        parentWindow.setSize(1200, 800)
        parentWindow.layout = BorderLayout(0, 0)

        val scrn = PlayerStatisticsScreen()
        scrn.setVariables(gameType, player)
        scrn.initFake(hmGameIdToWrapper)
        parentWindow.add(scrn)
        parentWindow.isVisible = true

        dispose()
    }

    private fun handleSavingEntities(save: Boolean, hmGameIdToWrapper: Map<Long, GameWrapper>)
    {
        val wrappers = hmGameIdToWrapper.values
        if (save)
        {
            val games = mutableListOf<GameEntity>()
            val participants = mutableListOf<ParticipantEntity>()
            val darts = mutableListOf<DartEntity>()

            wrappers.forEach{
                games.add(it.gameEntity!!)
                participants.add(it.participantEntity!!)

                darts += it.dartEntities

                it.clearEntities()
            }

            BulkInserter.insert(games)
            BulkInserter.insert(participants)
            BulkInserter.insert(darts)
        }
    }

    private fun getParentWindowForResults(title: String): Window
    {
        if (modal)
        {
            val dlg = JDialog()
            dlg.isModal = true
            dlg.title = title
            return dlg
        }

        return JFrame(title)
    }
}

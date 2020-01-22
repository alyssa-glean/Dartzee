package dartzee.screen.ai

import dartzee.`object`.Dart
import dartzee.ai.AbstractDartsModel
import dartzee.bean.SpinnerSingleSelector
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

class AIConfigurationSubPanelX01 : AbstractAIConfigurationSubPanel(), ActionListener
{
    var hmScoreToDart = mutableMapOf<Int, Dart>()

    val spinnerScoringDart = SpinnerSingleSelector()
    private val btnConfigureSetupDarts = JButton("Configure Setup...")
    val chckbxMercyRule = JCheckBox("Mercy Rule")
    val lblWhenScoreLess = JLabel("when score less than")
    val spinnerMercyThreshold = JSpinner()

    init
    {
        border = null
        layout = null
        val lblScoringDart = JLabel("Scoring Dart")
        lblScoringDart.setBounds(20, 20, 120, 25)
        add(lblScoringDart)
        spinnerScoringDart.setBounds(140, 20, 50, 25)
        add(spinnerScoringDart)
        btnConfigureSetupDarts.setBounds(20, 92, 150, 25)
        add(btnConfigureSetupDarts)
        chckbxMercyRule.setBounds(20, 55, 120, 25)
        add(chckbxMercyRule)
        spinnerMercyThreshold.setBounds(272, 55, 50, 25)
        spinnerMercyThreshold.model = SpinnerNumberModel(10, 4, 40, 2)
        add(spinnerMercyThreshold)
        lblWhenScoreLess.setBounds(140, 55, 118, 24)
        add(lblWhenScoreLess)

        //Listeners
        btnConfigureSetupDarts.addActionListener(this)
        chckbxMercyRule.addActionListener(this)
    }

    override fun valid() = true

    override fun populateModel(model: AbstractDartsModel)
    {
        model.hmScoreToDart = hmScoreToDart
        model.scoringDart = spinnerScoringDart.value as Int

        val mercyRule = chckbxMercyRule.isSelected
        if (mercyRule)
        {
            val mercyThreshold = spinnerMercyThreshold.value as Int
            model.mercyThreshold = mercyThreshold
        }
    }

    override fun initialiseFromModel(model: AbstractDartsModel)
    {
        spinnerScoringDart.value = model.scoringDart

        val mercyThreshold = model.mercyThreshold
        val mercyRule = mercyThreshold > -1
        chckbxMercyRule.isSelected = mercyRule
        spinnerMercyThreshold.isEnabled = mercyRule
        lblWhenScoreLess.isEnabled = mercyRule

        spinnerMercyThreshold.value = if (mercyRule) mercyThreshold else 10

        hmScoreToDart = model.hmScoreToDart
    }

    override fun actionPerformed(arg0: ActionEvent)
    {
        when (arg0.source)
        {
            btnConfigureSetupDarts -> AISetupConfigurationDialog.configureSetups(hmScoreToDart)
            chckbxMercyRule -> {
                val mercyRule = chckbxMercyRule.isSelected
                spinnerMercyThreshold.isEnabled = mercyRule
                lblWhenScoreLess.isEnabled = mercyRule
            }
        }
    }
}

package dartzee.screen.ai

import dartzee.ai.DartsAiModel
import dartzee.bean.SpinnerSingleSelector
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class AIConfigurationSubPanelX01 : AbstractAIConfigurationSubPanel(), ActionListener {
    val spinnerScoringDart = SpinnerSingleSelector()
    val chckbxMercyRule = JCheckBox("Mercy Rule")
    val lblWhenScoreLess = JLabel("when score less than")
    val spinnerMercyThreshold = JSpinner()

    init {
        border = null
        layout = null
        val lblScoringDart = JLabel("Scoring Dart")
        lblScoringDart.setBounds(20, 20, 120, 25)
        add(lblScoringDart)
        spinnerScoringDart.setBounds(140, 20, 50, 25)
        add(spinnerScoringDart)
        chckbxMercyRule.setBounds(20, 55, 120, 25)
        add(chckbxMercyRule)
        spinnerMercyThreshold.setBounds(272, 55, 50, 25)
        spinnerMercyThreshold.model = SpinnerNumberModel(10, 4, 40, 2)
        add(spinnerMercyThreshold)
        lblWhenScoreLess.setBounds(140, 55, 118, 24)
        add(lblWhenScoreLess)

        // Listeners
        chckbxMercyRule.addActionListener(this)
    }

    override fun populateModel(model: DartsAiModel): DartsAiModel {
        return model.copy(
            scoringDart = spinnerScoringDart.value as Int,
            mercyThreshold =
                if (chckbxMercyRule.isSelected) spinnerMercyThreshold.value as Int else null
        )
    }

    override fun initialiseFromModel(model: DartsAiModel) {
        spinnerScoringDart.value = model.scoringDart

        val mercyThreshold = model.mercyThreshold
        val mercyRule = mercyThreshold != null
        chckbxMercyRule.isSelected = mercyRule
        spinnerMercyThreshold.isEnabled = mercyRule
        lblWhenScoreLess.isEnabled = mercyRule

        spinnerMercyThreshold.value = if (mercyRule) mercyThreshold else 10
    }

    override fun actionPerformed(arg0: ActionEvent) {
        when (arg0.source) {
            chckbxMercyRule -> {
                val mercyRule = chckbxMercyRule.isSelected
                spinnerMercyThreshold.isEnabled = mercyRule
                lblWhenScoreLess.isEnabled = mercyRule
            }
        }
    }
}

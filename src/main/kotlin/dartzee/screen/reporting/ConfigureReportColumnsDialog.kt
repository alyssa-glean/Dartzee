package dartzee.screen.reporting

import dartzee.core.screen.SimpleDialog
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

private val CONFIGURABLE_COLUMNS = listOf("Type", "Players", "Start Date", "Finish Date", "Match")

class ConfigureReportColumnsDialog : SimpleDialog() {
    private val hmColumnNameToCheckBox = mutableMapOf<String, JCheckBox>()

    private val panel = JPanel()

    init {
        setSize(301, 251)
        title = "Configure Columns"
        isModal = true

        contentPane.add(panel, BorderLayout.CENTER)
        panel.layout = MigLayout("", "[][][][]", "[][][]")

        init()
    }

    private fun init() {
        CONFIGURABLE_COLUMNS.forEachIndexed { ix, columnName ->
            val cb = JCheckBox(columnName)
            cb.isSelected = true
            hmColumnNameToCheckBox[columnName] = cb

            panel.add(cb, "cell 1 " + (ix + 1))
        }
    }

    fun excludedColumns() = hmColumnNameToCheckBox.filterValues { !it.isSelected }.map { it.key }

    override fun okPressed() {
        dispose()
    }

    override fun allowCancel() = false
}

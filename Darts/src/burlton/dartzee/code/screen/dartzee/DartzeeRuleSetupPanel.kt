package burlton.dartzee.code.screen.dartzee

import burlton.dartzee.code.dartzee.DartzeeRuleDto
import burlton.dartzee.code.utils.InjectedThings
import burlton.dartzee.code.core.bean.RowSelectionListener
import burlton.dartzee.code.core.bean.ScrollTable
import burlton.dartzee.code.core.bean.ScrollTableOrdered
import burlton.desktopcore.code.util.TableUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class DartzeeRuleSetupPanel: JPanel(), ActionListener, RowSelectionListener
{
    val tableRules = ScrollTableOrdered()
    val btnAddRule = JButton()
    val btnAmendRule = JButton()
    val btnRemoveRule = JButton()
    val btnCalculateOrder = JButton()

    init
    {
        layout = BorderLayout(0, 0)
        add(tableRules, BorderLayout.CENTER)

        tableRules.border = EmptyBorder(5, 5, 0, 0)

        tableRules.addButtonToOrderingPanel(btnAddRule, 0)
        tableRules.addButtonToOrderingPanel(btnAmendRule, 1)
        tableRules.addButtonToOrderingPanel(btnRemoveRule, 2)
        tableRules.addButtonToOrderingPanel(btnCalculateOrder, 6)

        tableRules.setRowName("rule")
        tableRules.setRowHeight(40)
        tableRules.addRowSelectionListener(this)

        tableRules.addKeyAction(KeyEvent.VK_DELETE) { removeRule() }
        tableRules.addKeyAction(KeyEvent.VK_ENTER) { amendRule() }

        btnAddRule.icon = ImageIcon(javaClass.getResource("/buttons/add.png"))
        btnAddRule.toolTipText = "Add rule"
        btnAddRule.preferredSize = Dimension(40, 40)

        btnAmendRule.icon = ImageIcon(javaClass.getResource("/buttons/amend.png"))
        btnAmendRule.toolTipText = "Edit rule"
        btnAmendRule.preferredSize = Dimension(40, 40)

        btnRemoveRule.icon = ImageIcon(javaClass.getResource("/buttons/remove.png"))
        btnRemoveRule.toolTipText = "Remove rule"
        btnRemoveRule.preferredSize = Dimension(40, 40)

        btnCalculateOrder.icon = ImageIcon(javaClass.getResource("/buttons/calculator.png"))
        btnCalculateOrder.toolTipText = "Order by calculated difficulty"
        btnCalculateOrder.preferredSize = Dimension(40, 40)

        setTableModel()

        btnAddRule.addActionListener(this)
        btnAmendRule.addActionListener(this)
        btnRemoveRule.addActionListener(this)
        btnCalculateOrder.addActionListener(this)
    }

    private fun setTableModel()
    {
        val tm = TableUtil.DefaultModel()
        tm.addColumn("Rule")
        tm.addColumn("Difficulty")

        tableRules.model = tm

        tableRules.setRenderer(0, DartzeeRuleRenderer(0))
        tableRules.setRenderer(1, DartzeeRuleRenderer(1))

        selectionChanged(tableRules)
    }

    override fun actionPerformed(arg0: ActionEvent)
    {
        when (arg0.source)
        {
            btnAddRule -> addRule()
            btnAmendRule -> amendRule()
            btnRemoveRule -> removeRule()
            btnCalculateOrder -> sortRulesByDifficulty()
        }
    }

    private fun addRule()
    {
        val rule = InjectedThings.dartzeeRuleFactory.newRule()
        if (rule != null)
        {
            addRuleToTable(rule)
        }
    }
    private fun amendRule()
    {
        val rowIndex = tableRules.selectedModelRow

        val tm = tableRules.model
        val selection = tm.getValueAt(rowIndex, 0) as DartzeeRuleDto

        val newRule = InjectedThings.dartzeeRuleFactory.amendRule(selection)

        removeRule()

        tableRules.insertRow(arrayOf(newRule, newRule), rowIndex)
        tableRules.selectRow(rowIndex)

        tableRules.repaint()
    }
    private fun removeRule()
    {
        val tm = tableRules.model
        tm.removeRow(tableRules.selectedModelRow)

        tableRules.repaint()
    }
    private fun sortRulesByDifficulty()
    {
        tableRules.reorderRows { -(it[0] as DartzeeRuleDto).getDifficulty() }
    }

    fun addRulesToTable(rules: List<DartzeeRuleDto>)
    {
        rules.forEach { addRuleToTable(it) }
    }
    private fun addRuleToTable(rule: DartzeeRuleDto)
    {
        tableRules.addRow(arrayOf(rule, rule))
    }

    override fun selectionChanged(src: ScrollTable)
    {
        btnAmendRule.isEnabled = src.selectedModelRow != -1
        btnRemoveRule.isEnabled = src.selectedModelRow != -1
    }

    fun getRules() = tableRules.getAllRows().map { it[0] as DartzeeRuleDto }
}
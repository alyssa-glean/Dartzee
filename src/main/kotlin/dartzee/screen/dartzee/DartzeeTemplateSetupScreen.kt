package dartzee.screen.dartzee

import dartzee.core.bean.RowSelectionListener
import dartzee.core.bean.ScrollTable
import dartzee.core.util.DialogUtil
import dartzee.core.util.TableUtil
import dartzee.db.DartzeeRuleEntity
import dartzee.db.DartzeeTemplateEntity
import dartzee.db.EntityName
import dartzee.game.GameType
import dartzee.screen.EmbeddedScreen
import dartzee.utils.InjectedThings.mainDatabase
import dartzee.utils.deleteDartzeeTemplate
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JPanel

class DartzeeTemplateSetupScreen: EmbeddedScreen(), RowSelectionListener
{
    private val scrollTable = ScrollTable()
    private val panelEast = JPanel()
    private val btnAdd = JButton()
    private val btnRename = JButton()
    private val btnCopy = JButton()
    private val btnDelete = JButton()

    init
    {
        add(scrollTable)
        add(panelEast, BorderLayout.EAST)

        panelEast.layout = MigLayout("al center center, wrap, gapy 20")
        panelEast.add(btnAdd)
        panelEast.add(btnRename)
        panelEast.add(btnCopy)
        panelEast.add(btnDelete)

        btnAdd.name = "add"
        btnAdd.icon = ImageIcon(javaClass.getResource("/buttons/add.png"))
        btnAdd.toolTipText = "New Template"
        btnAdd.preferredSize = Dimension(40, 40)

        btnRename.name = "rename"
        btnRename.icon = ImageIcon(javaClass.getResource("/buttons/rename.png"))
        btnRename.toolTipText = "Rename Template"
        btnRename.preferredSize = Dimension(40, 40)

        btnCopy.name = "copy"
        btnCopy.icon = ImageIcon(javaClass.getResource("/buttons/copy.png"))
        btnCopy.toolTipText = "Copy Template"
        btnCopy.preferredSize = Dimension(40, 40)

        btnDelete.name = "delete"
        btnDelete.icon = ImageIcon(javaClass.getResource("/buttons/remove.png"))
        btnDelete.toolTipText = "Delete Template"
        btnDelete.preferredSize = Dimension(40, 40)

        scrollTable.setTableFont(Font(font.name, Font.PLAIN, 18))
        scrollTable.addRowSelectionListener(this)

        scrollTable.addKeyAction(KeyEvent.VK_DELETE) { deleteTemplate() }
        scrollTable.setHeaderFont(Font(font.name, Font.PLAIN, 20))

        btnAdd.addActionListener(this)
        btnRename.addActionListener(this)
        btnCopy.addActionListener(this)
        btnDelete.addActionListener(this)
    }

    override fun initialise()
    {
        val tm = TableUtil.DefaultModel()

        tm.addColumn("Template")
        tm.addColumn("Rules")
        tm.addColumn("Games")

        scrollTable.model = tm
        scrollTable.setRowName("template")
        scrollTable.setRowHeight(40)
        scrollTable.setColumnWidths("200;-1;100")

        scrollTable.setRenderer(1, DartzeeTemplateRuleRenderer())

        populateTable()

        selectionChanged(scrollTable)
    }

    private fun populateTable()
    {
        val cols = DartzeeTemplateEntity().getColumnsForSelectStatement("t")

        val allRules = DartzeeRuleEntity().retrieveEntities("EntityName = '${EntityName.DartzeeTemplate}'")
        val hmTemplateIdToRules = allRules.groupBy { it.entityId }

        val sb = StringBuilder()
        sb.append(" SELECT $cols, COUNT(g.RowId) AS GameCount")
        sb.append(" FROM DartzeeTemplate t")
        sb.append(" LEFT OUTER JOIN Game g ON (g.GameType = '${GameType.DARTZEE}' AND g.GameParams = t.RowId)")
        sb.append(" GROUP BY $cols")

        mainDatabase.executeQuery(sb).use { rs ->
            while (rs.next()) {
                val template = DartzeeTemplateEntity().factoryFromResultSet(rs)
                val gameCount = rs.getInt("GameCount")

                val rules = hmTemplateIdToRules[template.rowId].orEmpty()

                addTemplateToTable(template, rules, gameCount)
            }
        }
    }
    private fun addTemplateToTable(template: DartzeeTemplateEntity, rules: List<DartzeeRuleEntity>, gameCount: Int)
    {
        val dtos = rules.sortedBy { it.ordinal }.map { it.toDto() }
        
        scrollTable.addRow(arrayOf(template, dtos, gameCount))
    }

    private fun addTemplate()
    {
        DartzeeTemplateDialog.createTemplate(::initialise)
    }

    private fun renameTemplate()
    {
        val selection = getSelectedTemplate()

        val result = DialogUtil.showInput("Rename Template", "Name", null, selection.name) ?: return
        if (result.isNotEmpty())
        {
            selection.name = result
            selection.saveToDatabase()
            scrollTable.repaint()
        }
    }

    private fun copySelectedTemplate()
    {
        val selection = getSelectedTemplate()

        DartzeeTemplateDialog.createTemplate(::initialise, selection)
    }

    private fun deleteTemplate()
    {
        val selection = getSelectedTemplate()
        val gameCount = scrollTable.model.getValueAt(scrollTable.selectedModelRow, 2) as Int

        val deleted = deleteDartzeeTemplate(selection, gameCount)
        if (deleted)
        {
            initialise()
        }
    }

    private fun getSelectedTemplate(): DartzeeTemplateEntity
    {
        val rowIndex = scrollTable.selectedModelRow
        return scrollTable.model.getValueAt(rowIndex, 0) as DartzeeTemplateEntity
    }

    override fun actionPerformed(arg0: ActionEvent)
    {
        when (arg0.source)
        {
            btnAdd -> addTemplate()
            btnRename -> renameTemplate()
            btnCopy -> copySelectedTemplate()
            btnDelete -> deleteTemplate()
            else -> super.actionPerformed(arg0)
        }
    }

    override fun selectionChanged(src: ScrollTable)
    {
        btnRename.isEnabled = src.selectedModelRow != -1
        btnCopy.isEnabled = src.selectedModelRow != -1
        btnDelete.isEnabled = src.selectedModelRow != -1
    }

    override fun getScreenName() = "Dartzee Templates"
}
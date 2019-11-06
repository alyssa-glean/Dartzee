package burlton.dartzee.code.screen.dartzee

import burlton.dartzee.code.db.DARTZEE_TEMPLATE
import burlton.dartzee.code.db.DartzeeRuleEntity
import burlton.dartzee.code.db.DartzeeTemplateEntity
import burlton.dartzee.code.screen.ScreenCache
import burlton.desktopcore.code.bean.addGhostText
import burlton.desktopcore.code.screen.SimpleDialog
import burlton.desktopcore.code.util.DialogUtil
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.BevelBorder

class DartzeeTemplateDialog : SimpleDialog()
{
    var dartzeeTemplate: DartzeeTemplateEntity? = null

    private val namePanel = JPanel()
    val tfName = JTextField()
    val rulePanel = DartzeeRuleSetupPanel()

    init
    {
        title = "New Dartzee Template"
        size = Dimension(800, 600)
        isModal = true

        add(namePanel, BorderLayout.NORTH)
        add(rulePanel, BorderLayout.CENTER)

        tfName.addGhostText("Template Name")

        namePanel.layout = BorderLayout(0, 0)
        namePanel.border = BevelBorder(BevelBorder.LOWERED)
        namePanel.add(tfName)
    }

    override fun okPressed()
    {
        if (!valid())
        {
            return
        }

        val template = DartzeeTemplateEntity.factoryAndSave(tfName.text)

        val rules = rulePanel.getRules()
        rules.forEachIndexed { ix, rule ->
            val entity = rule.toEntity(ix + 1, DARTZEE_TEMPLATE, template.rowId)
            entity.saveToDatabase()
        }

        dartzeeTemplate = template

        dispose()
    }

    private fun valid(): Boolean
    {
        if (tfName.text.isEmpty())
        {
            DialogUtil.showError("You must enter a name.")
            tfName.requestFocus()
            return false
        }

        if (rulePanel.getRules().size < 2)
        {
            DialogUtil.showError("You must create at least 2 rules.")
            return false
        }

        return true
    }


    fun copy(templateToCopy: DartzeeTemplateEntity)
    {
        tfName.text = "${templateToCopy.name} - Copy"

        val rules = DartzeeRuleEntity().retrieveForTemplate(templateToCopy.rowId)

        val dtos = rules.sortedBy{ it.ordinal }.map { it.toDto() }
        rulePanel.addRulesToTable(dtos)
    }

    companion object
    {
        fun createTemplate(templateToCopy: DartzeeTemplateEntity? = null): DartzeeTemplateEntity?
        {
            val dlg = DartzeeTemplateDialog()
            templateToCopy?.let { dlg.copy(it) }
            dlg.setLocationRelativeTo(ScreenCache.getMainScreen())
            dlg.isVisible = true
            return dlg.dartzeeTemplate
        }
    }
}
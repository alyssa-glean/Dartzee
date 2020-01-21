package burlton.dartzee.code.db.sanity

import burlton.dartzee.code.db.AbstractEntity
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.dartzee.code.core.util.DialogUtil
import javax.swing.JOptionPane

class SanityCheckResultDanglingIdFields(private val idColumn: String,
                                        private val referencedEntity: String,
                                        entities: List<AbstractEntity<*>>) : AbstractSanityCheckResultEntities(entities)
{
    override fun getDescription() = "$entityName rows where the $idColumn points at a non-existent $referencedEntity"

    override fun autoFix()
    {
        val rowIds = entities.map { it.rowId }

        val ans = DialogUtil.showQuestion("Are you sure you want to delete ${entities.size} rows from $entityName?")
        if (ans != JOptionPane.YES_OPTION)
        {
            return
        }

        val success = DatabaseUtil.deleteRowsFromTable(entityName, rowIds)
        if (success)
        {
            DialogUtil.showInfo("Rows deleted successfully. You should re-run the sanity check.")
        }
        else
        {
            DialogUtil.showError("An error occurred deleting the rows.")
        }
    }
}

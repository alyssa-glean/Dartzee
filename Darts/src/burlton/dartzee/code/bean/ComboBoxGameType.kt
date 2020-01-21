package burlton.dartzee.code.bean

import burlton.dartzee.code.utils.getAllGameTypes
import burlton.dartzee.code.utils.getTypeDesc
import burlton.dartzee.code.core.bean.ComboBoxItem
import burlton.dartzee.code.core.bean.selectedItemTyped
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

class ComboBoxGameType : JComboBox<ComboBoxItem<Int>>()
{
    init
    {
        val model = DefaultComboBoxModel<ComboBoxItem<Int>>()

        val gameTypes = getAllGameTypes()
        for (gameType in gameTypes)
        {
            val item = ComboBoxItem(gameType, getTypeDesc(gameType))
            model.addElement(item)
        }

        setModel(model)
    }

    fun getGameType() = selectedItemTyped().hiddenData
}

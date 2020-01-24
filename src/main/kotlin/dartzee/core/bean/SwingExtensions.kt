package dartzee.core.bean

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Point
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.text.JTextComponent


fun <K> JComboBox<K>.items(): List<K>
{
    val list = mutableListOf<K>()
    for (i in 0 until model.size)
    {
        list.add(model.getElementAt(i))
    }

    return list
}

fun <K> JComboBox<K>.selectedItemTyped(): K = getItemAt(selectedIndex)

fun <T> JComboBox<*>.findByConcreteClass(clazz: Class<T>): T? = items().find { clazz.isInstance(it) } as T?
inline fun <reified T> JComboBox<*>.findByClass(): T? = items().find { it is T } as T?

inline fun <reified T> JComboBox<*>.selectByClass() = findByClass<T>()?.also { selectedItem = it }

fun JTextField.addUpdateListener(actionListener: ActionListener)
{
    val tf = this
    addFocusListener(object: FocusListener
    {
        override fun focusLost(e: FocusEvent?)
        {
            val event = ActionEvent(tf, -1, "")
            actionListener.actionPerformed(event)
        }
        override fun focusGained(e: FocusEvent?){}
    })
}

fun JTable.addKeyAction(key: Int, fn: () -> Unit) {
    val action = object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            fn()
        }
    }

    inputMap.put(KeyStroke.getKeyStroke(key, JComponent.WHEN_FOCUSED),"ACTION_$key")
    actionMap.put("ACTION_$key", action)
}

fun JTextComponent.addGhostText(text: String)
{
    this.layout = BorderLayout()
    this.add(GhostText(text, this))
}

fun BufferedImage.paint(fn: (pt: Point) -> Color?)
{
    val pts = getPointList(width, height)
    val colors = pts.map { fn(it)?.rgb ?: 0 }

    setRGB(0, 0, width, height, colors.toIntArray(), 0, width)
}
fun getPointList(width: Int, height: Int): List<Point>
{
    val yRange = 0 until height
    val xRange = 0 until width

    return yRange.map { y -> xRange.map { x -> Point(x, y)} }.flatten()
}
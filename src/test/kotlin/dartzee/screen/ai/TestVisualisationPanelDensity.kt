package dartzee.screen.ai

import com.github.alexburlton.swingtest.shouldMatchImage
import dartzee.helper.AbstractTest
import dartzee.helper.makeDartsModel
import org.junit.Test
import java.awt.Dimension

class TestVisualisationPanelDensity: AbstractTest()
{
    @Test
    fun `Should match snapshot - T20`()
    {
        val model = makeDartsModel(scoringDart = 20)

        val panel = VisualisationPanelDensity()
        panel.size = Dimension(600, 500)
        panel.populate(mapOf(), model)

        panel.shouldMatchImage("T20")
    }

    @Test
    fun `Should match snapshot - bullseye`()
    {
        val model = makeDartsModel(scoringDart = 25, standardDeviation = 100.0)

        val panel = VisualisationPanelDensity()
        panel.size = Dimension(600, 500)
        panel.populate(mapOf(), model)

        panel.shouldMatchImage("Bullseye")
    }
}
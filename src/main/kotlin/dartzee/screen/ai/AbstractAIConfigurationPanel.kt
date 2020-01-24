package dartzee.screen.ai

import dartzee.ai.AbstractDartsModel

/**
 * The panel that actually constructs the DartsModel, and thus contains the things that are specific to it
 */
abstract class AbstractAIConfigurationPanel : AbstractAIConfigurationSubPanel()
{
    abstract fun initialiseModel(): AbstractDartsModel

    override fun populateModel(model: AbstractDartsModel)
    {
        //Do nothing (we initialise the model instead)
    }
}

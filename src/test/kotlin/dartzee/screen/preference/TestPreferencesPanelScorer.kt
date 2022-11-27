package dartzee.screen.preference

import dartzee.utils.PREFERENCES_DOUBLE_BG_BRIGHTNESS
import dartzee.utils.PREFERENCES_DOUBLE_FG_BRIGHTNESS
import dartzee.utils.PREFERENCES_DOUBLE_HUE_FACTOR
import dartzee.utils.PreferenceUtil
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TestPreferencesPanelScorer: AbstractPreferencePanelTest<PreferencesPanelScorer>()
{
    override fun getPreferencesAffected(): MutableList<String>
    {
        return mutableListOf(PREFERENCES_DOUBLE_HUE_FACTOR,
                PREFERENCES_DOUBLE_BG_BRIGHTNESS,
                PREFERENCES_DOUBLE_FG_BRIGHTNESS)
    }

    override fun factory() = PreferencesPanelScorer()

    override fun checkUiFieldValuesAreDefaults(panel: PreferencesPanelScorer)
    {
        panel.spinnerHueFactor.value shouldBe 0.8
        panel.spinnerFgBrightness.value shouldBe 0.5
        panel.spinnerBgBrightness.value shouldBe 1.0
    }

    override fun setUiFieldValuesToNonDefaults(panel: PreferencesPanelScorer)
    {
        panel.spinnerHueFactor.value = 0.5
        panel.spinnerFgBrightness.value = 0.9
        panel.spinnerBgBrightness.value = 0.6
    }

    override fun checkUiFieldValuesAreNonDefaults(panel: PreferencesPanelScorer)
    {
        panel.spinnerHueFactor.value shouldBe 0.5
        panel.spinnerFgBrightness.value shouldBe 0.9
        panel.spinnerBgBrightness.value shouldBe 0.6
    }

    override fun checkPreferencesAreSetToNonDefaults()
    {
        PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_HUE_FACTOR) shouldBe 0.5
        PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_FG_BRIGHTNESS) shouldBe 0.9
        PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_BG_BRIGHTNESS) shouldBe 0.6
    }
}
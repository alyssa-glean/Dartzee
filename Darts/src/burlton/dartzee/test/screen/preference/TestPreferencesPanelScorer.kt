package burlton.dartzee.test.screen.preference

import burlton.dartzee.code.screen.preference.PreferencesPanelScorer
import burlton.dartzee.code.utils.*
import io.kotlintest.shouldBe

class TestPreferencesPanelScorer: AbstractPreferencePanelTest<PreferencesPanelScorer>()
{
    override fun getPreferencesAffected(): MutableList<String>
    {
        return mutableListOf(PREFERENCES_DOUBLE_HUE_FACTOR,
                PREFERENCES_DOUBLE_BG_BRIGHTNESS,
                PREFERENCES_DOUBLE_FG_BRIGHTNESS,
                PREFERENCES_BOOLEAN_DISPLAY_DART_TOTAL_SCORE)
    }

    override fun factory() = PreferencesPanelScorer()

    override fun checkUiFieldValuesAreDefaults(panel: PreferencesPanelScorer)
    {
        panel.rdbtn40.isSelected shouldBe true
        panel.rdbtnD20.isSelected shouldBe false
        panel.spinnerHueFactor.value shouldBe 0.8
        panel.spinnerFgBrightness.value shouldBe 0.5
        panel.spinnerBgBrightness.value shouldBe 1.0
    }

    override fun setUiFieldValuesToNonDefaults(panel: PreferencesPanelScorer)
    {
        panel.rdbtn40.isSelected = false
        panel.rdbtnD20.isSelected = true
        panel.spinnerHueFactor.value = 0.5
        panel.spinnerFgBrightness.value = 0.9
        panel.spinnerBgBrightness.value = 0.6
    }

    override fun checkUiFieldValuesAreNonDefaults(panel: PreferencesPanelScorer)
    {
        panel.rdbtn40.isSelected shouldBe false
        panel.rdbtnD20.isSelected shouldBe true
        panel.spinnerHueFactor.value shouldBe 0.5
        panel.spinnerFgBrightness.value shouldBe 0.9
        panel.spinnerBgBrightness.value shouldBe 0.6
    }

    override fun checkPreferencesAreSetToNonDefaults()
    {
        PreferenceUtil.getBooleanValue(PREFERENCES_BOOLEAN_DISPLAY_DART_TOTAL_SCORE) shouldBe false
        PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_HUE_FACTOR) shouldBe 0.5
        PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_FG_BRIGHTNESS) shouldBe 0.9
        PreferenceUtil.getDoubleValue(PREFERENCES_DOUBLE_BG_BRIGHTNESS) shouldBe 0.6
    }
}
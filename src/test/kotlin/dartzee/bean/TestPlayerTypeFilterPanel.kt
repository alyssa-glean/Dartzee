package dartzee.bean

import dartzee.db.PlayerEntity
import dartzee.helper.AbstractTest
import dartzee.helper.insertPlayer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import org.junit.jupiter.api.Test

class TestPlayerTypeFilterPanel : AbstractTest() {
    @Test
    fun `Should have All selected by default`() {
        val panel = PlayerTypeFilterPanel()
        panel.rdbtnAll.isSelected shouldBe true
        panel.getWhereSql().shouldBeEmpty()
    }

    @Test
    fun `Should return correct filter SQL for humans and AIs`() {
        val ai = insertPlayer(strategy = "foo")
        val human = insertPlayer(strategy = "")

        val panel = PlayerTypeFilterPanel()

        val players = PlayerEntity.retrievePlayers(panel.getWhereSql())
        players.size shouldBe 2

        panel.rdbtnAi.doClick()
        val aiPlayers = PlayerEntity.retrievePlayers(panel.getWhereSql())
        aiPlayers.size shouldBe 1
        aiPlayers.first().rowId shouldBe ai.rowId

        panel.rdbtnHuman.doClick()
        val humanPlayers = PlayerEntity.retrievePlayers(panel.getWhereSql())
        humanPlayers.size shouldBe 1
        humanPlayers.first().rowId shouldBe human.rowId
    }
}

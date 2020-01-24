package dartzee.bean

import dartzee.db.PlayerEntity
import dartzee.helper.AbstractTest
import dartzee.helper.insertPlayer
import dartzee.helper.wipeTable
import io.kotlintest.matchers.string.shouldBeEmpty
import io.kotlintest.shouldBe
import org.junit.Test

class TestPlayerTypeFilterPanel: AbstractTest()
{
    @Test
    fun `Should have All selected by default`()
    {
        val panel = PlayerTypeFilterPanel()
        panel.rdbtnAll.isSelected shouldBe true
        panel.getWhereSql().shouldBeEmpty()
    }

    @Test
    fun `Should return correct filter SQL for humans and AIs`()
    {
        wipeTable("Player")

        val ai = insertPlayer(strategy = 1)
        val human = insertPlayer(strategy = -1)

        val panel = PlayerTypeFilterPanel()

        val players = PlayerEntity.retrievePlayers(panel.getWhereSql(), false)
        players.size shouldBe 2

        panel.rdbtnAi.doClick()
        val aiPlayers = PlayerEntity.retrievePlayers(panel.getWhereSql(), false)
        aiPlayers.size shouldBe 1
        aiPlayers.first().rowId shouldBe ai.rowId

        panel.rdbtnHuman.doClick()
        val humanPlayers = PlayerEntity.retrievePlayers(panel.getWhereSql(), false)
        humanPlayers.size shouldBe 1
        humanPlayers.first().rowId shouldBe human.rowId
    }
}
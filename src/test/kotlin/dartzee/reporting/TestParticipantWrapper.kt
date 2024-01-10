package dartzee.reporting

import dartzee.helper.AbstractTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TestParticipantWrapper : AbstractTest() {
    @Test
    fun `Should describe the player and their position correctly`() {
        ParticipantWrapper("Alice", 3).toString() shouldBe "Alice (3)"
        ParticipantWrapper("Bob", 6).toString() shouldBe "Bob (6)"
        ParticipantWrapper("Clive", -1).toString() shouldBe "Clive (-)"
    }
}

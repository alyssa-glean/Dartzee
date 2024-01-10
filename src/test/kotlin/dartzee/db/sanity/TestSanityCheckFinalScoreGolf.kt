package dartzee.db.sanity

import dartzee.db.ParticipantEntity
import dartzee.game.GameType
import dartzee.helper.AbstractTest
import dartzee.helper.insertDart
import dartzee.helper.insertGame
import dartzee.helper.insertParticipant
import dartzee.helper.insertPlayer
import dartzee.`object`.SegmentType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TestSanityCheckFinalScoreGolf : AbstractTest() {
    @Test
    fun `Should take the score from the latest dart thrown`() {
        val pt = setUpParticipant(4)

        // Round one - 4
        insertDart(
            pt,
            roundNumber = 1,
            ordinal = 1,
            score = 1,
            segmentType = SegmentType.OUTER_SINGLE
        )
        insertDart(
            pt,
            roundNumber = 1,
            ordinal = 2,
            score = 1,
            segmentType = SegmentType.INNER_SINGLE
        )
        insertDart(
            pt,
            roundNumber = 1,
            ordinal = 3,
            score = 1,
            segmentType = SegmentType.OUTER_SINGLE
        )

        val results = SanityCheckFinalScoreGolf().runCheck()
        results.shouldBeEmpty()
    }

    @Test
    fun `Should score 5 if score does not match round number`() {
        val pt = setUpParticipant(5)
        insertDart(pt, roundNumber = 1, ordinal = 1, score = 2, segmentType = SegmentType.DOUBLE)

        val results = SanityCheckFinalScoreGolf().runCheck()
        results.shouldBeEmpty()
    }

    @Test
    fun `Should sum the score across rounds for the same participant`() {
        val pt = setUpParticipant(7)

        // Round one
        insertDart(
            pt,
            roundNumber = 1,
            ordinal = 1,
            score = 1,
            segmentType = SegmentType.OUTER_SINGLE
        )
        insertDart(
            pt,
            roundNumber = 2,
            ordinal = 1,
            score = 2,
            segmentType = SegmentType.INNER_SINGLE
        )

        val results = SanityCheckFinalScoreGolf().runCheck()
        results.shouldBeEmpty()
    }

    @Test
    fun `Should get the right score based on segment type`() {
        checkScoreCorrectForSegmentType(SegmentType.MISS, 5)
        checkScoreCorrectForSegmentType(SegmentType.OUTER_SINGLE, 4)
        checkScoreCorrectForSegmentType(SegmentType.INNER_SINGLE, 3)
        checkScoreCorrectForSegmentType(SegmentType.TREBLE, 2)
        checkScoreCorrectForSegmentType(SegmentType.DOUBLE, 1)
    }

    @Test
    fun `Should flag up if the score is incorrect`() {
        val pt = setUpParticipant(5)

        insertDart(
            pt,
            roundNumber = 1,
            ordinal = 1,
            score = 1,
            segmentType = SegmentType.OUTER_SINGLE
        )

        val results = SanityCheckFinalScoreGolf().runCheck()
        results.size shouldBe 1

        val result = results.first()
        result.shouldBeInstanceOf<SanityCheckResultFinalScoreMismatch>()
        result.getDescription() shouldBe "FinalScores that don't match the raw data (Golf)"
        result.getCount() shouldBe 1
    }

    private fun checkScoreCorrectForSegmentType(segmentType: SegmentType, expectedScore: Int) {
        val pt = setUpParticipant(expectedScore)

        insertDart(pt, roundNumber = 1, ordinal = 1, score = 1, segmentType = segmentType)

        val results = SanityCheckFinalScoreGolf().runCheck()
        results.shouldBeEmpty()
    }

    private fun setUpParticipant(finalScore: Int): ParticipantEntity {
        val game = insertGame(gameType = GameType.GOLF)
        val player = insertPlayer()
        return insertParticipant(
            gameId = game.rowId,
            playerId = player.rowId,
            finalScore = finalScore
        )
    }
}

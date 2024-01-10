package dartzee.logging

import dartzee.CURRENT_TIME_STRING
import dartzee.helper.AbstractTest
import dartzee.makeLogRecord
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TestLogRecord : AbstractTest() {
    @Test
    fun `Should render as a string for the logging console`() {
        val record =
            makeLogRecord(loggingCode = LoggingCode("some.code"), message = "This is a log")

        record.toString() shouldBe "$CURRENT_TIME_STRING   [some.code] This is a log"
    }

    @Test
    fun `Should return NULL if no error object`() {
        val record = makeLogRecord()
        record.getThrowableStr() shouldBe null
    }

    @Test
    fun `Should return the dated stack trace if it exists`() {
        val t = Throwable("Boom")
        val record = makeLogRecord(errorObject = t)

        record.getThrowableStr() shouldBe "$CURRENT_TIME_STRING   ${extractStackTrace(t)}"
    }

    @Test
    fun `Should convert to valid JSON string`() {
        val record =
            makeLogRecord(
                loggingCode = LoggingCode("someEvent"),
                keyValuePairs = mapOf("devMode" to true, "currentScreen" to null)
            )

        val str = record.toJsonString()
        str shouldBe
            """{"timestamp":"2020-04-13T11:04:00Z","severity":"INFO","loggingCode":"someEvent","message":"A thing happened","devMode":"true","currentScreen":"null"}"""
    }
}

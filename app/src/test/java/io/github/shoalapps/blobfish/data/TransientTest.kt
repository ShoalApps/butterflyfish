package io.github.shoalapps.blobfish.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransientTest {
    @Test fun busyServersAreRetriedForReads() {
        for (code in listOf(502, 503, 504)) assertTrue(Transient.shouldRetry("GET", code, 0))
        assertTrue(Transient.shouldRetry("GET", null, 1))
    }

    @Test fun retriesStopAfterTwoTries() {
        assertFalse(Transient.shouldRetry("GET", 504, Transient.delaysMs.size))
    }

    @Test fun writesAreNeverRetried() {
        assertFalse(Transient.shouldRetry("POST", 504, 0))
        assertFalse(Transient.shouldRetry("POST", null, 0))
    }

    @Test fun realErrorsAreNotRetried() {
        for (code in listOf(400, 401, 404, 500)) assertFalse(Transient.shouldRetry("GET", code, 0))
    }

    @Test fun aBusyServerIsExplainedWithoutTheCode() {
        assertEquals("Bluesky is busy right now. Try again in a moment.", Transient.message(504))
        assertEquals("Bluesky had a problem (500). Try again in a moment.", Transient.message(500))
        assertEquals("Bluesky said 418", Transient.message(418))
    }
}

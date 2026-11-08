package io.github.shoalapps.blobfish.data

import io.github.shoalapps.blobfish.data.EngagementRules.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolishRulesTest {
    // ---- Likes, reposts and follows ----

    @Test fun aTapShowsAtOnceAndSaysWhatToSend() {
        val (liked, step) = EngagementRules.tap(Mark(null, 4))
        assertEquals(Mark(Engagement.PENDING, 5), liked)
        assertEquals(Step.Create, step)
        val (unliked, undo) = EngagementRules.tap(Mark("at://me/like/1", 5))
        assertEquals(Mark(null, 4), unliked)
        assertEquals(Step.Delete("at://me/like/1"), undo)
    }

    @Test fun aSecondTapWhileSendingIsIgnored() {
        val pending = Mark(Engagement.PENDING, 5)
        assertEquals(pending to Step.Wait, EngagementRules.tap(pending))
    }

    @Test fun countsNeverGoBelowZero() {
        assertEquals(0, EngagementRules.tap(Mark("at://me/like/1", 0)).first.count)
    }

    @Test fun yourTapWinsOverAListLoadedBeforeIt() {
        val stale = Mark(null, 4)
        val mine = Mark("at://me/like/1", 5)
        assertEquals(mine, EngagementRules.shown(stale, mine))
        assertEquals(Mark(null, 4), EngagementRules.shown(Mark("at://me/like/1", 5), Mark(null, 4)))
    }

    @Test fun theServerIsTrustedOnceItAgrees() {
        val fresh = Mark("at://me/like/1", 9)
        assertEquals(fresh, EngagementRules.shown(fresh, Mark("at://me/like/1", 5)))
        assertEquals(Mark(null, 3), EngagementRules.shown(Mark(null, 3), null))
    }

    @Test fun aPendingTapIsShownWhateverTheServerSays() {
        val pending = Mark(Engagement.PENDING, 5)
        assertEquals(pending, EngagementRules.shown(Mark("at://elsewhere", 5), pending))
    }

    // ---- Staying signed in ----

    @Test fun onlyARefusedSignInSignsYouOut() {
        assertTrue(SessionRules.rejected(401, null))
        assertTrue(SessionRules.rejected(400, "invalid_grant"))
        assertTrue(SessionRules.rejected(400, "ExpiredToken"))
    }

    @Test fun networkAndServerTroubleKeepYouSignedIn() {
        assertFalse(SessionRules.rejected(null, null))
        for (code in listOf(500, 502, 503, 504, 429)) assertFalse(SessionRules.rejected(code, null))
        assertFalse(SessionRules.rejected(400, "invalid_dpop_proof"))
        assertFalse(SessionRules.rejected(400, null))
    }

    // ---- Profile editing ----

    @Test fun onlyAMissingProfileStartsFromEmpty() {
        assertTrue(ProfileRules.missing(400, "RecordNotFound"))
        assertTrue(ProfileRules.missing(404, null))
        assertFalse(ProfileRules.missing(500, null))
        assertFalse(ProfileRules.missing(400, "InvalidRequest"))
        assertFalse(ProfileRules.missing(401, null))
    }

    @Test fun anEditElsewhereIsAConflict() {
        assertTrue(ProfileRules.conflict("InvalidSwap"))
        assertFalse(ProfileRules.conflict("InternalServerError"))
        assertFalse(ProfileRules.conflict(null))
    }
}

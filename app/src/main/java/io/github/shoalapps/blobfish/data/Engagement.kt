package io.github.shoalapps.blobfish.data

import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException

/** Your like, repost or follow of something: its record (null when you haven't), and the count shown beside it. */
data class Mark(val record: String?, val count: Int = 0) {
    val on get() = record != null
    val pending get() = record == Engagement.PENDING
}

/** The rules for taps, kept apart from Compose so they can be tested. */
object EngagementRules {
    sealed interface Step {
        /** A tap while the last one is still being sent: ignored, so nothing is undone before it exists. */
        data object Wait : Step
        data object Create : Step
        data class Delete(val record: String) : Step
    }

    /**
     * What to show: your latest tap, until the server's copy agrees with it. A list loaded before
     * the tap can't undo it, and once the server has caught up, its count is used.
     */
    fun shown(server: Mark, mine: Mark?): Mark = when {
        mine == null -> server
        mine.pending -> mine
        mine.on == server.on -> server
        else -> mine
    }

    /** What a tap shows straight away, and what has to be sent. */
    fun tap(current: Mark): Pair<Mark, Step> = when {
        current.pending -> current to Step.Wait
        current.record == null -> Mark(Engagement.PENDING, current.count + 1) to Step.Create
        else -> Mark(null, (current.count - 1).coerceAtLeast(0)) to Step.Delete(current.record)
    }
}

/**
 * Likes, reposts and follows you've changed in this session, shared by every screen, so a post
 * shows the same like in the feed, its thread and on a profile, and keeps it when scrolled away.
 */
object Engagement {
    const val PENDING = "pending"

    private val likes = mutableStateMapOf<String, Mark>()
    private val reposts = mutableStateMapOf<String, Mark>()
    private val follows = mutableStateMapOf<String, Mark>()

    fun like(p: Post) = EngagementRules.shown(Mark(p.myLike, p.likeCount), likes[p.uri])
    fun repost(p: Post) = EngagementRules.shown(Mark(p.myRepost, p.repostCount), reposts[p.uri])
    fun follow(did: String, server: String?) = EngagementRules.shown(Mark(server), follows[did])

    suspend fun toggleLike(p: Post, create: suspend () -> String, delete: suspend (String) -> Unit) = toggle(likes, p.uri, like(p), create, delete)
    suspend fun toggleRepost(p: Post, create: suspend () -> String, delete: suspend (String) -> Unit) = toggle(reposts, p.uri, repost(p), create, delete)
    suspend fun toggleFollow(did: String, server: String?, create: suspend () -> String, delete: suspend (String) -> Unit) = toggle(follows, did, follow(did, server), create, delete)

    /** A different account sees its own likes and follows, not these. */
    fun clear() { likes.clear(); reposts.clear(); follows.clear() }

    /**
     * Shows a tap at once, sends it, and puts things back if it fails. Returns why it failed, for
     * the screen to say so, or null.
     */
    private suspend fun toggle(map: SnapshotStateMap<String, Mark>, key: String, current: Mark, create: suspend () -> String, delete: suspend (String) -> Unit): Throwable? {
        val (next, step) = EngagementRules.tap(current)
        if (step == EngagementRules.Step.Wait) return null
        map[key] = next
        return try {
            when (step) {
                EngagementRules.Step.Create -> map[key] = next.copy(record = create())
                is EngagementRules.Step.Delete -> delete(step.record)
                EngagementRules.Step.Wait -> Unit
            }
            null
        } catch (e: CancellationException) {
            map[key] = current
            throw e
        } catch (e: Exception) {
            map[key] = current
            e
        }
    }
}

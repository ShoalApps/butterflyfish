package io.github.shoalapps.blobfish.data

/** The list of signed-in Bluesky accounts, kept apart from storage so it can be tested. */
object AccountList {
    /** An account is its DID on its server: the same person on two servers is two accounts. */
    fun key(s: Session) = s.did + "|" + s.pds

    /** Adds an account, or replaces it in place (new tokens, a changed handle). */
    fun upsert(list: List<Session>, s: Session): List<Session> {
        val i = list.indexOfFirst { key(it) == key(s) }
        return if (i < 0) list + s else list.toMutableList().also { it[i] = s }
    }

    fun remove(list: List<Session>, key: String): List<Session> = list.filterNot { key(it) == key }
}

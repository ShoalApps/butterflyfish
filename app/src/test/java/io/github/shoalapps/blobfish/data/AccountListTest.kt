package io.github.shoalapps.blobfish.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountListTest {
    private fun s(did: String, pds: String = "https://bsky.social", access: String = "a") = Session(did, "$did.bsky.social", access, "r", pds)

    @Test fun signingInAddsAccountsInOrder() {
        val list = AccountList.upsert(AccountList.upsert(emptyList(), s("alice")), s("bob"))
        assertEquals(listOf("alice", "bob"), list.map { it.did })
    }

    @Test fun renewedTokensReplaceTheAccountInPlace() {
        val list = AccountList.upsert(listOf(s("alice"), s("bob")), s("alice", access = "new"))
        assertEquals(listOf("alice", "bob"), list.map { it.did })
        assertEquals("new", list[0].access)
    }

    @Test fun theSameDidOnAnotherServerIsAnotherAccount() {
        val list = AccountList.upsert(listOf(s("alice")), s("alice", pds = "https://pds.example"))
        assertEquals(2, list.size)
    }

    @Test fun signingOutRemovesOnlyThatAccount() {
        val list = listOf(s("alice"), s("bob"))
        assertEquals(listOf("bob"), AccountList.remove(list, AccountList.key(s("alice"))).map { it.did })
        assertEquals(2, AccountList.remove(list, "nobody|x").size)
    }
}

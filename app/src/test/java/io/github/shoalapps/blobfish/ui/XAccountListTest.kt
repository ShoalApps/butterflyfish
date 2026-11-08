package io.github.shoalapps.blobfish.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XAccountListTest {
    private fun a(handle: String, cookies: String = "auth_token=1") = SavedXAccount(handle, cookies = cookies)

    @Test fun accountsAreAddedInOrder() {
        val list = XAccountList.upsert(XAccountList.upsert(emptyList(), a("alice")), a("bob"))
        assertEquals(listOf("alice", "bob"), list.map { it.handle })
    }

    @Test fun newerCookiesReplaceTheAccountWhateverItsCapitals() {
        val list = XAccountList.upsert(listOf(a("Alice"), a("bob")), a("alice", "auth_token=2"))
        assertEquals(2, list.size)
        assertEquals("auth_token=2", list[0].cookies)
    }

    @Test fun signingOutRemovesOnlyThatAccount() {
        assertEquals(listOf("bob"), XAccountList.remove(listOf(a("alice"), a("bob")), "ALICE").map { it.handle })
    }

    @Test fun cookieNamesAreReadOnce() {
        assertEquals(listOf("auth_token", "ct0", "twid"), XAccountList.cookieNames("auth_token=a; ct0=b;twid=u%3D1; ct0=c"))
        assertTrue(XAccountList.cookieNames(null).isEmpty())
        assertTrue(XAccountList.cookieNames(" ; ").isEmpty())
    }
}

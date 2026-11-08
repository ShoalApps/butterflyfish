package io.github.shoalapps.blobfish.ui

import android.content.Context
import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.shoalapps.blobfish.data.Author
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** An X account signed in on this phone: who it is, and the website's sign-in cookies for it. */
@Serializable
data class SavedXAccount(val handle: String, val name: String? = null, val avatar: String? = null, val cookies: String)

/** The list of X accounts, kept apart from storage and the website so it can be tested. */
object XAccountList {
    /** Handles are the same account whatever their capitals. */
    fun same(a: String, b: String) = a.equals(b, ignoreCase = true)

    /** Adds an account, or replaces it in place (newer cookies, a new name or picture). */
    fun upsert(list: List<SavedXAccount>, account: SavedXAccount): List<SavedXAccount> {
        val i = list.indexOfFirst { same(it.handle, account.handle) }
        return if (i < 0) list + account else list.toMutableList().also { it[i] = account }
    }

    fun remove(list: List<SavedXAccount>, handle: String) = list.filterNot { same(it.handle, handle) }

    /** The names in a `name=value; name2=value2` cookie string. */
    fun cookieNames(cookies: String?): List<String> =
        cookies.orEmpty().split(';').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }.distinct()
}

/**
 * Several X accounts. X signs you in with cookies on its website, which Butterflyfish reads in a
 * hidden page, and that page holds one account at a time. So each account's cookies are kept
 * here, and switching puts the chosen account's cookies on the page in place of the current one's.
 * They stay in this app's private storage, like the page's own cookies.
 */
object XAccounts {
    private const val SITE = "https://x.com"
    private lateinit var prefs: android.content.SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    var accounts by mutableStateOf<List<SavedXAccount>>(emptyList())
        private set

    /** Bumped whenever the signed-in account changes, so screens showing X start again. */
    var generation by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("x-accounts", Context.MODE_PRIVATE)
        accounts = prefs.getString("accounts", null)?.let { runCatching { json.decodeFromString<List<SavedXAccount>>(it) }.getOrNull() }.orEmpty()
    }

    /**
     * Guards only changes to the list. It's never held while calling out (XIdentity has its own lock
     * and calls [saveCurrent]), so the two can't wait on each other.
     */
    private val lock = Any()

    private fun update(change: (List<SavedXAccount>) -> List<SavedXAccount>) = synchronized(lock) {
        val list = change(accounts)
        accounts = list
        if (::prefs.isInitialized) prefs.edit().putString("accounts", json.encodeToString(list)).apply()
    }

    /** Keeps the signed-in account's latest cookies (X renews them), so switching back finds it signed in. */
    fun saveCurrent() {
        if (!XView.signedIn) return
        val me = XIdentity.author ?: return
        val cookies = CookieManager.getInstance().getCookie(SITE) ?: return
        update { XAccountList.upsert(it, SavedXAccount(me.handle, me.displayName, me.avatar, cookies)) }
    }

    /** Uses another saved X account. Nothing is signed out. */
    fun switchTo(handle: String) {
        saveCurrent()
        val target = accounts.firstOrNull { XAccountList.same(it.handle, handle) } ?: return
        forgetPage()
        io.github.shoalapps.blobfish.ManagerAccounts.applyCookies(SITE, target.cookies)
        XIdentity.remember(Author("x:@${target.handle}", target.handle, target.name, target.avatar))
        generation++
        XView.open(io.github.shoalapps.blobfish.Graph.app, "$SITE/home")
    }

    /** Keeps the current account and opens X's sign-in page for another. */
    fun startAdding() {
        saveCurrent()
        forgetPage()
        generation++
        XView.open(io.github.shoalapps.blobfish.Graph.app, "$SITE/i/flow/login")
    }

    /** Signs the account in use out of this phone; the others stay. */
    fun signOutCurrent() {
        XIdentity.author?.handle?.let { h -> update { XAccountList.remove(it, h) } }
        XView.signOut()
        generation++
    }

    /** Clears the page's account: its cookies and everything read with them. */
    private fun forgetPage() {
        XBridge.clearAccount()
        io.github.shoalapps.blobfish.data.Engagement.clear()
        clearCookies()
    }

    /**
     * Removes every x.com cookie. Each is expired both as a cookie for all of x.com and as one for
     * the exact host, since a leftover of either kind would be sent alongside the next account's.
     */
    fun clearCookies() {
        val cm = CookieManager.getInstance()
        for (name in XAccountList.cookieNames(cm.getCookie(SITE))) {
            cm.setCookie(SITE, "$name=; Max-Age=0; Path=/; Domain=.x.com")
            cm.setCookie(SITE, "$name=; Max-Age=0; Path=/")
        }
        cm.flush()
    }
}

/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.client

import android.webkit.CookieManager
import com.hippo.ehviewer.EhApplication
import com.hippo.network.CookieDatabase
import com.hippo.network.CookieSet
import com.hippo.util.launchIO
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Collections
import java.util.regex.Pattern

@OptIn(DelicateCoroutinesApi::class)
object EhCookieStore : CookieJar {
    private val cookieManager = CookieManager.getInstance()
    private val db: CookieDatabase = CookieDatabase(EhApplication.application, "okhttp3-cookie.db")
    private val map: MutableMap<String, CookieSet> = db.allCookies
    private val updateLock = Mutex()
    const val KEY_CLOUDFLARE = "cf_clearance"
    const val KEY_HATH_PERKS = "hath_perks"
    const val KEY_IPB_MEMBER_ID = "ipb_member_id"
    const val KEY_IPB_PASS_HASH = "ipb_pass_hash"
    const val KEY_IGNEOUS = "igneous"
    const val KEY_QUOTA = "iq"
    const val KEY_SETTINGS_PROFILE = "sp"
    const val KEY_STAR = "star"
    private const val KEY_UTMP = "__utmp"
    private const val KEY_CONTENT_WARNING = "nw"
    private const val CONTENT_WARNING_NOT_SHOW = "1"
    private val sTipsCookie: Cookie = Cookie.Builder()
        .name(KEY_CONTENT_WARNING)
        .value(CONTENT_WARNING_NOT_SHOW)
        .domain(EhUrl.DOMAIN_E)
        .path("/")
        .expiresAt(Long.MAX_VALUE)
        .build()

    fun hasSignedIn(): Boolean {
        val url = EhUrl.HOST_E.toHttpUrl()
        return contains(url, KEY_IPB_MEMBER_ID) && contains(url, KEY_IPB_PASS_HASH)
    }

    suspend fun copyCookie(domain: String, newDomain: String, name: String, path: String = "/") {
        val cookie = map[domain]?.get(name, domain, path)
        cookie?.let { addCookie(newCookie(it, newDomain)) }
    }

    suspend fun deleteCookie(url: HttpUrl, name: String) {
        val deletedCookie = Cookie.Builder()
            .name(name)
            .value("deleted")
            .domain(url.host)
            .expiresAt(0)
            .build()
        addCookie(deletedCookie)
    }

    suspend fun addCookie(cookie: Cookie) {
        updateLock.withLock {
            // For cookie database
            var toAdd: Cookie? = null
            var toUpdate: Cookie? = null
            var toRemove: Cookie? = null
            var set = map[cookie.domain]
            if (set == null) {
                set = CookieSet()
                map[cookie.domain] = set
            }
            if (cookie.expiresAt <= System.currentTimeMillis()) {
                toRemove = set.remove(cookie)
                // If the cookie is not persistent, it's not in database
                if (toRemove != null && !toRemove.persistent) {
                    toRemove = null
                }
            } else {
                toAdd = cookie
                toUpdate = set.add(cookie)
                // If the cookie is not persistent, it's not in database
                if (!toAdd.persistent) toAdd = null
                if (toUpdate != null && !toUpdate.persistent) toUpdate = null
                // Remove the cookie if it updates to null
                if (toAdd == null && toUpdate != null) {
                    toRemove = toUpdate
                    toUpdate = null
                }
            }
            if (toRemove != null) {
                db.remove(toRemove)
            }
            if (toAdd != null) {
                if (toUpdate != null) {
                    db.update(toUpdate, toAdd)
                } else {
                    db.add(toAdd)
                }
            }
        }
    }

    fun getCookieHeader(url: HttpUrl): String {
        val cookies = getCookies(url)
        val cookieHeader = StringBuilder()
        for (i in cookies.indices) {
            if (i > 0) {
                cookieHeader.append("; ")
            }
            val cookie = cookies[i]
            cookieHeader.append(cookie.name).append('=').append(cookie.value)
        }
        return cookieHeader.toString()
    }

    fun getCookieValue(url: HttpUrl, name: String): String? {
        getCookies(url).forEach {
            if (it.name == name) return it.value
        }
        return null
    }

    @Synchronized
    fun getCookies(url: HttpUrl): List<Cookie> {
        val accepted: MutableList<Cookie> = ArrayList()
        val expired: MutableList<Cookie> = ArrayList()
        for ((domain, cookieSet) in map) {
            if (domainMatch(url, domain)) {
                cookieSet[url, accepted, expired]
            }
        }
        for (cookie in expired) {
            if (cookie.persistent) {
                launchIO {
                    db.remove(cookie)
                }
            }
        }

        // RFC 6265 Section-5.4 step 2, sort the cookie-list
        // Cookies with longer paths are listed before cookies with shorter paths.
        // Ignore creation-time, we don't store them.
        accepted.sortWith { o1: Cookie, o2: Cookie -> o2.path.length - o1.path.length }
        return accepted
    }

    /**
     * Remove all cookies in this `CookieRepository`.
     */
    suspend fun clear() {
        updateLock.withLock {
            map.clear()
            db.clear()
        }
    }

    fun newCookie(
        cookie: Cookie,
        newDomain: String,
        forcePersistent: Boolean = false,
        forceLongLive: Boolean = false,
        forceNotHostOnly: Boolean = false,
    ): Cookie {
        val builder = Cookie.Builder()
        builder.name(cookie.name)
        builder.value(cookie.value)
        if (forceLongLive) {
            builder.expiresAt(Long.MAX_VALUE)
        } else if (cookie.persistent) {
            builder.expiresAt(cookie.expiresAt)
        } else if (forcePersistent) {
            builder.expiresAt(Long.MAX_VALUE)
        }
        if (cookie.hostOnly && !forceNotHostOnly) {
            builder.hostOnlyDomain(newDomain)
        } else {
            builder.domain(newDomain)
        }
        builder.path(cookie.path)
        if (cookie.secure) {
            builder.secure()
        }
        if (cookie.httpOnly) {
            builder.httpOnly()
        }
        return builder.build()
    }

    /**
     * Quick and dirty pattern to differentiate IP addresses from hostnames. This is an approximation
     * of Android's private InetAddress#isNumeric API.
     *
     *
     * This matches IPv6 addresses as a hex string containing at least one colon, and possibly
     * including dots after the first colon. It matches IPv4 addresses as strings containing only
     * decimal digits and dots. This pattern matches strings like "a:.23" and "54" that are neither IP
     * addresses nor hostnames; they will be verified as IP addresses (which is a more strict
     * verification).
     */
    private val VERIFY_AS_IP_ADDRESS =
        Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)")

    /**
     * Returns true if `host` is not a host name and might be an IP address.
     */
    private fun verifyAsIpAddress(host: String): Boolean = VERIFY_AS_IP_ADDRESS.matcher(host).matches()

    // okhttp3.Cookie.domainMatch(HttpUrl, String)
    private fun domainMatch(url: HttpUrl, domain: String?): Boolean {
        val urlHost = url.host
        return if (urlHost == domain) {
            true // As in 'example.com' matching 'example.com'.
        } else {
            urlHost.endsWith(domain!!) &&
                urlHost[urlHost.length - domain.length - 1] == '.' &&
                !verifyAsIpAddress(
                    urlHost,
                )
        }
        // As in 'example.com' matching 'www.example.com'.
    }

    private fun contains(url: HttpUrl, name: String?): Boolean {
        for (cookie in getCookies(url)) {
            if (cookie.name == name) {
                return true
            }
        }
        return false
    }

    fun loadForWebView(url: String, filter: (Cookie) -> Boolean) {
        cookieManager.removeAllCookies(null)
        getCookies(url.toHttpUrl()).forEach {
            if (filter(it)) {
                cookieManager.setCookie(url, it.toString())
            }
        }
    }

    fun saveFromWebView(url: String, filter: (Cookie) -> Boolean): Boolean {
        val cookies = cookieManager.getCookie(url) ?: return false
        var saved = false
        cookies.split(';').forEach { header ->
            Cookie.parse(url.toHttpUrl(), header)?.let {
                if (filter(it)) {
                    launchIO { addCookie(it) }
                    saved = true
                }
            }
        }
        return saved
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = getCookies(url)
        val checkTips = domainMatch(url, EhUrl.DOMAIN_E)
        return if (checkTips) {
            val result: MutableList<Cookie> = ArrayList(cookies.size + 1)
            // Add all but skip some
            for (cookie in cookies) {
                val name = cookie.name
                if (KEY_CONTENT_WARNING == name) {
                    continue
                }
                result.add(cookie)
            }
            // Add some
            result.add(sTipsCookie)
            Collections.unmodifiableList(result)
        } else {
            cookies
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            // See https://github.com/Ehviewer-Overhauled/Ehviewer/issues/873
            if (cookie.name != KEY_UTMP) {
                launchIO { addCookie(cookie) }
            }
        }
    }
}

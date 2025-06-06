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

import com.hippo.ehviewer.Settings
import com.hippo.network.UrlBuilder

object EhUrl {
    const val SITE_E = 0
    const val SITE_EX = 1
    const val DOMAIN_E = "e-hentai.org"
    const val DOMAIN_EX = "exhentai.org"
    const val DOMAIN_LOFI = "lofi.e-hentai.org"
    const val HOST_E = "https://$DOMAIN_E/"
    const val HOST_EX = "https://$DOMAIN_EX/"
    private const val API_E = "https://api.e-hentai.org/api.php"
    private const val API_EX = "https://s.exhentai.org/api.php"
    private const val URL_FAVORITES_E = HOST_E + "favorites.php"
    private const val URL_FAVORITES_EX = HOST_EX + "favorites.php"
    private const val URL_IMAGE_SEARCH_E = "https://upload.e-hentai.org/image_lookup.php"
    private const val URL_IMAGE_SEARCH_EX = "https://exhentai.org/upload/image_lookup.php"
    private const val URL_MY_TAGS_E = HOST_E + "mytags"
    private const val URL_MY_TAGS_EX = HOST_EX + "mytags"
    private const val URL_POPULAR_E = HOST_E + "popular"
    private const val URL_POPULAR_EX = HOST_EX + "popular"
    private const val URL_WATCHED_E = HOST_E + "watched"
    private const val URL_WATCHED_EX = HOST_EX + "watched"
    const val URL_UCONFIG_E = HOST_E + "uconfig.php"
    const val URL_UCONFIG_EX = HOST_EX + "uconfig.php"
    const val URL_FORUMS = "https://forums.e-hentai.org/"
    const val URL_SIGN_IN = URL_FORUMS + "index.php?act=Login"
    const val URL_REGISTER = URL_FORUMS + "index.php?act=Reg&CODE=00"
    const val API_SIGN_IN = "$URL_SIGN_IN&CODE=01"
    const val URL_FUNDS = HOST_E + "exchange.php?t=gp"
    const val URL_HOME = HOST_E + "home.php"
    const val URL_NEWS = HOST_E + "news.php"
    const val REFERER_E = "https://$DOMAIN_E"
    private const val REFERER_EX = "https://$DOMAIN_EX"
    private const val ORIGIN_E = REFERER_E
    private const val ORIGIN_EX = REFERER_EX

    val host: String
        get() = when (Settings.gallerySite) {
            SITE_E -> HOST_E
            SITE_EX -> HOST_EX
            else -> HOST_E
        }

    val favoritesUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_FAVORITES_E
            SITE_EX -> URL_FAVORITES_EX
            else -> URL_FAVORITES_E
        }

    val apiUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> API_E
            SITE_EX -> API_EX
            else -> API_E
        }

    val referer: String
        get() = when (Settings.gallerySite) {
            SITE_E -> REFERER_E
            SITE_EX -> REFERER_EX
            else -> REFERER_E
        }

    val origin: String
        get() = when (Settings.gallerySite) {
            SITE_E -> ORIGIN_E
            SITE_EX -> ORIGIN_EX
            else -> ORIGIN_E
        }

    val uConfigUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_UCONFIG_E
            SITE_EX -> URL_UCONFIG_EX
            else -> URL_UCONFIG_E
        }

    val myTagsUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_MY_TAGS_E
            SITE_EX -> URL_MY_TAGS_EX
            else -> URL_MY_TAGS_E
        }

    val popularUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_POPULAR_E
            SITE_EX -> URL_POPULAR_EX
            else -> URL_POPULAR_E
        }

    val imageSearchUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_IMAGE_SEARCH_E
            SITE_EX -> URL_IMAGE_SEARCH_EX
            else -> URL_IMAGE_SEARCH_E
        }

    val watchedUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_WATCHED_E
            SITE_EX -> URL_WATCHED_EX
            else -> URL_WATCHED_E
        }

    fun getGalleryDetailUrl(
        gid: Long,
        token: String?,
        index: Int = 0,
        allComment: Boolean = false,
        sha1: Boolean = false,
    ): String {
        val builder = UrlBuilder(host + "g/" + gid + '/' + token + '/')
        if (index > 0) builder.addQuery("p", index)
        if (allComment) builder.addQuery("hc", 1)
        if (sha1) builder.addQuery("datatags", 1)
        return builder.build()
    }

    fun getGalleryMultiPageViewerUrl(gid: Long, token: String, sha1: Boolean = false): String {
        val builder = UrlBuilder(host + "mpv/" + gid + '/' + token + '/')
        if (sha1) builder.addQuery("datatags", 1)
        return builder.build()
    }

    fun getPageUrl(gid: Long, index: Int, pToken: String): String = host + "s/" + pToken + '/' + gid + '-' + (index + 1)

    fun getAddFavorites(gid: Long, token: String?): String = host + "gallerypopups.php?gid=" + gid + "&t=" + token + "&act=addfav"

    fun getDownloadArchive(gid: Long, token: String?): String = host + "archiver.php?gid=" + gid + "&token=" + token

    fun getTagDefinitionUrl(tag: String): String = "https://ehwiki.org/wiki/" + tag.replace(' ', '_')
}

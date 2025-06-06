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
package com.hippo.ehviewer.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.hippo.app.EditTextCheckBoxDialogBuilder
import com.hippo.app.ListCheckBoxDialogBuilder
import com.hippo.ehviewer.EhApplication.Companion.application
import com.hippo.ehviewer.EhApplication.Companion.favouriteStatusRouter
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.unifile.UniFile
import com.hippo.util.isAtLeastT
import com.hippo.yorozuya.collect.LongList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CommonOperations {
    private fun doAddToFavorites(
        activity: Activity,
        galleryInfo: GalleryInfo,
        slot: Int,
        note: String,
        listener: EhClient.Callback<Unit>,
    ) {
        val request = EhRequest()
        request.setMethod(EhClient.METHOD_ADD_FAVORITES)
        request.setArgs(galleryInfo.gid, galleryInfo.token, slot, note)
        request.setCallback(listener)
        request.enqueue(activity)
    }

    private fun doAddToFavorites(
        activity: Activity,
        galleryInfo: GalleryInfo,
        slot: Int,
        listener: EhClient.Callback<Unit>,
        foreEdit: Boolean,
    ) {
        when (slot) {
            -1 -> {
                EhDB.putLocalFavorites(galleryInfo)
                listener.onSuccess(Unit)
            }
            in 0..9 -> {
                if (!foreEdit && Settings.neverAddFavNotes) {
                    doAddToFavorites(activity, galleryInfo, slot, "", listener)
                } else {
                    val builder = EditTextCheckBoxDialogBuilder(
                        activity,
                        null,
                        activity.getString(R.string.favorite_note),
                        activity.getString(R.string.favorite_note_never_show),
                        Settings.neverAddFavNotes,
                    )
                    builder.setTitle(R.string.add_favorite_note_dialog_title)
                    builder.setPositiveButton(android.R.string.ok, null)
                    val dialog = builder.show()
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        val text = builder.text.trim { it <= ' ' }
                        Settings.putNeverAddFavNotes(builder.isChecked)
                        dialog.dismiss()
                        doAddToFavorites(activity, galleryInfo, slot, text, listener)
                    }
                    dialog.setOnCancelListener { listener.onCancel() }
                }
            }
            else -> {
                listener.onFailure(Exception()) // TODO Add text
            }
        }
    }

    fun addToFavorites(
        activity: Activity,
        galleryInfo: GalleryInfo,
        listener: EhClient.Callback<Unit>,
        foreSelect: Boolean = false,
    ) {
        val slot = Settings.defaultFavSlot
        val localFav = activity.getString(R.string.local_favorites)
        val items = Settings.favCat.toMutableList().apply { add(0, localFav) }
        if (!foreSelect && slot in -1..9) {
            val newFavoriteName = if (slot >= 0) items[slot + 1] else null
            doAddToFavorites(
                activity,
                galleryInfo,
                slot,
                DelegateFavoriteCallback(listener, galleryInfo, newFavoriteName, slot),
                false,
            )
        } else {
            ListCheckBoxDialogBuilder(
                activity,
                items,
                { builder: ListCheckBoxDialogBuilder?, _: AlertDialog?, position: Int ->
                    val slot1 = position - 1
                    val newFavoriteName = if (slot1 in 0..9) items[slot1 + 1] else null
                    doAddToFavorites(
                        activity,
                        galleryInfo,
                        slot1,
                        DelegateFavoriteCallback(listener, galleryInfo, newFavoriteName, slot1),
                        foreSelect,
                    )
                    if (builder?.isChecked == true) {
                        Settings.putDefaultFavSlot(slot1)
                    } else {
                        Settings.putDefaultFavSlot(Settings.INVALID_DEFAULT_FAV_SLOT)
                    }
                },
                activity.getString(R.string.remember_favorite_collection),
                slot != Settings.INVALID_DEFAULT_FAV_SLOT,
            )
                .setTitle(R.string.add_favorites_dialog_title)
                .setOnCancelListener { listener.onCancel() }
                .show()
        }
    }

    fun removeFromFavorites(
        activity: Activity?,
        galleryInfo: GalleryInfo,
        listener: EhClient.Callback<Unit>,
        isLocal: Boolean = false,
    ) {
        EhDB.removeLocalFavorites(galleryInfo.gid)
        if (isLocal) {
            EhDB.updateHistoryFavSlot(galleryInfo.gid, -2)
            listener.onSuccess(Unit)
        } else {
            val request = EhRequest()
            request.setMethod(EhClient.METHOD_ADD_FAVORITES)
            request.setArgs(galleryInfo.gid, galleryInfo.token, -1, "")
            request.setCallback(DelegateFavoriteCallback(listener, galleryInfo, null, -2))
            request.enqueue(activity!!)
        }
    }

    fun startDownload(activity: MainActivity, galleryInfo: GalleryInfo, forceDefault: Boolean) {
        startDownload(activity, listOf(galleryInfo), forceDefault)
    }

    fun startDownload(
        activity: MainActivity,
        galleryInfos: List<GalleryInfo>,
        forceDefault: Boolean,
    ) {
        if (isAtLeastT) {
            application.topActivity?.checkAndRequestNotificationPermission()
        }
        doStartDownload(activity, galleryInfos, forceDefault)
    }

    private fun doStartDownload(
        activity: MainActivity,
        galleryInfos: List<GalleryInfo>,
        forceDefault: Boolean,
    ) {
        val toStart = LongList()
        val toAdd: MutableList<GalleryInfo> = ArrayList()
        for (gi in galleryInfos) {
            if (DownloadManager.containDownloadInfo(gi.gid)) {
                toStart.add(gi.gid)
            } else {
                toAdd.add(gi)
            }
        }
        if (!toStart.isEmpty()) {
            val intent = Intent(activity, DownloadService::class.java)
            intent.action = DownloadService.ACTION_START_RANGE
            intent.putExtra(DownloadService.KEY_GID_LIST, toStart)
            ContextCompat.startForegroundService(activity, intent)
        }
        if (toAdd.isEmpty()) {
            activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
            return
        }
        var justStart = forceDefault
        var label: String? = null
        // Get default download label
        if (!justStart && Settings.hasDefaultDownloadLabel) {
            label = Settings.defaultDownloadLabel
            justStart = label == null || DownloadManager.containLabel(label)
        }
        // If there is no other label, just use null label
        if (!justStart && DownloadManager.labelList.isEmpty()) {
            justStart = true
            label = null
        }
        if (justStart) {
            // Got default label
            for (gi in toAdd) {
                val intent = Intent(activity, DownloadService::class.java)
                intent.action = DownloadService.ACTION_START
                intent.putExtra(DownloadService.KEY_LABEL, label)
                intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi)
                ContextCompat.startForegroundService(activity, intent)
            }
            // Notify
            activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
        } else {
            // Let use chose label
            val list = DownloadManager.labelList
            val items = mutableListOf<String>()
            items.add(activity.getString(R.string.default_download_label_name))
            items.addAll(list.mapNotNull { it.label })
            ListCheckBoxDialogBuilder(
                activity,
                items,
                { builder: ListCheckBoxDialogBuilder?, _: AlertDialog?, position: Int ->
                    var label1: String?
                    if (position == 0) {
                        label1 = null
                    } else {
                        label1 = items[position]
                        if (!DownloadManager.containLabel(label1)) {
                            label1 = null
                        }
                    }
                    // Start download
                    for (gi in toAdd) {
                        val intent = Intent(activity, DownloadService::class.java)
                        intent.action = DownloadService.ACTION_START
                        intent.putExtra(DownloadService.KEY_LABEL, label1)
                        intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi)
                        ContextCompat.startForegroundService(activity, intent)
                    }
                    // Save settings
                    if (builder?.isChecked == true) {
                        Settings.putHasDefaultDownloadLabel(true)
                        Settings.putDefaultDownloadLabel(label1)
                    } else {
                        Settings.putHasDefaultDownloadLabel(false)
                    }
                    // Notify
                    activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
                },
                activity.getString(R.string.remember_download_label),
                false,
            )
                .setTitle(R.string.download)
                .show()
        }
    }

    private class DelegateFavoriteCallback(
        private val delegate: EhClient.Callback<Unit>,
        private val info: GalleryInfo,
        private val newFavoriteName: String?,
        private val slot: Int,
    ) : EhClient.Callback<Unit> {
        override fun onSuccess(result: Unit) {
            EhDB.updateHistoryFavSlot(info.gid, slot)
            info.favoriteName = newFavoriteName
            info.favoriteSlot = slot
            delegate.onSuccess(result)
            favouriteStatusRouter.modifyFavourites(info.gid, slot)
        }

        override fun onFailure(e: Exception) {
            delegate.onFailure(e)
        }

        override fun onCancel() {
            delegate.onCancel()
        }
    }
}

private fun removeNoMediaFile(downloadDir: UniFile) {
    val noMedia = downloadDir.subFile(".nomedia") ?: return
    noMedia.delete()
}

private fun ensureNoMediaFile(downloadDir: UniFile) {
    downloadDir.createFile(".nomedia") ?: return
}

private val lck = Mutex()

suspend fun keepNoMediaFileStatus() {
    lck.withLock {
        val downloadLocation = Settings.downloadLocation ?: return
        if (Settings.mediaScan) {
            removeNoMediaFile(downloadLocation)
        } else {
            ensureNoMediaFile(downloadLocation)
        }
    }
}

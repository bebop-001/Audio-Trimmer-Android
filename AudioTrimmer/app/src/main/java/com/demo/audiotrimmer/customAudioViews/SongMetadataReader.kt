/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.demo.audiotrimmer.customAudioViews

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class SongMetadataReader(activity: Activity?, filename: String) {
    var GENRES_URI = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
    var mActivity: Activity? = null
    var mFilename = ""
    var mTitle: String? = ""
    var mArtist: String? = ""
    var mAlbum: String? = ""
    var mGenre = ""
    var mYear = -1

    init {
        mActivity = activity
        mFilename = filename
        mTitle = _getBasename(filename)
        try {
            ReadMetadata()
        } catch (e: Exception) {
        }
    }

    private fun ReadMetadata() {
        // Get a map from genre ids to names
        val genreIdMap = HashMap<String, String>()
        var c = mActivity!!.contentResolver.query(
            GENRES_URI, arrayOf(
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME
            ),
            null, null, null
        )
        c!!.moveToFirst()
        while (!c.isAfterLast) {
            genreIdMap[c.getString(0)] = c.getString(1)
            c.moveToNext()
        }
        c.close()
        mGenre = ""
        for (genreId in genreIdMap.keys) {
            c = mActivity!!.contentResolver.query(
                makeGenreUri(genreId), arrayOf(MediaStore.Audio.Media.DATA),
                MediaStore.Audio.Media.DATA + " LIKE \"" + mFilename + "\"",
                null, null
            )
            if (c!!.count != 0) {
                mGenre = genreIdMap[genreId]!!
                break
            }
            c.close()
            c = null
        }
        val uri = MediaStore.Audio.Media.getContentUriForPath(mFilename)
        c = mActivity!!.contentResolver.query(
            uri!!, arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATA
            ),
            MediaStore.Audio.Media.DATA + " LIKE \"" + mFilename + "\"",
            null, null
        )
        if (c!!.count == 0) {
            mTitle = _getBasename(mFilename)
            mArtist = ""
            mAlbum = ""
            mYear = -1
            return
        }
        c.moveToFirst()
        mTitle = _getStringFromColumn(c, MediaStore.Audio.Media.TITLE)
        if (mTitle == null || mTitle!!.length == 0) {
            mTitle = _getBasename(mFilename)
        }
        mArtist = _getStringFromColumn(c, MediaStore.Audio.Media.ARTIST)
        mAlbum = _getStringFromColumn(c, MediaStore.Audio.Media.ALBUM)
        mYear = _getIntegerFromColumn(c, MediaStore.Audio.Media.YEAR)
        c.close()
    }

    private fun makeGenreUri(genreId: String): Uri {
        val CONTENTDIR = MediaStore.Audio.Genres.Members.CONTENT_DIRECTORY
        return Uri.parse(
            StringBuilder()
                .append(GENRES_URI.toString())
                .append("/")
                .append(genreId)
                .append("/")
                .append(CONTENTDIR)
                .toString()
        )
    }

    private fun _getStringFromColumn(c: Cursor?, columnName: String): String? {
        val index = c!!.getColumnIndexOrThrow(columnName)
        val value = c.getString(index)
        return if (value != null && value.length > 0) {
            value
        } else {
            null
        }
    }

    private fun _getIntegerFromColumn(c: Cursor?, columnName: String): Int {
        val index = c!!.getColumnIndexOrThrow(columnName)
        val value = c.getInt(index)
        return value ?: -1
    }

    private fun _getBasename(filename: String): String {
        return filename.substring(
            filename.lastIndexOf('/') + 1,
            filename.lastIndexOf('.')
        )
    }
}

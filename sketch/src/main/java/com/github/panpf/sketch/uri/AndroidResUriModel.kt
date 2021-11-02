/*
 * Copyright (C) 2019 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch.uri

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.github.panpf.sketch.datasource.ContentDataSource
import com.github.panpf.sketch.datasource.DataSource
import com.github.panpf.sketch.request.DownloadResult

class AndroidResUriModel : UriModel() {

    companion object {
        const val SCHEME = "android.resource://"

        /**
         * 根据资源名称和类型创建 uri
         *
         * @param packageName     包名
         * @param resType         资源类型，例如 "drawable" 或 "mipmap"
         * @param drawableResName 图片资源名称
         * @return 例如：android.resource://com.github.panpf.sketch.sample/mipmap/ic_launch
         */
        @JvmStatic
        fun makeUriByName(packageName: String, resType: String, drawableResName: String): String {
            return "$SCHEME$packageName/$resType/$drawableResName"
        }

        /**
         * 根据资源 ID 创建 uri
         *
         * @param packageName   包名
         * @param drawableResId 图片资源ID
         * @return 例如：android.resource://com.github.panpf.sketch.sample/1031232
         */
        @JvmStatic
        fun makeUriById(packageName: String, drawableResId: Int): String {
            return "$SCHEME$packageName/$drawableResId"
        }
    }

    override fun match(uri: String): Boolean {
        return !TextUtils.isEmpty(uri) && uri.startsWith(SCHEME)
    }

    override fun getDataSource(
        context: Context,
        uri: String,
        downloadResult: DownloadResult?
    ): DataSource {
        return ContentDataSource(context, Uri.parse(uri))
    }
}
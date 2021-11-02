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
import android.text.TextUtils
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.InputStream

open class Base64UriModel : AbsStreamDiskCacheUriModel() {

    companion object {
        const val SCHEME = "data:image/"
    }

    override fun match(uri: String): Boolean {
        return !TextUtils.isEmpty(uri) && uri.startsWith(SCHEME)
    }

    /**
     * 获取 uri 所真正包含的内容部分，例如 "data:image/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z"，就会返回 "/9j/4QaORX...C8bg/U7T/in//Z"
     *
     * @param uri 图片 uri
     * @return uri 所真正包含的内容部分，例如 "data:image/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z"，就会返回 "/9j/4QaORX...C8bg/U7T/in//Z"
     */
    override fun getUriContent(uri: String): String {
        return if (!TextUtils.isEmpty(uri)) uri.substring(uri.indexOf(";") + ";base64,".length) else uri
    }

    override fun getDiskCacheKey(uri: String): String {
        return getUriContent(uri)
    }

    override val isConvertShortUriForKey: Boolean
        get() = true

    @Throws(GetDataSourceException::class)
    override fun getContent(context: Context, uri: String): InputStream {
        return ByteArrayInputStream(Base64.decode(getUriContent(uri), Base64.DEFAULT))
    }
}
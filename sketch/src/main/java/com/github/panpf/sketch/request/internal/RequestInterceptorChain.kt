/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
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
package com.github.panpf.sketch.request.internal

import androidx.annotation.MainThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.RequestInterceptor
import com.github.panpf.sketch.util.requiredMainThread

internal class RequestInterceptorChain(
    override val sketch: Sketch,
    override val initialRequest: ImageRequest,
    override val request: ImageRequest,
    override val requestContext: RequestContext,
    private val interceptors: List<RequestInterceptor>,
    private val index: Int,
) : RequestInterceptor.Chain {

    @MainThread
    override suspend fun proceed(request: ImageRequest): ImageData {
        requiredMainThread()
        requestContext.addRequest(request)

        val interceptor = interceptors[index]
        val next = RequestInterceptorChain(
            sketch, initialRequest, request, requestContext, interceptors, index + 1
        )
        return interceptor.intercept(next)
    }
}

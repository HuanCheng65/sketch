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
package com.github.panpf.sketch.zoom.block.internal

import com.github.panpf.sketch.zoom.block.internal.ObjectPool.ObjectFactory
import java.util.LinkedList
import java.util.Queue

class ObjectPool<T>(
    private val objectFactory: ObjectFactory<T>,
    private var maxPoolSize: Int = MAX_POOL_SIZE
) {

    companion object {
        private const val MAX_POOL_SIZE = 10
    }

    private val editLock = Any()
    private val cacheQueue: Queue<T>

    constructor(classType: Class<T>, maxPoolSize: Int = MAX_POOL_SIZE) : this(ObjectFactory<T> {
        try {
            classType.newInstance()
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }, maxPoolSize)

    init {
        cacheQueue = LinkedList()
    }

    fun get(): T {
        synchronized(editLock) {
            val t = if (!cacheQueue.isEmpty()) cacheQueue.poll() else objectFactory.newObject()
            if (t is CacheStatus) {
                (t as CacheStatus).isInCachePool = false
            }
            return t
        }
    }

    fun put(t: T) {
        synchronized(editLock) {
            if (cacheQueue.size < maxPoolSize) {
                if (t is CacheStatus) {
                    (t as CacheStatus).isInCachePool = true
                }
                cacheQueue.add(t)
            }
        }
    }

    fun clear() {
        synchronized(editLock) { cacheQueue.clear() }
    }

    fun getMaxPoolSize(): Int {
        return maxPoolSize
    }

    fun setMaxPoolSize(maxPoolSize: Int) {
        this.maxPoolSize = maxPoolSize
        synchronized(editLock) {
            if (cacheQueue.size > maxPoolSize) {
                val number = maxPoolSize - cacheQueue.size
                var count = 0
                while (count++ < number) {
                    cacheQueue.poll()
                }
            }
        }
    }

    fun size(): Int {
        synchronized(editLock) { return cacheQueue.size }
    }

    fun interface ObjectFactory<T> {
        fun newObject(): T
    }

    interface CacheStatus {
        var isInCachePool: Boolean
    }
}
package com.github.panpf.sketch.resize

import com.github.panpf.sketch.util.Size

internal class RealSizeResolver(private val size: Size) : SizeResolver {

    override suspend fun size() = size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RealSizeResolver && size == other.size
    }

    override fun hashCode() = size.hashCode()
}
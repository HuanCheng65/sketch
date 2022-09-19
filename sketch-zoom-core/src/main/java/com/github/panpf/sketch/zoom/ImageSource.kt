package com.github.panpf.sketch.zoom

import android.content.Context
import android.content.res.Resources
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

interface ImageSource {

    val key: String

    fun newInputStream(): InputStream
}

class FileImageSource(private val file: File) : ImageSource {

    override val key: String = file.path

    override fun newInputStream(): InputStream {
        return FileInputStream(file)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileImageSource
        if (file.path != other.file.path) return false
        return true
    }

    override fun hashCode(): Int {
        return file.path.hashCode()
    }

    override fun toString(): String {
        return "FileDataSource(${file.path})"
    }
}

class AssetImageSource(
    private val context: Context,
    private val assetFileName: String
) : ImageSource {

    override val key: String = "asset://$assetFileName"

    override fun newInputStream(): InputStream {
        return context.assets.open(assetFileName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AssetImageSource
        if (context != other.context) return false
        if (assetFileName != other.assetFileName) return false
        return true
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + assetFileName.hashCode()
        return result
    }

    override fun toString(): String {
        return "AssetDataSource(context=$context, assetFileName='$assetFileName')"
    }
}

class ResourceImageSource(
    private val resources: Resources,
    private val resId: Int
) : ImageSource {

    override val key: String = "resource://${resources.getResourceName(resId)}"

    override fun newInputStream(): InputStream {
        return resources.openRawResource(resId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ResourceImageSource
        if (resources != other.resources) return false
        if (resId != other.resId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = resources.hashCode()
        result = 31 * result + resId
        return result
    }

    override fun toString(): String {
        return "ResourceDataSource(resources=$resources, resId=$resId)"
    }
}
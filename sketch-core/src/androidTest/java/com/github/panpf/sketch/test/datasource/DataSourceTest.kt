package com.github.panpf.sketch.test.datasource

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.AssetDataSource
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.tools4j.test.ktx.assertThrow
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileNotFoundException

@RunWith(AndroidJUnit4::class)
class DataSourceTest {

    @Test
    fun testFile() {
        val context = InstrumentationRegistry.getContext()
        val sketch = Sketch.new(context)
        AssetDataSource(
            sketch = sketch,
            request = LoadRequest(newAssetUri("fd5717876ab046b8aa889c9aaac4b56c.jpeg")),
            assetFileName = "fd5717876ab046b8aa889c9aaac4b56c.jpeg"
        ).apply {
            val file = runBlocking {
                file()
            }
            Assert.assertTrue(file.path.contains("/cache/"))
            val file1 = runBlocking {
                file()
            }
            Assert.assertEquals(file.path, file1.path)
        }

        assertThrow(FileNotFoundException::class) {
            AssetDataSource(
                sketch = sketch,
                request = LoadRequest(newAssetUri("not_found.jpeg")),
                assetFileName = "not_found.jpeg"
            ).apply {
                runBlocking {
                    file()
                }
            }
        }
    }
}
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
package com.github.panpf.sketch.extensions.test.util

import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDisplayInterceptor
import com.github.panpf.sketch.util.PauseLoadWhenScrollingMixedScrollListener
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PauseLoadWhenScrollingMixedScrollListenerTest {

    @Test
    fun testRecyclerView() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val recyclerView = RecyclerView(context).apply {
            adapter = object : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    throw UnsupportedOperationException()
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {

                }

                override fun getItemCount(): Int = 0
            }
        }
        val listener = PauseLoadWhenScrollingMixedScrollListener()

        Assert.assertFalse(PauseLoadWhenScrollingDisplayInterceptor.scrolling)

        listener.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING)
        Assert.assertTrue(PauseLoadWhenScrollingDisplayInterceptor.scrolling)

        listener.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING)
        Assert.assertTrue(PauseLoadWhenScrollingDisplayInterceptor.scrolling)

        listener.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE)
        Assert.assertFalse(PauseLoadWhenScrollingDisplayInterceptor.scrolling)

        listener.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE)
        Assert.assertFalse(PauseLoadWhenScrollingDisplayInterceptor.scrolling)
    }

    @Test
    fun testListView() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val recyclerView = ListView(context).apply {
            adapter = object : BaseAdapter() {
                override fun getCount(): Int = 0

                override fun getItem(position: Int): Any = ""

                override fun getItemId(position: Int): Long = 0

                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    throw UnsupportedOperationException()
                }
            }
        }
        val listener = PauseLoadWhenScrollingMixedScrollListener()

        Assert.assertFalse(PauseLoadWhenScrollingDisplayInterceptor.scrolling)

        listener.onScrollStateChanged(
            recyclerView,
            AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
        )
        Assert.assertTrue(PauseLoadWhenScrollingDisplayInterceptor.scrolling)

        listener.onScrollStateChanged(
            recyclerView,
            AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
        )
        Assert.assertTrue(PauseLoadWhenScrollingDisplayInterceptor.scrolling)

        listener.onScrollStateChanged(recyclerView, AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
        Assert.assertFalse(PauseLoadWhenScrollingDisplayInterceptor.scrolling)

        listener.onScrollStateChanged(recyclerView, AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
        Assert.assertFalse(PauseLoadWhenScrollingDisplayInterceptor.scrolling)
    }
}
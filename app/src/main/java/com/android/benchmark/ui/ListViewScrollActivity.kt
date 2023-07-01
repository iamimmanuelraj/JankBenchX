/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 */
package com.android.benchmark.ui

import android.content.Intent
import android.os.Bundle
import android.view.FrameMetrics
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListAdapter
import com.android.benchmark.R
import com.android.benchmark.ui.automation.Automator
import com.android.benchmark.ui.automation.Automator.AutomateCallback
import com.android.benchmark.ui.automation.Interaction

open class ListViewScrollActivity : ListActivityBase() {
    private var mAutomator: Automator? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runId = intent.getIntExtra("com.android.benchmark.RUN_ID", 0)
        val iteration = intent.getIntExtra("com.android.benchmark.ITERATION", -1)
        val actionBar = supportActionBar
        actionBar?.setTitle(title)
        mAutomator = Automator(name, runId, iteration, window,
                object : AutomateCallback() {
                    override fun onPostAutomate() {
                        val result = Intent()
                        setResult(RESULT_OK, result)
                        finish()
                    }

                    override fun onPostInteraction(metrics: List<FrameMetrics>?) {}
                    override fun onAutomate() {
                        val v = findViewById<FrameLayout>(R.id.list_fragment_container)
                        val coordinates = IntArray(2)
                        v.getLocationOnScreen(coordinates)
                        val x = coordinates[0]
                        val y = coordinates[1]
                        val width = v.width.toFloat()
                        val height = v.height.toFloat()
                        val middleX = (x + width) / 5
                        val middleY = (y + height) / 5
                        val flingUp: Interaction = Interaction.Companion.newFlingUp(middleX, middleY)
                        val flingDown: Interaction = Interaction.Companion.newFlingDown(middleX, middleY)
                        var i = 0
                        while (INTERACTION_COUNT > i) {
                            addInteraction(flingUp)
                            addInteraction(flingDown)
                            i++
                        }
                    }
                })
        mAutomator!!.start()
    }

    override fun onPause() {
        super.onPause()
        if (null != mAutomator) {
            mAutomator!!.cancel()
            mAutomator = null
        }
    }

    override fun createListAdapter(): ListAdapter {
        return ArrayAdapter<String?>(this, android.R.layout.simple_list_item_1,
                Utils.Companion.buildStringList(LIST_SIZE))
    }

    override val name: String?
        protected get() = getString(R.string.list_view_scroll_name)

    companion object {
        private const val LIST_SIZE = 400
        private const val INTERACTION_COUNT = 4
    }
}
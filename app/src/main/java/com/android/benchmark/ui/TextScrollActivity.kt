/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.benchmark.ui

android.os.Bundleimport android.widget.ArrayAdapterimport android.widget.ListAdapterimport android.widget.ListViewimport com.android.benchmark.registry.BenchmarkRegistryimport com.android.benchmark.ui.automation.Automatorimport com.android.benchmark.ui.automation.Automator.AutomateCallbackimport com.android.benchmark.ui.automation.Interaction
class TextScrollActivity : ListActivityBase() {
    private var mHitPercentage = 100
    private var mAutomator: Automator? = null
    override var name: String? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        mHitPercentage = intent.getIntExtra(EXTRA_HIT_RATE,
                mHitPercentage)
        super.onCreate(savedInstanceState)
        val runId = intent.getIntExtra("com.android.benchmark.RUN_ID", 0)
        val iteration = intent.getIntExtra("com.android.benchmark.ITERATION", -1)
        val id = intent.getIntExtra(BenchmarkRegistry.Companion.EXTRA_ID, -1)
        if (-1 == id) {
            finish()
            return
        }
        name = BenchmarkRegistry.Companion.getBenchmarkName(this, id)
        mAutomator = Automator(name, runId, iteration, window,
                object : AutomateCallback() {
                    override fun onPostAutomate() {
                        val result = Intent()
                        setResult(RESULT_OK, result)
                        finish()
                    }

                    override fun onAutomate() {
                        val v = findViewById<ListView>(android.R.id.list)
                        val coordinates = IntArray(2)
                        v.getLocationOnScreen(coordinates)
                        val x = coordinates[0]
                        val y = coordinates[1]
                        val width = v.width.toFloat()
                        val height = v.height.toFloat()
                        val middleX = (x + width) / 2
                        val middleY = (y + height) / 2
                        val flingUp: Interaction = Interaction.Companion.newFlingUp(middleX, middleY)
                        val flingDown: Interaction = Interaction.Companion.newFlingDown(middleX, middleY)
                        addInteraction(flingUp)
                        addInteraction(flingDown)
                        addInteraction(flingUp)
                        addInteraction(flingDown)
                        addInteraction(flingUp)
                        addInteraction(flingDown)
                        addInteraction(flingUp)
                        addInteraction(flingDown)
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
                Utils.Companion.buildParagraphListWithHitPercentage(PARAGRAPH_COUNT, 80))
    }

    companion object {
        const val EXTRA_HIT_RATE = ".TextScrollActivity.EXTRA_HIT_RATE"
        private const val PARAGRAPH_COUNT = 200
    }
}
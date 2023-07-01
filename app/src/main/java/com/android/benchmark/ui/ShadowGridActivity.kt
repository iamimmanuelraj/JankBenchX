/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.benchmark.ui

import android.content.Intentimport

android.os.Bundleimport android.view.Viewimport android.widget.ArrayAdapterimport android.widget.ListViewimport androidx.appcompat.app.AppCompatActivityimport androidx.fragment.app.ListFragmentimport com.android.benchmark.Rimport com.android.benchmark.ui.automation.Automatorimport com.android.benchmark.ui.automation.Automator.AutomateCallbackimport com.android.benchmark.ui.automation.Interaction
class ShadowGridActivity : AppCompatActivity() {
    private var mAutomator: Automator? = null

    class MyListFragment : ListFragment() {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.divider = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runId = intent.getIntExtra("com.android.benchmark.RUN_ID", 0)
        val iteration = intent.getIntExtra("com.android.benchmark.ITERATION", -1)
        val fm = supportFragmentManager
        if (null == fm.findFragmentById(android.R.id.content)) {
            val listFragment: ListFragment = MyListFragment()
            listFragment.listAdapter = ArrayAdapter<String?>(this,
                    R.layout.card_row, R.id.card_text, Utils.Companion.buildStringList(200))
            fm.beginTransaction().add(android.R.id.content, listFragment).commit()
            val testName = getString(R.string.shadow_grid_name)
            mAutomator = Automator(testName, runId, iteration, window,
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
    }

    override fun onPause() {
        super.onPause()
        if (null != mAutomator) {
            mAutomator!!.cancel()
            mAutomator = null
        }
    }
}
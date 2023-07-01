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

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.android.benchmark.R
import com.android.benchmark.ui.automation.Automator
import com.android.benchmark.ui.automation.Automator.AutomateCallback
import com.android.benchmark.ui.automation.Interaction

class EditTextInputActivity : AppCompatActivity() {
    private var mAutomator: Automator? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editText = EditText(this)
        val runId = intent.getIntExtra("com.android.benchmark.RUN_ID", 0)
        val iteration = intent.getIntExtra("com.android.benchmark.ITERATION", -1)
        editText.width = 400
        editText.height = 200
        setContentView(editText)
        val testName = getString(R.string.edit_text_input_name)
        val actionBar = supportActionBar
        actionBar?.setTitle(testName)
        mAutomator = Automator(testName, runId, iteration, window,
                object : AutomateCallback() {
                    override fun onPostAutomate() {
                        val result = Intent()
                        setResult(RESULT_OK, result)
                        finish()
                    }

                    override fun onAutomate() {
                        val coordinates = IntArray(2)
                        editText.getLocationOnScreen(coordinates)
                        val x = coordinates[0]
                        val y = coordinates[1]
                        val width = editText.width.toFloat()
                        val height = editText.height.toFloat()
                        val middleX = (x + width) / 2
                        val middleY = (y + height) / 2
                        val tap: Interaction = Interaction.Companion.newTap(middleX, middleY)
                        addInteraction(tap)
                        val alphabet = intArrayOf(
                                KeyEvent.KEYCODE_A,
                                KeyEvent.KEYCODE_B,
                                KeyEvent.KEYCODE_C,
                                KeyEvent.KEYCODE_D,
                                KeyEvent.KEYCODE_E,
                                KeyEvent.KEYCODE_F,
                                KeyEvent.KEYCODE_G,
                                KeyEvent.KEYCODE_H,
                                KeyEvent.KEYCODE_I,
                                KeyEvent.KEYCODE_J,
                                KeyEvent.KEYCODE_K,
                                KeyEvent.KEYCODE_L,
                                KeyEvent.KEYCODE_M,
                                KeyEvent.KEYCODE_N,
                                KeyEvent.KEYCODE_O,
                                KeyEvent.KEYCODE_P,
                                KeyEvent.KEYCODE_Q,
                                KeyEvent.KEYCODE_R,
                                KeyEvent.KEYCODE_S,
                                KeyEvent.KEYCODE_T,
                                KeyEvent.KEYCODE_U,
                                KeyEvent.KEYCODE_V,
                                KeyEvent.KEYCODE_W,
                                KeyEvent.KEYCODE_X,
                                KeyEvent.KEYCODE_Y,
                                KeyEvent.KEYCODE_Z,
                                KeyEvent.KEYCODE_SPACE
                        )
                        val typeAlphabet: Interaction = Interaction.Companion.newKeyInput(intArrayOf(
                                KeyEvent.KEYCODE_A,
                                KeyEvent.KEYCODE_B,
                                KeyEvent.KEYCODE_C,
                                KeyEvent.KEYCODE_D,
                                KeyEvent.KEYCODE_E,
                                KeyEvent.KEYCODE_F,
                                KeyEvent.KEYCODE_G,
                                KeyEvent.KEYCODE_H,
                                KeyEvent.KEYCODE_I,
                                KeyEvent.KEYCODE_J,
                                KeyEvent.KEYCODE_K,
                                KeyEvent.KEYCODE_L,
                                KeyEvent.KEYCODE_M,
                                KeyEvent.KEYCODE_N,
                                KeyEvent.KEYCODE_O,
                                KeyEvent.KEYCODE_P,
                                KeyEvent.KEYCODE_Q,
                                KeyEvent.KEYCODE_R,
                                KeyEvent.KEYCODE_S,
                                KeyEvent.KEYCODE_T,
                                KeyEvent.KEYCODE_U,
                                KeyEvent.KEYCODE_V,
                                KeyEvent.KEYCODE_W,
                                KeyEvent.KEYCODE_X,
                                KeyEvent.KEYCODE_Y,
                                KeyEvent.KEYCODE_Z,
                                KeyEvent.KEYCODE_SPACE))
                        var i = 0
                        while (5 > i) {
                            addInteraction(typeAlphabet)
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

    private val runFilename: String
        private get() = javaClass.simpleName +
                System.currentTimeMillis()
}
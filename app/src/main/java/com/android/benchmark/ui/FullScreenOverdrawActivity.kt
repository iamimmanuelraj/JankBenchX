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

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import com.android.benchmark.R
import com.android.benchmark.registry.BenchmarkRegistry
import com.android.benchmark.ui.automation.Automator
import com.android.benchmark.ui.automation.Automator.AutomateCallback
import com.android.benchmark.ui.automation.Interaction

class FullScreenOverdrawActivity : AppCompatActivity() {
    private var mAutomator: Automator? = null

    private inner class OverdrawView(context: Context?) : View(context) {
        var paint = Paint()
        var mColorValue = 0
        @Keep
        fun setColorValue(colorValue: Int) {
            mColorValue = colorValue
            invalidate()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val objectAnimator = ObjectAnimator.ofInt(this, "colorValue", 0, 255)
            objectAnimator.repeatMode = ValueAnimator.REVERSE
            objectAnimator.repeatCount = 100
            objectAnimator.start()
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            paint.color = Color.rgb(mColorValue, 255 - mColorValue, 255)
            var i = 0
            while (10 > i) {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                i++
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val overdrawView = OverdrawView(this)
        overdrawView.keepScreenOn = true
        setContentView(overdrawView)
        val runId = intent.getIntExtra("com.android.benchmark.RUN_ID", 0)
        val iteration = intent.getIntExtra("com.android.benchmark.ITERATION", -1)
        val name: String = BenchmarkRegistry.Companion.getBenchmarkName(this, R.id.benchmark_overdraw)
        mAutomator = Automator(name, runId, iteration, window,
                object : AutomateCallback() {
                    override fun onPostAutomate() {
                        val result = Intent()
                        setResult(RESULT_OK, result)
                        finish()
                    }

                    override fun onAutomate() {
                        val coordinates = IntArray(2)
                        overdrawView.getLocationOnScreen(coordinates)
                        val x = coordinates[0]
                        val y = coordinates[1]
                        val width = overdrawView.width.toFloat()
                        val height = overdrawView.height.toFloat()
                        val middleX = (x + width) / 5
                        val middleY = (y + height) / 5
                        addInteraction(Interaction.Companion.newTap(middleX, middleY))
                    }
                })
        mAutomator!!.start()
    }
}
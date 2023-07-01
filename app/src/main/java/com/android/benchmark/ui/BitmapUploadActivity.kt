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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.benchmark.R
import com.android.benchmark.registry.BenchmarkRegistry
import com.android.benchmark.ui.automation.Automator
import com.android.benchmark.ui.automation.Automator.AutomateCallback
import com.android.benchmark.ui.automation.Interaction

/**
 *
 */
class BitmapUploadActivity : AppCompatActivity() {
    private var mAutomator: Automator? = null

    class UploadView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
        private var mColorValue = 0
        private var mBitmap: Bitmap? = null
        private val mMetrics = DisplayMetrics()
        private val mRect = Rect()
        fun setColorValue(colorValue: Int) {
            if (colorValue == mColorValue) return
            mColorValue = colorValue

            // modify the bitmap's color to ensure it's uploaded to the GPU
            mBitmap!!.eraseColor(Color.rgb(mColorValue, 255 - mColorValue, 255))
            invalidate()
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            display.getMetrics(mMetrics)
            val minDisplayDimen = Math.min(mMetrics.widthPixels, mMetrics.heightPixels)
            val bitmapSize = Math.min((minDisplayDimen * 0.75).toInt(), 720)
            if (null == mBitmap || mBitmap!!.width != bitmapSize || mBitmap!!.height != bitmapSize) {
                mBitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (null != mBitmap) {
                mRect[0, 0, width] = height
                canvas.drawBitmap(mBitmap!!, null, mRect, null)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // animate color to force bitmap uploads
            return super.onTouchEvent(event)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitmap_upload)
        val uploadRoot = findViewById<View>(R.id.upload_root)
        uploadRoot.keepScreenOn = true
        uploadRoot.setOnTouchListener { view, motionEvent ->
            val uploadView = findViewById<UploadView>(R.id.upload_view)
            val colorValueAnimator = ObjectAnimator.ofInt(uploadView, "colorValue", 0, 255)
            colorValueAnimator.repeatMode = ValueAnimator.REVERSE
            colorValueAnimator.repeatCount = 100
            colorValueAnimator.start()

            // animate scene root to guarantee there's a minimum amount of GPU rendering work
            val yAnimator = ObjectAnimator.ofFloat(
                    view, "translationY", 0f, 100f)
            yAnimator.repeatMode = ValueAnimator.REVERSE
            yAnimator.repeatCount = 100
            yAnimator.start()
            true
        }
        val uploadView = findViewById<UploadView>(R.id.upload_view)
        val runId = intent.getIntExtra("com.android.benchmark.RUN_ID", 0)
        val iteration = intent.getIntExtra("com.android.benchmark.ITERATION", -1)
        val name: String = BenchmarkRegistry.Companion.getBenchmarkName(this, R.id.benchmark_bitmap_upload)
        mAutomator = Automator(name, runId, iteration, window,
                object : AutomateCallback() {
                    override fun onPostAutomate() {
                        val result = Intent()
                        setResult(RESULT_OK, result)
                        finish()
                    }

                    override fun onAutomate() {
                        val coordinates = IntArray(2)
                        uploadRoot.getLocationOnScreen(coordinates)
                        val x = coordinates[0]
                        val y = coordinates[1]
                        val width = uploadRoot.width.toFloat()
                        val height = uploadRoot.height.toFloat()
                        val middleX = (x + width) / 5
                        val middleY = (y + height) / 5
                        addInteraction(Interaction.Companion.newTap(middleX, middleY))
                    }
                })
        mAutomator!!.start()
    }
}
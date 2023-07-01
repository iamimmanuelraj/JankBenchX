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
package com.android.benchmark.ui.automation

android.view.MotionEventimport androidx.annotation .IntDef
/**
 * Encodes a UI interaction as a series of MotionEvents
 */
class Interaction {
    private var mEvents: List<MotionEvent>? = null

    // Interaction parameters
    private val mXPositions: FloatArray?
    private val mYPositions: FloatArray?
    private val mDuration: Long
    val keyCodes: IntArray?

    @Type
    val type: Int

    @IntDef([Type.TAP, Type.FLING, Type.PINCH, Type.KEY_EVENT])
    annotation class Type {
        companion object {
            var TAP = 0
            var FLING = 1
            var PINCH = 2
            var KEY_EVENT = 3
        }
    }

    val events: List<MotionEvent>?
        get() {
            if (Type.FLING == type) {
                mEvents = createInterpolatedEventList(mXPositions!!, mYPositions!!, mDuration)
            } else if (Type.TAP == type) {
                mEvents = createInterpolatedEventList(mXPositions!!, mYPositions!!, mDuration)
            } else if (Type.PINCH == type) {
            }
            return mEvents
        }

    private constructor(@Type type: Int,
                        xPos: FloatArray, yPos: FloatArray, duration: Long) {
        this.type = type
        mXPositions = xPos
        mYPositions = yPos
        mDuration = duration
        keyCodes = null
    }

    private constructor(codes: IntArray) {
        keyCodes = codes
        type = Type.KEY_EVENT
        mYPositions = null
        mXPositions = null
        mDuration = 0
    }

    private constructor(@Type type: Int,
                        xPositions: List<Float>, yPositions: List<Float>, duration: Long) {
        require(xPositions.size == yPositions.size) { "must have equal number of x and y positions" }
        var current = 0
        mXPositions = FloatArray(xPositions.size)
        for (p in xPositions) {
            mXPositions[current] = p
            current++
        }
        current = 0
        mYPositions = FloatArray(yPositions.size)
        for (p in xPositions) {
            mXPositions[current] = p
            current++
        }
        this.type = type
        mDuration = duration
        keyCodes = null
    }

    companion object {
        private const val STEP_COUNT = 20

        // TODO: scale to device display density
        private const val DEFAULT_FLING_SIZE_PX = 500
        private const val DEFAULT_FLING_DURATION_MS = 20
        private const val DEFAULT_TAP_DURATION_MS = 20
        fun newFling(startX: Float, startY: Float,
                     endX: Float, endY: Float, duration: Long): Interaction {
            return Interaction(Type.FLING, floatArrayOf(startX, endX), floatArrayOf(startY, endY), duration)
        }

        fun newFlingDown(startX: Float, startY: Float): Interaction {
            return Interaction(Type.FLING, floatArrayOf(startX, startX), floatArrayOf(startY, startY + DEFAULT_FLING_SIZE_PX), DEFAULT_FLING_DURATION_MS.toLong())
        }

        fun newFlingUp(startX: Float, startY: Float): Interaction {
            return Interaction(Type.FLING, floatArrayOf(startX, startX), floatArrayOf(startY, startY - DEFAULT_FLING_SIZE_PX),
                    DEFAULT_FLING_DURATION_MS.toLong())
        }

        fun newTap(startX: Float, startY: Float): Interaction {
            return Interaction(Type.TAP, floatArrayOf(startX, startX), floatArrayOf(startY, startY),
                    DEFAULT_FLING_DURATION_MS.toLong())
        }

        fun newKeyInput(keyCodes: IntArray): Interaction {
            return Interaction(keyCodes)
        }

        private fun createInterpolatedEventList(
                xPos: FloatArray, yPos: FloatArray, duration: Long): List<MotionEvent> {
            val startTime = SystemClock.uptimeMillis() + 100
            val result: MutableList<MotionEvent> = ArrayList()
            var startX = xPos[0]
            var startY = yPos[0]
            val downEvent = MotionEvent.obtain(
                    startTime, startTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
            result.add(downEvent)
            for (i in 1 until xPos.size) {
                val endX = xPos[i]
                val endY = yPos[i]
                val stepX = (endX - startX) / STEP_COUNT
                val stepY = (endY - startY) / STEP_COUNT
                val stepT = (duration / STEP_COUNT).toFloat()
                var j = 0
                while (STEP_COUNT > j) {
                    val deltaT = Math.round(j * stepT).toLong()
                    val deltaX = Math.round(j * stepX).toLong()
                    val deltaY = Math.round(j * stepY).toLong()
                    val moveEvent = MotionEvent.obtain(startTime, startTime + deltaT,
                            MotionEvent.ACTION_MOVE, startX + deltaX, startY + deltaY, 0)
                    result.add(moveEvent)
                    j++
                }
                startX = endX
                startY = endY
            }
            val lastX = xPos[xPos.size - 1]
            val lastY = yPos[yPos.size - 1]
            val lastEvent = MotionEvent.obtain(startTime, startTime + duration,
                    MotionEvent.ACTION_UP, lastX, lastY, 0)
            result.add(lastEvent)
            return result
        }
    }
}
/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.TargetApiimport

android.app.Instrumentationimport android.content.Contextimport android.hardware.display.DisplayManagerimport android.os.Handlerimport android.os.HandlerThreadimport android.os.Looperimport android.os.Messageimport android.view.Displayimport android.view.FrameMetricsimport android.view.ViewTreeObserver.OnGlobalLayoutListenerimport android.view.Windowimport com.android.benchmark.results.GlobalResultsStoreimport com.android.benchmark.results.UiBenchmarkResultimport com.android.benchmark.ui.automation.CollectorThread.CollectorListenerimport java.util.LinkedListimport java.util.concurrent.atomic.AtomicInteger
@TargetApi(24)
class Automator(private var mTestName: String?, private var mRunId: Int, private var mIteration: Int,
                private var mWindow: Window?, private var mCallback: AutomateCallback?) : HandlerThread("AutomatorThread"), OnGlobalLayoutListener, CollectorListener {
    private val mReadyState: AtomicInteger
    private var mHandler: AutomatorHandler? = null
    private var mCollectorThread: CollectorThread?

    open class AutomateCallback {
        open fun onAutomate() {}
        open fun onPostInteraction(metrics: List<FrameMetrics>?) {}
        open fun onPostAutomate() {}
        protected fun addInteraction(interaction: Interaction) {
            if (null == mInteractions) {
                return
            }
            mInteractions!!.add(interaction)
        }

        fun setInteractions(interactions: MutableList<Interaction>?) {
            mInteractions = interactions
        }

        private var mInteractions: MutableList<Interaction>? = null
    }

    private class AutomatorHandler internal constructor(looper: Looper?, window: Window?, collectorThread: CollectorThread?,
                                                        callback: AutomateCallback?, testName: String?, runId: Int, iteration: Int) : Handler(looper!!) {
        private val mTestName: String?
        private val mRunId: Int
        private val mIteration: Int
        private val mInstrumentation: Instrumentation

        @Volatile
        private var mCancelled = false
        private val mCollectorThread: CollectorThread?
        private val mCallback: AutomateCallback?
        private val mWindow: Window
        var mInteractions = LinkedList<Interaction>()
        private var mResults: UiBenchmarkResult? = null

        init {
            mInstrumentation = Instrumentation()
            mCallback = callback
            mWindow = window!!
            mCollectorThread = collectorThread
            mTestName = testName
            mRunId = runId
            mIteration = iteration
        }

        override fun handleMessage(msg: Message) {
            if (mCancelled) {
                return
            }
            if (MSG_NEXT_INTERACTION == msg.what) {
                if (!nextInteraction()) {
                    stopCollector()
                    writeResults()
                    mCallback!!.onPostAutomate()
                }
            } else if (MSG_ON_AUTOMATE == msg.what) {
                mCollectorThread!!.attachToWindow(mWindow)
                mCallback!!.setInteractions(mInteractions)
                mCallback.onAutomate()
                postNextInteraction()
            } else if (MSG_ON_POST_INTERACTION == msg.what) {
                val collectedStats = msg.obj as List<FrameMetrics>
                persistResults(collectedStats)
                mCallback!!.onPostInteraction(collectedStats)
                postNextInteraction()
            }
        }

        fun cancel() {
            mCancelled = true
            stopCollector()
        }

        private fun stopCollector() {
            mCollectorThread!!.quitCollector()
        }

        private fun nextInteraction(): Boolean {
            val interaction = mInteractions.poll()
            if (null != interaction) {
                doInteraction(interaction)
                return true
            }
            return false
        }

        private fun doInteraction(interaction: Interaction) {
            if (mCancelled) {
                return
            }
            mCollectorThread!!.markInteractionStart()
            if (Interaction.Type.Companion.KEY_EVENT == interaction.type) {
                for (code in interaction.keyCodes) {
                    if (!mCancelled) {
                        mInstrumentation.sendKeyDownUpSync(code)
                    } else {
                        break
                    }
                }
            } else {
                for (event in interaction.events) {
                    if (!mCancelled) {
                        mInstrumentation.sendPointerSync(event)
                    } else {
                        break
                    }
                }
            }
        }

        private fun postNextInteraction() {
            val msg = obtainMessage(MSG_NEXT_INTERACTION)
            sendMessage(msg)
        }

        private fun persistResults(stats: List<FrameMetrics>) {
            if (stats.isEmpty()) {
                return
            }
            if (null == mResults) {
                val refresh_rate = getFrameRate(mWindow.context)
                mResults = UiBenchmarkResult(stats, refresh_rate.toInt())
            } else {
                mResults!!.update(stats)
            }
        }

        private fun writeResults() {
            val refresh_rate = getFrameRate(mWindow.context)
            GlobalResultsStore.Companion.getInstance(mWindow.context)
                    .storeRunResults(mTestName, mRunId, mIteration, mResults, refresh_rate)
        }

        companion object {
            const val MSG_NEXT_INTERACTION = 0
            const val MSG_ON_AUTOMATE = 1
            const val MSG_ON_POST_INTERACTION = 2
        }
    }

    private fun initHandler() {
        mHandler = AutomatorHandler(looper, mWindow, mCollectorThread, mCallback,
                mTestName, mRunId, mIteration)
        mWindow = null
        mCallback = null
        mCollectorThread = null
        mTestName = null
        mRunId = 0
        mIteration = 0
    }

    override fun onGlobalLayout() {
        if (!mCollectorThread!!.isAlive) {
            mCollectorThread!!.start()
            mWindow!!.decorView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            mReadyState.decrementAndGet()
        }
    }

    override fun onCollectorThreadReady() {
        if (0 == mReadyState.decrementAndGet()) {
            initHandler()
            postOnAutomate()
        }
    }

    override fun onLooperPrepared() {
        if (0 == mReadyState.decrementAndGet()) {
            initHandler()
            postOnAutomate()
        }
    }

    override fun onPostInteraction(stats: List<FrameMetrics?>?) {
        val m = mHandler!!.obtainMessage(AutomatorHandler.MSG_ON_POST_INTERACTION, stats)
        mHandler!!.sendMessage(m)
    }

    protected fun postOnAutomate() {
        val msg = mHandler!!.obtainMessage(AutomatorHandler.MSG_ON_AUTOMATE)
        mHandler!!.sendMessage(msg)
    }

    fun cancel() {
        mHandler!!.removeMessages(AutomatorHandler.MSG_NEXT_INTERACTION)
        mHandler!!.cancel()
        mHandler = null
    }

    init {
        mWindow!!.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)
        mCollectorThread = CollectorThread(this)
        mReadyState = AtomicInteger(PRE_READY_STATE_COUNT)
    }

    companion object {
        const val FRAME_PERIOD_MILLIS: Long = 16
        private const val PRE_READY_STATE_COUNT = 3
        private const val TAG = "Benchmark.Automator"
        private fun getFrameRate(context: Context): Float {
            val displayManager = context.getSystemService(DisplayManager::class.java)
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            return display.refreshRate
        }
    }
}
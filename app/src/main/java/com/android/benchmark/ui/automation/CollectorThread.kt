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
package com.android.benchmark.ui.automation

android.os.Handlerimport android.os.HandlerThreadimport android.os.Messageimport android.os.SystemClockimport android.view.FrameMetricsimport android.view.Windowimport android.view.Window.OnFrameMetricsAvailableListenerimport java.lang.ref.WeakReferenceimport java.util.LinkedList
/**
 *
 */
internal class CollectorThread(listener: CollectorListener) : HandlerThread("FrameStatsCollectorThread") {
    private var mCollector: FrameStatsCollector? = null
    private var mAttachedWindow: Window? = null
    private val mFrameTimingStats: MutableList<FrameMetrics?>
    private var mLastFrameTime: Long = 0
    private var mWatchdog: WatchdogHandler? = null
    private val mListener: WeakReference<CollectorListener>

    @Volatile
    private var mCollecting = false

    internal interface CollectorListener {
        fun onCollectorThreadReady()
        fun onPostInteraction(stats: List<FrameMetrics?>?)
    }

    private inner class WatchdogHandler : Handler() {
        override fun handleMessage(msg: Message) {
            if (!mCollecting) {
                return
            }
            val currentTime = SystemClock.uptimeMillis()
            if (mLastFrameTime + Companion.SCHEDULE_INTERVAL_MILLIS <= currentTime) {
                // haven't seen a frame in a while, interaction is probably done
                mCollecting = false
                val listener = mListener.get()
                listener?.onPostInteraction(mFrameTimingStats)
            } else {
                schedule()
            }
        }

        fun schedule() {
            sendMessageDelayed(obtainMessage(Companion.MSG_SCHEDULE), Companion.SCHEDULE_INTERVAL_MILLIS)
        }

        fun deschedule() {
            removeMessages(Companion.MSG_SCHEDULE)
        }

        companion object {
            private val SCHEDULE_INTERVAL_MILLIS: Long = 20 * Automator.Companion.FRAME_PERIOD_MILLIS
            private const val MSG_SCHEDULE = 0
        }
    }

    @TargetApi(24)
    private inner class FrameStatsCollector : OnFrameMetricsAvailableListener {
        override fun onFrameMetricsAvailable(window: Window, frameMetrics: FrameMetrics, dropCount: Int) {
            if (!mCollecting) {
                return
            }
            mFrameTimingStats.add(FrameMetrics(frameMetrics))
            mLastFrameTime = SystemClock.uptimeMillis()
        }
    }

    init {
        mFrameTimingStats = LinkedList()
        mListener = WeakReference(listener)
    }

    @TargetApi(24)
    fun attachToWindow(window: Window) {
        if (null != mAttachedWindow) {
            mAttachedWindow!!.removeOnFrameMetricsAvailableListener(mCollector)
        }
        mAttachedWindow = window
        window.addOnFrameMetricsAvailableListener(mCollector!!, Handler(looper))
    }

    @TargetApi(24)
    @Synchronized
    fun detachFromWindow() {
        if (null != mAttachedWindow) {
            mAttachedWindow!!.removeOnFrameMetricsAvailableListener(mCollector)
        }
        mAttachedWindow = null
    }

    @TargetApi(24)
    override fun onLooperPrepared() {
        super.onLooperPrepared()
        mCollector = FrameStatsCollector()
        mWatchdog = WatchdogHandler()
        val listener = mListener.get()
        listener?.onCollectorThreadReady()
    }

    fun quitCollector(): Boolean {
        stopCollecting()
        detachFromWindow()
        println("Jank Percentage: " + 100 * janks / total.toDouble() + "%")
        tripleBuffered = false
        total = 0
        janks = 0
        return quit()
    }

    fun stopCollecting() {
        if (!mCollecting) {
            return
        }
        mCollecting = false
        mWatchdog!!.deschedule()
    }

    fun markInteractionStart() {
        mLastFrameTime = 0
        mFrameTimingStats.clear()
        mCollecting = true
        mWatchdog!!.schedule()
    }

    companion object {
        var tripleBuffered = false
        var janks = 0
        var total = 0
    }
}
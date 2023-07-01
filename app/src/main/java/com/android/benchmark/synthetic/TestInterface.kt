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
package com.android.benchmark.syntheticimport

import android.view.View
import android.widget.TextView
import com.android.benchmark.synthetic.TestInterface
import com.android.benchmark.synthetic.TestInterface.LooperThread
import com.android.benchmark.synthetic.TestInterface.TestResultCallback
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import java.util.LinkedList
import java.util.Queue

android.annotation .TargetApi
import com.android.benchmark.ui.automation.Automator.AutomateCallback
import android.os.HandlerThread
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.android.benchmark.ui.automation.CollectorThread.CollectorListener
import com.android.benchmark.ui.automation.Automator.AutomatorHandler
import com.android.benchmark.ui.automation.CollectorThread
import android.view.FrameMetrics
import com.android.benchmark.ui.automation.Interaction
import android.os.Looper
import kotlin.jvm.Volatile
import com.android.benchmark.results.UiBenchmarkResult
import android.view.MotionEvent
import com.android.benchmark.ui.automation.Automator
import com.android.benchmark.results.GlobalResultsStore
import android.hardware.display.DisplayManager
import androidx.annotation.IntDef
import com.android.benchmark.ui.automation.CollectorThread.FrameStatsCollector
import com.android.benchmark.ui.automation.CollectorThread.WatchdogHandler
import android.view.Window.OnFrameMetricsAvailableListener
import kotlin.jvm.Synchronized
import kotlin.Throws
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.benchmark.R
import com.android.benchmark.ui.ShadowGridActivity.MyListFragment
import android.widget.ArrayAdapter
import android.content.Intent
import android.app.Activity
import com.android.benchmark.ui.ListActivityBase
import com.android.benchmark.ui.TextScrollActivity
import com.android.benchmark.registry.BenchmarkRegistry
import android.util.DisplayMetrics
import android.view.View.OnTouchListener
import com.android.benchmark.ui.BitmapUploadActivity.UploadView
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.widget.EditText
import android.widget.FrameLayout
import com.android.benchmark.ui.ListViewScrollActivity
import androidx.annotation.Keep
import com.android.benchmark.ui.FullScreenOverdrawActivity.OverdrawView
import com.android.benchmark.ui.ImageListViewScrollActivity.BitmapWorkerTask
import com.android.benchmark.ui.ImageListViewScrollActivity.ImageListAdapter
import android.os.AsyncTask
import com.android.benchmark.ui.ImageListViewScrollActivity
import android.widget.BaseAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import com.android.benchmark.api.JankBenchAPI
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.android.benchmark.api.JankBenchService
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.topjohnwu.superuser.Shell
import retrofit2.http.POST
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.ExpandableListView
import com.android.benchmark.app.BenchmarkListAdapter
import android.widget.Toast
import android.text.TextPaint
import android.content.res.TypedArray
import com.android.benchmark.app.UiResultsFragment
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import android.widget.SimpleAdapter
import android.widget.BaseExpandableListAdapter
import com.android.benchmark.registry.BenchmarkGroup
import android.graphics.Typeface
import com.android.benchmark.registry.BenchmarkGroup.Benchmark
import android.widget.CheckBox
import com.android.benchmark.app.RunLocalBenchmarksActivity.LocalBenchmark
import com.android.benchmark.app.RunLocalBenchmarksActivity.LocalBenchmarksList
import com.android.benchmark.app.RunLocalBenchmarksActivity.LocalBenchmarksListAdapter
import com.android.benchmark.app.RunLocalBenchmarksActivity
import com.android.benchmark.ui.ShadowGridActivity
import com.android.benchmark.ui.EditTextInputActivity
import com.android.benchmark.ui.FullScreenOverdrawActivity
import com.android.benchmark.ui.BitmapUploadActivity
import com.android.benchmark.synthetic.MemoryActivity
import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.content.ComponentName
import com.android.benchmark.registry.BenchmarkCategory
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParser
import android.util.SparseArray
import android.util.Xml
import com.android.benchmark.synthetic.TestInterface.TestResultCallback
import com.android.benchmark.synthetic.TestInterface.LooperThread
import com.android.benchmark.synthetic.TestInterface
import com.android.benchmark.app.PerfTimeline
import com.android.benchmark.synthetic.MemoryActivity.SyntheticTestCallback
import android.view.WindowManager

class TestInterface internal constructor(v: View, runtimeSeconds: Int, callback: TestResultCallback) {
    external fun nInit(options: Long): Long
    external fun nDestroy(b: Long): Long
    external fun nGetData(b: Long, data: FloatArray?): Float
    external fun nRunPowerManagementTest(b: Long, options: Long): Boolean
    external fun nRunCPUHeatSoakTest(b: Long, options: Long): Boolean
    external fun nMemTestStart(b: Long): Boolean
    external fun nMemTestBandwidth(b: Long, size: Long): Float
    external fun nMemTestLatency(b: Long, size: Long): Float
    external fun nMemTestEnd(b: Long)
    external fun nGFlopsTest(b: Long, opt: Long): Float
    open class TestResultCallback {
        open fun onTestResult(command: Int, result: Float) {}
    }

    var mLinesLow: FloatArray
    var mLinesHigh: FloatArray
    var mLinesValue: FloatArray
    var mTextStatus: TextView? = null
    var mTextMin: TextView? = null
    var mTextMax: TextView? = null
    var mTextTypical: TextView? = null
    private val mViewToUpdate: View
    private val mLT: LooperThread

    init {
        val buckets = runtimeSeconds * 1000
        mLinesLow = FloatArray(buckets * 4)
        mLinesHigh = FloatArray(buckets * 4)
        mLinesValue = FloatArray(buckets * 4)
        mViewToUpdate = v
        mLT = LooperThread(this, callback)
        mLT.start()
    }

    internal class LooperThread(private val mTI: TestInterface, private val mCallback: TestResultCallback) : Thread("BenchmarkTestThread") {
        @Volatile
        private var mRun = true
        var mCommandQueue: Queue<Int> = LinkedList()
        fun runCommand(command: Int) {
            val i = Integer.valueOf(command)
            synchronized(this) {
                mCommandQueue.add(i)
                notifyAll()
            }
        }

        override fun run() {
            val b = mTI.nInit(0)
            if (0L == b) {
                return
            }
            while (mRun) {
                var command = 0
                synchronized(this) {
                    if (mCommandQueue.isEmpty()) {
                        try {
                            wait()
                        } catch (e: InterruptedException) {
                        }
                    }
                    if (!mCommandQueue.isEmpty()) {
                        command = mCommandQueue.remove()
                    }
                }
                if (CommandExit == command) {
                    mRun = false
                } else if (TestPowerManagement == command) {
                    val score = mTI.testPowerManagement(b)
                    mCallback.onTestResult(TestPowerManagement, 0f)
                } else if (TestMemoryBandwidth == command) {
                    mTI.testCPUMemoryBandwidth(b)
                } else if (TestMemoryLatency == command) {
                    mTI.testCPUMemoryLatency(b)
                } else if (TestHeatSoak == command) {
                    mTI.testCPUHeatSoak(b)
                } else if (TestGFlops == command) {
                    mTI.testCPUGFlops(b)
                }

                //mViewToUpdate.post(new Runnable() {
                //  public void run() {
                //     mViewToUpdate.invalidate();
                //}
                //});
            }
            mTI.nDestroy(b)
        }

        fun exit() {
            mRun = false
        }

        companion object {
            const val CommandExit = 1
            const val TestPowerManagement = 2
            const val TestMemoryBandwidth = 3
            const val TestMemoryLatency = 4
            const val TestHeatSoak = 5
            const val TestGFlops = 6
        }
    }

    fun postTextToView(v: TextView, s: String) {
        v.post { v.text = s }
    }

    fun calcAverage(data: FloatArray): Float {
        var total = 0.0f
        for (ct in data.indices) {
            total += data[ct]
        }
        return total / data.size
    }

    fun makeGraph(data: FloatArray, lines: FloatArray) {
        for (ct in data.indices) {
            lines[ct * 4] = ct.toFloat()
            lines[ct * 4 + 1] = 500.0f - data[ct]
            lines[ct * 4 + 2] = ct.toFloat()
            lines[ct * 4 + 3] = 500.0f
        }
    }

    fun testPowerManagement(b: Long): Float {
        val dat = FloatArray(mLinesLow.size / 4)
        postTextToView(mTextStatus!!, "Running single-threaded")
        nRunPowerManagementTest(b, 1)
        nGetData(b, dat)
        makeGraph(dat, mLinesLow)
        mViewToUpdate.postInvalidate()
        val avgMin = calcAverage(dat)
        postTextToView(mTextMin!!, "Single threaded $avgMin per second")
        postTextToView(mTextStatus!!, "Running multi-threaded")
        nRunPowerManagementTest(b, 4)
        nGetData(b, dat)
        makeGraph(dat, mLinesHigh)
        mViewToUpdate.postInvalidate()
        val avgMax = calcAverage(dat)
        postTextToView(mTextMax!!, "Multi threaded $avgMax per second")
        postTextToView(mTextStatus!!, "Running typical")
        nRunPowerManagementTest(b, 0)
        nGetData(b, dat)
        makeGraph(dat, mLinesValue)
        mViewToUpdate.postInvalidate()
        val avgTypical = calcAverage(dat)
        val ofIdeal = avgTypical / (avgMax + avgMin) * 200.0f
        postTextToView(mTextTypical!!, String.format("Typical mix (50/50) %%%2.0f of ideal", ofIdeal))
        return ofIdeal * (avgMax + avgMin)
    }

    fun testCPUHeatSoak(b: Long): Float {
        val dat = FloatArray(1000)
        postTextToView(mTextStatus!!, "Running heat soak test")
        run {
            var t = 0
            while (1000 > t) {
                mLinesLow[t * 4] = t.toFloat()
                mLinesLow[t * 4 + 1] = 498.0f
                mLinesLow[t * 4 + 2] = t.toFloat()
                mLinesLow[t * 4 + 3] = 500.0f
                t++
            }
        }
        var peak = 0.0f
        var total = 0.0f
        var dThroughput = 0f
        var prev = 0f
        val stats = SummaryStatistics()
        var t = 0
        while (1000 > t) {
            nRunCPUHeatSoakTest(b, 1)
            nGetData(b, dat)
            val p = calcAverage(dat)
            if (0f != prev) {
                dThroughput += prev - p
            }
            prev = p
            mLinesLow[t * 4 + 1] = 499.0f - p
            if (peak < p) {
                peak = p
            }
            for (f in dat) {
                stats.addValue(f.toDouble())
            }
            total += p
            mViewToUpdate.postInvalidate()
            postTextToView(mTextMin!!, "Peak $peak per second")
            postTextToView(mTextMax!!, "Current $p per second")
            postTextToView(mTextTypical!!, "Average $total" / (t + 1) + " per second")
            t++
        }
        val decreaseOverTime = dThroughput / 1000
        println("dthroughput/dt: $decreaseOverTime")
        val score = (stats.mean / (stats.standardDeviation * decreaseOverTime)).toFloat()
        postTextToView(mTextStatus!!, "Score: $score")
        return score
    }

    fun testCPUMemoryBandwidth(b: Long) {
        val sizeK = intArrayOf(1, 2, 3, 4, 5, 6, 7,
                8, 10, 12, 14, 16, 20, 24, 28,
                32, 40, 48, 56, 64, 80, 96, 112,
                128, 160, 192, 224, 256, 320, 384, 448,
                512, 640, 768, 896, 1024, 1280, 1536, 1792,
                2048, 2560, 3584, 4096, 5120, 6144, 7168,
                8192, 10240, 12288, 14336, 16384
        )
        val subSteps = 15
        val results = FloatArray(sizeK.size * subSteps)
        nMemTestStart(b)
        val dat = FloatArray(1000)
        postTextToView(mTextStatus!!, "Running Memory Bandwidth test")
        var t = 0
        while (1000 > t) {
            mLinesLow[t * 4] = t.toFloat()
            mLinesLow[t * 4 + 1] = 498.0f
            mLinesLow[t * 4 + 2] = t.toFloat()
            mLinesLow[t * 4 + 3] = 500.0f
            t++
        }
        for (i in sizeK.indices) {
            postTextToView(mTextStatus!!, "Running " + sizeK[i] + " K")
            var rtot = 0.0f
            var j = 0
            while (subSteps > j) {
                val ret = nMemTestBandwidth(b, (sizeK[i] * 1024).toLong())
                rtot += ret
                results[i * subSteps + j] = ret
                mLinesLow[(i * subSteps + j) * 4 + 1] = 499.0f - results[i * 15 + j] * 20.0f
                mViewToUpdate.postInvalidate()
                j++
            }
            rtot /= subSteps.toFloat()
            if (2 == sizeK[i]) {
                postTextToView(mTextMin!!, "2K $rtot GB/s")
            }
            if (128 == sizeK[i]) {
                postTextToView(mTextMax!!, "128K $rtot GB/s")
            }
            if (8192 == sizeK[i]) {
                postTextToView(mTextTypical!!, "8M $rtot GB/s")
            }
        }
        nMemTestEnd(b)
        postTextToView(mTextStatus!!, "Done")
    }

    fun testCPUMemoryLatency(b: Long) {
        val sizeK = intArrayOf(1, 2, 3, 4, 5, 6, 7,
                8, 10, 12, 14, 16, 20, 24, 28,
                32, 40, 48, 56, 64, 80, 96, 112,
                128, 160, 192, 224, 256, 320, 384, 448,
                512, 640, 768, 896, 1024, 1280, 1536, 1792,
                2048, 2560, 3584, 4096, 5120, 6144, 7168,
                8192, 10240, 12288, 14336, 16384
        )
        val subSteps = 15
        val results = FloatArray(sizeK.size * subSteps)
        nMemTestStart(b)
        val dat = FloatArray(1000)
        postTextToView(mTextStatus!!, "Running Memory Latency test")
        var t = 0
        while (1000 > t) {
            mLinesLow[t * 4] = t.toFloat()
            mLinesLow[t * 4 + 1] = 498.0f
            mLinesLow[t * 4 + 2] = t.toFloat()
            mLinesLow[t * 4 + 3] = 500.0f
            t++
        }
        for (i in sizeK.indices) {
            postTextToView(mTextStatus!!, "Running " + sizeK[i] + " K")
            var rtot = 0.0f
            var j = 0
            while (subSteps > j) {
                var ret = nMemTestLatency(b, (sizeK[i] * 1024).toLong())
                rtot += ret
                results[i * subSteps + j] = ret
                if (400.0f < ret) ret = 400.0f
                if (0.0f > ret) ret = 0.0f
                mLinesLow[(i * subSteps + j) * 4 + 1] = 499.0f - ret
                //android.util.Log.e("bench", "test bw " + sizeK[i] + " - " + ret);
                mViewToUpdate.postInvalidate()
                j++
            }
            rtot /= subSteps.toFloat()
            if (2 == sizeK[i]) {
                postTextToView(mTextMin!!, "2K $rtot ns")
            }
            if (128 == sizeK[i]) {
                postTextToView(mTextMax!!, "128K $rtot ns")
            }
            if (8192 == sizeK[i]) {
                postTextToView(mTextTypical!!, "8M $rtot ns")
            }
        }
        nMemTestEnd(b)
        postTextToView(mTextStatus!!, "Done")
    }

    fun testCPUGFlops(b: Long) {
        val sizeK = intArrayOf(1, 2, 3, 4, 5, 6, 7
        )
        val subSteps = 15
        val results = FloatArray(sizeK.size * subSteps)
        nMemTestStart(b)
        val dat = FloatArray(1000)
        postTextToView(mTextStatus!!, "Running Memory Latency test")
        var t = 0
        while (1000 > t) {
            mLinesLow[t * 4] = t.toFloat()
            mLinesLow[t * 4 + 1] = 498.0f
            mLinesLow[t * 4 + 2] = t.toFloat()
            mLinesLow[t * 4 + 3] = 500.0f
            t++
        }
        for (i in sizeK.indices) {
            postTextToView(mTextStatus!!, "Running " + sizeK[i] + " K")
            var rtot = 0.0f
            var j = 0
            while (subSteps > j) {
                var ret = nGFlopsTest(b, (sizeK[i] * 1024).toLong())
                rtot += ret
                results[i * subSteps + j] = ret
                if (400.0f < ret) ret = 400.0f
                if (0.0f > ret) ret = 0.0f
                mLinesLow[(i * subSteps + j) * 4 + 1] = 499.0f - ret
                mViewToUpdate.postInvalidate()
                j++
            }
            rtot /= subSteps.toFloat()
            if (2 == sizeK[i]) {
                postTextToView(mTextMin!!, "2K $rtot ns")
            }
            if (128 == sizeK[i]) {
                postTextToView(mTextMax!!, "128K $rtot ns")
            }
            if (8192 == sizeK[i]) {
                postTextToView(mTextTypical!!, "8M $rtot ns")
            }
        }
        nMemTestEnd(b)
        postTextToView(mTextStatus!!, "Done")
    }

    fun runPowerManagement() {
        mLT.runCommand(LooperThread.TestPowerManagement)
    }

    fun runMemoryBandwidth() {
        mLT.runCommand(LooperThread.TestMemoryBandwidth)
    }

    fun runMemoryLatency() {
        mLT.runCommand(LooperThread.TestMemoryLatency)
    }

    fun runCPUHeatSoak() {
        mLT.runCommand(LooperThread.TestHeatSoak)
    }

    fun runCPUGFlops() {
        mLT.runCommand(LooperThread.TestGFlops)
    }

    companion object {
        init {
            System.loadLibrary("nativebench")
        }
    }
}
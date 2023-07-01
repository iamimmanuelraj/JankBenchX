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
package com.android.benchmark.resultsimport

import android.view.FrameMetrics
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.SummaryStatistics

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

/**
 * Utility for storing and analyzing UI benchmark results.
 */
@TargetApi(24)
class UiBenchmarkResult {
    private val BASE_SCORE = 100
    private val CONSISTENCY_BONUS_MAX = 100
    private var FRAME_PERIOD_MS = 16
    private var ZERO_SCORE_TOTAL_DURATION_MS = 2 * FRAME_PERIOD_MS
    private var JANK_PENALTY_THRESHOLD_MS = Math.floor(0.75 * FRAME_PERIOD_MS).toInt()
    private var ZERO_SCORE_ABOVE_THRESHOLD_MS = ZERO_SCORE_TOTAL_DURATION_MS - JANK_PENALTY_THRESHOLD_MS
    private var JANK_PENALTY_PER_MS_ABOVE_THRESHOLD = BASE_SCORE / ZERO_SCORE_ABOVE_THRESHOLD_MS.toDouble()
    private val mStoredStatistics: Array<DescriptiveStatistics?>

    constructor(instances: List<FrameMetrics>, refresh_rate: Int) {
        initializeThresholds(refresh_rate)
        mStoredStatistics = arrayOfNulls<DescriptiveStatistics>(METRICS.size)
        insertMetrics(instances)
    }

    constructor(values: DoubleArray, refresh_rate: Int) {
        initializeThresholds(refresh_rate)
        mStoredStatistics = arrayOfNulls<DescriptiveStatistics>(METRICS.size)
        insertValues(values)
    }

    // Dynamically set threshold values based on display refresh rate
    private fun initializeThresholds(refresh_rate: Int) {
        FRAME_PERIOD_MS = Math.floorDiv(1000, refresh_rate)
        ZERO_SCORE_TOTAL_DURATION_MS = FRAME_PERIOD_MS * 2
        JANK_PENALTY_THRESHOLD_MS = Math.floor(0.75 * FRAME_PERIOD_MS).toInt()
        ZERO_SCORE_ABOVE_THRESHOLD_MS = ZERO_SCORE_TOTAL_DURATION_MS - JANK_PENALTY_THRESHOLD_MS
        JANK_PENALTY_PER_MS_ABOVE_THRESHOLD = BASE_SCORE / ZERO_SCORE_ABOVE_THRESHOLD_MS.toDouble()
    }

    fun update(instances: List<FrameMetrics>) {
        insertMetrics(instances)
    }

    fun update(values: DoubleArray) {
        insertValues(values)
    }

    fun getAverage(id: Int): Double {
        val pos = getMetricPosition(id)
        return mStoredStatistics[pos]!!.mean
    }

    fun getMinimum(id: Int): Double {
        val pos = getMetricPosition(id)
        return mStoredStatistics[pos]!!.min
    }

    fun getMaximum(id: Int): Double {
        val pos = getMetricPosition(id)
        return mStoredStatistics[pos]!!.max
    }

    fun getMaximumIndex(id: Int): Int {
        val pos = getMetricPosition(id)
        val storedMetrics = mStoredStatistics[pos]!!.values
        var maxIdx = 0
        for (i in storedMetrics.indices) {
            if (storedMetrics[i] >= storedMetrics[maxIdx]) {
                maxIdx = i
            }
        }
        return maxIdx
    }

    fun getMetricAtIndex(index: Int, metricId: Int): Double {
        return mStoredStatistics[getMetricPosition(metricId)]!!.getElement(index)
    }

    fun getPercentile(id: Int, percentile: Int): Double {
        var percentile = percentile
        if (100 < percentile) percentile = 100
        if (0 > percentile) percentile = 0
        val metricPos = getMetricPosition(id)
        return mStoredStatistics[metricPos]!!.getPercentile(percentile.toDouble())
    }

    val totalFrameCount: Int
        get() = if (0 == mStoredStatistics.size) {
            0
        } else mStoredStatistics[0]!!.n.toInt()
    val score: Int
        get() {
            val badFramesStats = SummaryStatistics()
            val totalFrameCount = totalFrameCount
            for (i in 0 until totalFrameCount) {
                val totalDuration = getMetricAtIndex(i, FrameMetrics.TOTAL_DURATION)
                if (totalDuration >= JANK_PENALTY_THRESHOLD_MS) {
                    badFramesStats.addValue(totalDuration)
                }
            }
            val length = getSortedJankFrameIndices().size
            val jankFrameCount = 100 * length / totalFrameCount.toDouble()
            println("Mean: " + badFramesStats.mean + " JankP: " + jankFrameCount
                    + " StdDev: " + badFramesStats.standardDeviation +
                    " Count Bad: " + badFramesStats.n + " Count Jank: " + length)
            return Math.round(
                    badFramesStats.mean * jankFrameCount * badFramesStats.standardDeviation).toInt()
        }

    fun getNumJankFrames(): Int {
        return getSortedJankFrameIndices().size
    }

    fun getNumBadFrames(): Int {
        var num_bad_frames = 0
        val totalFrameCount = totalFrameCount
        for (i in 0 until totalFrameCount) {
            val totalDuration = getMetricAtIndex(i, FrameMetrics.TOTAL_DURATION)
            if (totalDuration >= JANK_PENALTY_THRESHOLD_MS) {
                num_bad_frames++
            }
        }
        return num_bad_frames
    }

    fun getJankPenalty(): Int {
        val total95th = mStoredStatistics[getMetricPosition(FrameMetrics.TOTAL_DURATION)]
                .getPercentile(95.0)
        println("95: $total95th")
        val aboveThreshold = total95th - JANK_PENALTY_THRESHOLD_MS
        if (0 >= aboveThreshold) {
            return 0
        }
        return if (aboveThreshold > ZERO_SCORE_ABOVE_THRESHOLD_MS) {
            BASE_SCORE
        } else Math.ceil(JANK_PENALTY_PER_MS_ABOVE_THRESHOLD * aboveThreshold).toInt()
    }

    fun getConsistencyBonus(): Int {
        val totalDurationStats = mStoredStatistics[getMetricPosition(FrameMetrics.TOTAL_DURATION)]
        val standardDeviation = totalDurationStats!!.standardDeviation
        if (0.0 == standardDeviation) {
            return CONSISTENCY_BONUS_MAX
        }

        // 1 / CV of the total duration.
        val bonus = totalDurationStats.mean / standardDeviation
        return Math.min(Math.round(bonus), CONSISTENCY_BONUS_MAX.toLong()).toInt()
    }

    fun getSortedJankFrameIndices(): IntArray {
        val jankFrameIndices = ArrayList<Int>()
        var tripleBuffered = false
        val totalFrameCount = totalFrameCount
        val totalDurationPos = getMetricPosition(FrameMetrics.TOTAL_DURATION)
        for (i in 0 until totalFrameCount) {
            val thisDuration = mStoredStatistics[totalDurationPos]!!.getElement(i)
            if (!tripleBuffered) {
                if (thisDuration > FRAME_PERIOD_MS) {
                    tripleBuffered = true
                    jankFrameIndices.add(i)
                }
            } else {
                if (thisDuration > 2 * FRAME_PERIOD_MS) {
                    tripleBuffered = false
                    jankFrameIndices.add(i)
                }
            }
        }
        val res = IntArray(jankFrameIndices.size)
        var i = 0
        for (index in jankFrameIndices) {
            res[i] = index
            i++
        }
        return res
    }

    private fun getMetricPosition(id: Int): Int {
        for (i in METRICS.indices) {
            if (id == METRICS.get(i)) {
                return i
            }
        }
        return -1
    }

    private fun insertMetrics(instances: List<FrameMetrics>) {
        for (frame in instances) {
            for (i in METRICS.indices) {
                var stats = mStoredStatistics[i]
                if (null == stats) {
                    stats = DescriptiveStatistics()
                    mStoredStatistics[i] = stats
                }
                mStoredStatistics[i]!!.addValue(frame.getMetric(METRICS.get(i)) / 1000000.0)
            }
        }
    }

    private fun insertValues(values: DoubleArray) {
        require(values.size == METRICS.size) { "invalid values array" }
        for (i in values.indices) {
            var stats = mStoredStatistics[i]
            if (null == stats) {
                stats = DescriptiveStatistics()
                mStoredStatistics[i] = stats
            }
            mStoredStatistics[i]!!.addValue(values[i])
        }
    }

    companion object {
        private const val METRIC_WAS_JANKY = -1
        private val METRICS = intArrayOf(
                FrameMetrics.UNKNOWN_DELAY_DURATION,
                FrameMetrics.INPUT_HANDLING_DURATION,
                FrameMetrics.ANIMATION_DURATION,
                FrameMetrics.LAYOUT_MEASURE_DURATION,
                FrameMetrics.DRAW_DURATION,
                FrameMetrics.SYNC_DURATION,
                FrameMetrics.COMMAND_ISSUE_DURATION,
                FrameMetrics.SWAP_BUFFERS_DURATION,
                FrameMetrics.TOTAL_DURATION)
    }
}
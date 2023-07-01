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
package com.android.benchmark.appimport

import android.os.AsyncTask
import android.os.Bundle
import android.view.FrameMetrics
import android.widget.SimpleAdapter
import androidx.fragment.app.ListFragment
import com.android.benchmark.R
import com.android.benchmark.results.GlobalResultsStore
import com.android.benchmark.results.UiBenchmarkResult
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

@TargetApi(24)
class UiResultsFragment : ListFragment() {
    private var mResults = ArrayList<UiBenchmarkResult?>()
    private val mLoadScoresTask: AsyncTask<Void, Void, ArrayList<Map<String, String>>> = object : AsyncTask<Void?, Void?, ArrayList<Map<String?, String?>?>?>() {
        protected override fun doInBackground(vararg voids: Void): ArrayList<Map<String, String?>> {
            val data: Array<String?>
            if (0 == mResults.size || null == mResults[0]) {
                data = arrayOf(
                        "No metrics reported", ""
                )
            } else {
                data = arrayOfNulls<String>(NUM_FIELDS * (1 + mResults.size) + 2)
                val stats = SummaryStatistics()
                var totalFrameCount = 0
                var totalAvgFrameDuration = 0.0
                var total99FrameDuration = 0.0
                var total95FrameDuration = 0.0
                var total90FrameDuration = 0.0
                var totalLongestFrame = 0.0
                var totalShortestFrame = 0.0
                for (i in mResults.indices) {
                    var start: Int = i * NUM_FIELDS + +NUM_FIELDS
                    data[start] = "Iteration"
                    start++
                    data[start] = i.toString()
                    start++
                    data[start] = "Total Frames"
                    start++
                    val currentFrameCount = mResults[i]!!.totalFrameCount
                    totalFrameCount += currentFrameCount
                    data[start] = Integer.toString(currentFrameCount)
                    start++
                    data[start] = "Average frame duration:"
                    start++
                    val currentAvgFrameDuration = mResults[i]!!.getAverage(FrameMetrics.TOTAL_DURATION)
                    totalAvgFrameDuration += currentAvgFrameDuration
                    data[start] = String.format("%.2f", currentAvgFrameDuration)
                    start++
                    data[start] = "Frame duration 99th:"
                    start++
                    val current99FrameDuration = mResults[i]!!.getPercentile(FrameMetrics.TOTAL_DURATION, 99)
                    total99FrameDuration += current99FrameDuration
                    data[start] = String.format("%.2f", current99FrameDuration)
                    start++
                    data[start] = "Frame duration 95th:"
                    start++
                    val current95FrameDuration = mResults[i]!!.getPercentile(FrameMetrics.TOTAL_DURATION, 95)
                    total95FrameDuration += current95FrameDuration
                    data[start] = String.format("%.2f", current95FrameDuration)
                    start++
                    data[start] = "Frame duration 90th:"
                    start++
                    val current90FrameDuration = mResults[i]!!.getPercentile(FrameMetrics.TOTAL_DURATION, 90)
                    total90FrameDuration += current90FrameDuration
                    data[start] = String.format("%.2f", current90FrameDuration)
                    start++
                    data[start] = "Longest frame:"
                    start++
                    val longestFrame = mResults[i]!!.getMaximum(FrameMetrics.TOTAL_DURATION)
                    if (0.0 == totalLongestFrame || longestFrame > totalLongestFrame) {
                        totalLongestFrame = longestFrame
                    }
                    data[start] = String.format("%.2f", longestFrame)
                    start++
                    data[start] = "Shortest frame:"
                    start++
                    val shortestFrame = mResults[i]!!.getMinimum(FrameMetrics.TOTAL_DURATION)
                    if (0.0 == totalShortestFrame || totalShortestFrame > shortestFrame) {
                        totalShortestFrame = shortestFrame
                    }
                    data[start] = String.format("%.2f", shortestFrame)
                    start++
                    data[start] = "Score:"
                    start++
                    val score = mResults[i]!!.score.toDouble()
                    stats.addValue(score)
                    data[start] = String.format("%.2f", score)
                    start++
                    data[start] = "=============="
                    start++
                    data[start] = "============================"
                    start++
                }
                var start = 0
                data[0] = "Overall: "
                data[1] = ""
                data[start] = "Total Frames"
                start++
                data[start] = Integer.toString(totalFrameCount)
                start++
                data[start] = "Average frame duration:"
                start++
                data[start] = String.format("%.2f", totalAvgFrameDuration / mResults.size)
                start++
                data[start] = "Frame duration 99th:"
                start++
                data[start] = String.format("%.2f", total99FrameDuration / mResults.size)
                start++
                data[start] = "Frame duration 95th:"
                start++
                data[start] = String.format("%.2f", total95FrameDuration / mResults.size)
                start++
                data[start] = "Frame duration 90th:"
                start++
                data[start] = String.format("%.2f", total90FrameDuration / mResults.size)
                start++
                data[start] = "Longest frame:"
                start++
                data[start] = String.format("%.2f", totalLongestFrame)
                start++
                data[start] = "Shortest frame:"
                start++
                data[start] = String.format("%.2f", totalShortestFrame)
                start++
                data[start] = "Score:"
                start++
                data[start] = String.format("%.2f", stats.geometricMean)
                start++
                data[start] = "=============="
                start++
                data[start] = "============================"
                start++
            }
            val dataMap = ArrayList<Map<String, String?>>()
            var i = 0
            while (i < data.size - 1) {
                val map = HashMap<String, String?>()
                map["name"] = data[i]
                map["value"] = data[i + 1]
                dataMap.add(map)
                i += 2
            }
            return dataMap
        }

        protected override fun onPostExecute(dataMap: ArrayList<Map<String?, String?>?>) {
            listAdapter = SimpleAdapter(activity, dataMap, R.layout.results_list_item, arrayOf("name", "value"), intArrayOf(R.id.result_name, R.id.result_value))
            setListShown(true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setListShown(false)
        mLoadScoresTask.execute()
    }

    fun setRunInfo(name: String?, runId: Int) {
        mResults = GlobalResultsStore.Companion.getInstance(activity).loadTestResults(name, runId)
    }

    companion object {
        private const val TAG = "UiResultsFragment"
        private const val NUM_FIELDS = 20
    }
}
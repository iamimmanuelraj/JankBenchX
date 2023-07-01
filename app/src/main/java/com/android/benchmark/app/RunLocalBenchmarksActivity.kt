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
package com.android.benchmark.appimport

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.ListFragment
import com.android.benchmark.R
import com.android.benchmark.api.JankBenchAPI
import com.android.benchmark.config.Constants
import com.android.benchmark.registry.BenchmarkGroup
import com.android.benchmark.registry.BenchmarkRegistry
import com.android.benchmark.results.GlobalResultsStore
import com.android.benchmark.synthetic.MemoryActivity
import com.android.benchmark.ui.BitmapUploadActivity
import com.android.benchmark.ui.EditTextInputActivity
import com.android.benchmark.ui.FullScreenOverdrawActivity
import com.android.benchmark.ui.ImageListViewScrollActivity
import com.android.benchmark.ui.ListViewScrollActivity
import com.android.benchmark.ui.ShadowGridActivity
import com.android.benchmark.ui.TextScrollActivity
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

class RunLocalBenchmarksActivity : AppCompatActivity() {
    private var mBenchmarksToRun: ArrayList<LocalBenchmark>? = null
    private var mBenchmarkCursor = 0
    private var mCurrentRunId = 0
    private var mFinish = false
    private val mHandler = Handler()

    class LocalBenchmarksList : ListFragment() {
        private var mBenchmarks: ArrayList<LocalBenchmark>? = null
        private var mRunId = 0
        fun setBenchmarks(benchmarks: ArrayList<LocalBenchmark>?) {
            mBenchmarks = benchmarks
        }

        fun setRunId(id: Int) {
            mRunId = id
        }

        override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
            if (null != this.activity!!.findViewById<View?>(R.id.list_fragment_container)) {
                val fm = activity!!.supportFragmentManager
                val resultsView = UiResultsFragment()
                val testName: String = BenchmarkRegistry.Companion.getBenchmarkName(v.context,
                        mBenchmarks!![position].id)
                resultsView.setRunInfo(testName, mRunId)
                val fragmentTransaction = fm.beginTransaction()
                fragmentTransaction.replace(R.id.list_fragment_container, resultsView)
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            }
        }
    }

    inner class LocalBenchmark internal constructor(var id: Int, var totalCount: Int) {
        var runCount = 0
        var mResultsUri = ArrayList<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running_list)
        initLocalBenchmarks(intent)
        if (null != findViewById(R.id.list_fragment_container)) {
            val fm = supportFragmentManager
            val listView = LocalBenchmarksList()
            listView.listAdapter = LocalBenchmarksListAdapter(LayoutInflater.from(this))
            listView.setBenchmarks(mBenchmarksToRun)
            listView.setRunId(mCurrentRunId)
            fm.beginTransaction().add(R.id.list_fragment_container, listView).commit()
        }
        val scoreView = findViewById<TextView>(R.id.score_text_view)
        scoreView.text = "Running tests!"
    }

    private fun translateBenchmarkIndex(index: Int): Int {
        return if (0 <= index && index < ALL_TESTS.size) {
            ALL_TESTS.get(index)
        } else -1
    }

    private fun initLocalBenchmarks(intent: Intent) {
        mBenchmarksToRun = ArrayList()
        var enabledIds = intent.getIntArrayExtra(BenchmarkGroup.Companion.BENCHMARK_EXTRA_ENABLED_TESTS)
        val runCount = intent.getIntExtra(BenchmarkGroup.Companion.BENCHMARK_EXTRA_RUN_COUNT, RUN_COUNT)
        mFinish = intent.getBooleanExtra(BenchmarkGroup.Companion.BENCHMARK_EXTRA_FINISH, false)
        if (null == enabledIds) {
            // run all tests
            enabledIds = ALL_TESTS
        }
        val idString = StringBuilder()
        idString.append(runCount)
        idString.append(System.currentTimeMillis())
        for (i in enabledIds.indices) {
            var id = enabledIds[i]
            println("considering $id")
            if (!isValidBenchmark(id)) {
                println("not valid $id")
                id = translateBenchmarkIndex(id)
                println("got out $id")
                println("expected: " + R.id.benchmark_overdraw)
            }
            if (isValidBenchmark(id)) {
                var localRunCount = runCount
                if (isCompute(id)) {
                    localRunCount = 1
                }
                mBenchmarksToRun!!.add(LocalBenchmark(id, localRunCount))
                idString.append(id)
            }
        }
        mBenchmarkCursor = 0
        mCurrentRunId = idString.toString().hashCode()
    }

    private fun isCompute(id: Int): Boolean {
        return id == R.id.benchmark_cpu_gflops || id == R.id.benchmark_cpu_heat_soak || id == R.id.benchmark_memory_bandwidth || id == R.id.benchmark_memory_latency || id == R.id.benchmark_power_management
    }

    override fun onResume() {
        super.onResume()
        mHandler.postDelayed({ runNextBenchmark() }, 1000)
    }

    private fun computeOverallScore() {
        val scoreView = findViewById<TextView>(R.id.score_text_view)
        scoreView.text = "Computing score..."
        object : AsyncTask<Void?, Void?, Int?>() {
            protected override fun doInBackground(vararg voids: Void): Int {
                val gsr: GlobalResultsStore = GlobalResultsStore.Companion.getInstance(this@RunLocalBenchmarksActivity)
                val testLevelScores = ArrayList<Double>()
                val stats = SummaryStatistics()
                for (b in mBenchmarksToRun!!) {
                    val detailedResults = gsr.loadDetailedResults(mCurrentRunId)
                    for (testResult in detailedResults.values) {
                        for (res in testResult) {
                            var score = res.score
                            if (0 == score) {
                                score = 1
                            }
                            stats.addValue(score.toDouble())
                        }
                        testLevelScores.add(stats.geometricMean)
                        stats.clear()
                    }
                }
                for (score in testLevelScores) {
                    stats.addValue(score)
                }
                return Math.round(stats.geometricMean).toInt()
            }

            protected override fun onPostExecute(score: Int) {
                val view = findViewById<TextView>(R.id.score_text_view)
                view.text = "Score: $score"
            }
        }.execute()
    }

    private fun runNextBenchmark() {
        var benchmark = mBenchmarksToRun!![mBenchmarkCursor]
        val runAgain = false
        if (benchmark.runCount < benchmark.totalCount) {
            runBenchmarkForId(mBenchmarksToRun!![mBenchmarkCursor].id, benchmark.runCount)
            benchmark.runCount++
        } else if (mBenchmarkCursor + 1 < mBenchmarksToRun!!.size) {
            mBenchmarkCursor++
            benchmark = mBenchmarksToRun!![mBenchmarkCursor]
            runBenchmarkForId(benchmark.id, benchmark.runCount)
            benchmark.runCount++
        } else if (runAgain) {
            mBenchmarkCursor = 0
            initLocalBenchmarks(intent)
            runBenchmarkForId(mBenchmarksToRun!![mBenchmarkCursor].id, benchmark.runCount)
        } else if (mFinish) {
            finish()
        } else {
            Log.i("BENCH", "BenchmarkDone!")
            computeOverallScore()
            object : AsyncTask<Void?, Void?, Void?>() {
                var success = false
                protected override fun doInBackground(vararg voids: Void): Void? {
                    runOnUiThread { Toast.makeText(this@RunLocalBenchmarksActivity, "Uploading results...", Toast.LENGTH_LONG).show() }
                    success = JankBenchAPI.Companion.uploadResults(this@RunLocalBenchmarksActivity, Constants.Companion.BASE_URL)
                    return null
                }

                protected override fun onPostExecute(aVoid: Void) {
                    runOnUiThread { Toast.makeText(this@RunLocalBenchmarksActivity, if (success) "Upload succeeded" else "Upload failed", Toast.LENGTH_LONG).show() }
                }
            }.execute()
        }
    }

    private fun runBenchmarkForId(id: Int, iteration: Int) {
        val intent: Intent?
        var syntheticTestId = -1
        println("iteration: $iteration")
        if (id == R.id.benchmark_list_view_scroll) {
            intent = Intent(applicationContext, ListViewScrollActivity::class.java)
        } else if (id == R.id.benchmark_image_list_view_scroll) {
            intent = Intent(applicationContext, ImageListViewScrollActivity::class.java)
        } else if (id == R.id.benchmark_shadow_grid) {
            intent = Intent(applicationContext, ShadowGridActivity::class.java)
        } else if (id == R.id.benchmark_text_high_hitrate) {
            intent = Intent(applicationContext, TextScrollActivity::class.java)
            intent.putExtra(TextScrollActivity.Companion.EXTRA_HIT_RATE, 80)
            intent.putExtra(BenchmarkRegistry.Companion.EXTRA_ID, id)
        } else if (id == R.id.benchmark_text_low_hitrate) {
            intent = Intent(applicationContext, TextScrollActivity::class.java)
            intent.putExtra(TextScrollActivity.Companion.EXTRA_HIT_RATE, 20)
            intent.putExtra(BenchmarkRegistry.Companion.EXTRA_ID, id)
        } else if (id == R.id.benchmark_edit_text_input) {
            intent = Intent(applicationContext, EditTextInputActivity::class.java)
        } else if (id == R.id.benchmark_overdraw) {
            intent = Intent(applicationContext, FullScreenOverdrawActivity::class.java)
        } else if (id == R.id.benchmark_bitmap_upload) {
            intent = Intent(applicationContext, BitmapUploadActivity::class.java)
        } else if (id == R.id.benchmark_memory_bandwidth) {
            syntheticTestId = 0
            intent = Intent(applicationContext, MemoryActivity::class.java)
            intent.putExtra("test", syntheticTestId)
        } else if (id == R.id.benchmark_memory_latency) {
            syntheticTestId = 1
            intent = Intent(applicationContext, MemoryActivity::class.java)
            intent.putExtra("test", syntheticTestId)
        } else if (id == R.id.benchmark_power_management) {
            syntheticTestId = 2
            intent = Intent(applicationContext, MemoryActivity::class.java)
            intent.putExtra("test", syntheticTestId)
        } else if (id == R.id.benchmark_cpu_heat_soak) {
            syntheticTestId = 3
            intent = Intent(applicationContext, MemoryActivity::class.java)
            intent.putExtra("test", syntheticTestId)
        } else if (id == R.id.benchmark_cpu_gflops) {
            syntheticTestId = 4
            intent = Intent(applicationContext, MemoryActivity::class.java)
            intent.putExtra("test", syntheticTestId)
        } else {
            intent = null
        }
        if (null != intent) {
            intent.putExtra("com.android.benchmark.RUN_ID", mCurrentRunId)
            intent.putExtra("com.android.benchmark.ITERATION", iteration)
            startActivityForResult(intent, id and 0xffff, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == R.id.benchmark_shadow_grid || requestCode == R.id.benchmark_list_view_scroll || requestCode == R.id.benchmark_image_list_view_scroll || requestCode == R.id.benchmark_text_high_hitrate || requestCode == R.id.benchmark_text_low_hitrate || requestCode == R.id.benchmark_edit_text_input) {
            // Do something
        } else {
            // Do something else
        }
    }

    internal inner class LocalBenchmarksListAdapter(private val mInflater: LayoutInflater) : BaseAdapter() {
        override fun getCount(): Int {
            return mBenchmarksToRun!!.size
        }

        override fun getItem(i: Int): Any {
            return mBenchmarksToRun!![i]
        }

        override fun getItemId(i: Int): Long {
            return mBenchmarksToRun!![i].id.toLong()
        }

        override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (null == convertView) {
                convertView = mInflater.inflate(R.layout.running_benchmark_list_item, null)
            }
            val name = convertView!!.findViewById<TextView>(R.id.benchmark_name)
            name.setText(BenchmarkRegistry.Companion.getBenchmarkName(
                    this@RunLocalBenchmarksActivity, mBenchmarksToRun!![i].id))
            return convertView
        }
    }

    companion object {
        const val RUN_COUNT = 5
        private val ALL_TESTS = intArrayOf(
                R.id.benchmark_list_view_scroll,
                R.id.benchmark_image_list_view_scroll,
                R.id.benchmark_shadow_grid,
                R.id.benchmark_text_high_hitrate,
                R.id.benchmark_text_low_hitrate,
                R.id.benchmark_edit_text_input,
                R.id.benchmark_overdraw,
                R.id.benchmark_bitmap_upload)

        private fun isValidBenchmark(benchmarkId: Int): Boolean {
            return benchmarkId == R.id.benchmark_cpu_gflops || benchmarkId == R.id.benchmark_cpu_heat_soak || benchmarkId == R.id.benchmark_memory_bandwidth || benchmarkId == R.id.benchmark_memory_latency || benchmarkId == R.id.benchmark_power_management
        }
    }
}
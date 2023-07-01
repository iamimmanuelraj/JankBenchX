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

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.view.FrameMetrics
import android.widget.Toast
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import java.io.FileWriter
import java.io.IOException
import java.text.DateFormat
import java.util.Date

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

class GlobalResultsStore private constructor(private val mContext: Context) : SQLiteOpenHelper(mContext, "BenchmarkResults", null, VERSION) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + UI_RESULTS_TABLE + " (" +
                " _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " name TEXT," +
                " run_id INTEGER," +
                " iteration INTEGER," +
                " timestamp TEXT," +
                " unknown_delay REAL," +
                " input REAL," +
                " animation REAL," +
                " layout REAL," +
                " draw REAL," +
                " sync REAL," +
                " command_issue REAL," +
                " swap_buffers REAL," +
                " total_duration REAL," +
                " jank_frame BOOLEAN, " +
                " device_charging INTEGER);")
        sqLiteDatabase.execSQL("CREATE TABLE " + REFRESH_RATE_TABLE + " (" +
                " _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " run_id INTEGER," +
                " refresh_rate INTEGER);")
    }

    fun storeRunResults(testName: String?, runId: Int, iteration: Int,
                        result: UiBenchmarkResult, refresh_rate: Float) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val date = DateFormat.getDateTimeInstance().format(Date())
            var jankIndexIndex = 0
            val sortedJankIndices = result.sortedJankFrameIndices
            val totalFrameCount = result.totalFrameCount
            for (frameIdx in 0 until totalFrameCount) {
                val cv = ContentValues()
                cv.put("name", testName)
                cv.put("run_id", runId)
                cv.put("iteration", iteration)
                cv.put("timestamp", date)
                cv.put("unknown_delay",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.UNKNOWN_DELAY_DURATION))
                cv.put("input",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.INPUT_HANDLING_DURATION))
                cv.put("animation",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.ANIMATION_DURATION))
                cv.put("layout",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.LAYOUT_MEASURE_DURATION))
                cv.put("draw",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.DRAW_DURATION))
                cv.put("sync",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.SYNC_DURATION))
                cv.put("command_issue",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.COMMAND_ISSUE_DURATION))
                cv.put("swap_buffers",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.SWAP_BUFFERS_DURATION))
                cv.put("total_duration",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.TOTAL_DURATION))
                if (jankIndexIndex < sortedJankIndices.size &&
                        sortedJankIndices[jankIndexIndex] == frameIdx) {
                    jankIndexIndex++
                    cv.put("jank_frame", true)
                } else {
                    cv.put("jank_frame", false)
                }
                db.insert(UI_RESULTS_TABLE, null, cv)
            }

            // Store Display Refresh Rate
            val cv = ContentValues()
            cv.put("run_id", runId)
            cv.put("refresh_rate", Math.round(refresh_rate))
            db.insert(REFRESH_RATE_TABLE, null, cv)
            db.setTransactionSuccessful()
            Toast.makeText(mContext, ("Score: " + result.score
                    + " Jank: ") + 100 * sortedJankIndices.size / totalFrameCount.toFloat() + "%",
                    Toast.LENGTH_LONG).show()
        } finally {
            db.endTransaction()
        }
    }

    fun loadTestResults(testName: String, runId: Int): ArrayList<UiBenchmarkResult> {
        val db = readableDatabase
        val resultList = ArrayList<UiBenchmarkResult>()
        try {
            val columnsToQuery = arrayOf(
                    "name",
                    "run_id",
                    "iteration",
                    "unknown_delay",
                    "input",
                    "animation",
                    "layout",
                    "draw",
                    "sync",
                    "command_issue",
                    "swap_buffers",
                    "total_duration")
            val cursor = db.query(
                    UI_RESULTS_TABLE, columnsToQuery, "run_id=? AND name=?", arrayOf<String>(Integer.toString(runId), testName), null, null, "iteration")
            val values = DoubleArray(columnsToQuery.size - 3)
            while (cursor.moveToNext()) {
                val iteration = cursor.getInt(cursor.getColumnIndexOrThrow("iteration"))
                values[0] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("unknown_delay"))
                values[1] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("input"))
                values[2] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("animation"))
                values[3] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("layout"))
                values[4] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("draw"))
                values[5] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("sync"))
                values[6] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("command_issue"))
                values[7] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("swap_buffers"))
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"))
                var iterationResult: UiBenchmarkResult
                if (resultList.size == iteration) {
                    val refresh_rate = loadRefreshRate(runId, db)
                    iterationResult = UiBenchmarkResult(values, refresh_rate)
                    resultList.add(iteration, iterationResult)
                } else {
                    iterationResult = resultList[iteration]
                    iterationResult.update(values)
                }
            }
            cursor.close()
        } finally {
            db.close()
        }
        val total = resultList[0].totalFrameCount
        for (i in 0 until total) {
            println(resultList[0].getMetricAtIndex(0, FrameMetrics.TOTAL_DURATION))
        }
        return resultList
    }

    fun loadDetailedResults(runId: Int): HashMap<String, ArrayList<UiBenchmarkResult>> {
        val db = readableDatabase
        val results = HashMap<String, ArrayList<UiBenchmarkResult>>()
        try {
            val columnsToQuery = arrayOf(
                    "name",
                    "run_id",
                    "iteration",
                    "unknown_delay",
                    "input",
                    "animation",
                    "layout",
                    "draw",
                    "sync",
                    "command_issue",
                    "swap_buffers",
                    "total_duration")
            val cursor = db.query(
                    UI_RESULTS_TABLE, columnsToQuery, "run_id=?", arrayOf<String>(Integer.toString(runId)), null, null, "name, iteration")
            val values = DoubleArray(columnsToQuery.size - 3)
            while (cursor.moveToNext()) {
                val iteration = cursor.getInt(cursor.getColumnIndexOrThrow("iteration"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                var resultList = results[name]
                if (null == resultList) {
                    resultList = ArrayList()
                    results[name] = resultList
                }
                values[0] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("unknown_delay"))
                values[1] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("input"))
                values[2] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("animation"))
                values[3] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("layout"))
                values[4] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("draw"))
                values[5] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("sync"))
                values[6] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("command_issue"))
                values[7] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("swap_buffers"))
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"))
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"))
                var iterationResult: UiBenchmarkResult
                if (resultList.size == iteration) {
                    val refresh_rate = loadRefreshRate(runId, db)
                    iterationResult = UiBenchmarkResult(values, refresh_rate)
                    resultList.add(iterationResult)
                } else {
                    iterationResult = resultList[iteration]
                    iterationResult.update(values)
                }
            }
            cursor.close()
        } finally {
            db.close()
        }
        return results
    }

    fun getLastRunId(): Int {
        var runId = 0
        val db = readableDatabase
        try {
            val query = "SELECT run_id FROM " + UI_RESULTS_TABLE + " WHERE _id = (SELECT MAX(_id) FROM " + UI_RESULTS_TABLE + ")"
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                runId = cursor.getInt(0)
            }
            cursor.close()
        } finally {
            db.close()
        }
        return runId
    }

    fun loadRefreshRate(runId: Int, db: SQLiteDatabase): Int {
        var refresh_rate = -1
        val columnsToQuery = arrayOf(
                "run_id",
                "refresh_rate"
        )
        val cursor = db.query(REFRESH_RATE_TABLE, columnsToQuery, "run_id=?", arrayOf<String>(Integer.toString(runId)), null, null, null)
        if (cursor.moveToFirst()) {
            refresh_rate = cursor.getInt(1)
        }
        cursor.close()
        return refresh_rate
    }

    fun loadDetailedAggregatedResults(runId: Int): HashMap<String, UiBenchmarkResult> {
        val db = readableDatabase
        val testsResults = HashMap<String, UiBenchmarkResult>()
        try {
            val columnsToQuery = arrayOf(
                    "name",
                    "run_id",
                    "iteration",
                    "unknown_delay",
                    "input",
                    "animation",
                    "layout",
                    "draw",
                    "sync",
                    "command_issue",
                    "swap_buffers",
                    "total_duration")
            val cursor = db.query(
                    UI_RESULTS_TABLE, columnsToQuery, "run_id=?", arrayOf<String>(Integer.toString(runId)), null, null, "name")
            val values = DoubleArray(columnsToQuery.size - 3)
            while (cursor.moveToNext()) {
                val testName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                values[0] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("unknown_delay"))
                values[1] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("input"))
                values[2] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("animation"))
                values[3] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("layout"))
                values[4] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("draw"))
                values[5] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("sync"))
                values[6] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("command_issue"))
                values[7] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("swap_buffers"))
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"))
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"))
                var result = testsResults[testName]
                if (null == result) {
                    val refresh_rate = loadRefreshRate(runId, db)
                    result = UiBenchmarkResult(values, refresh_rate)
                    testsResults[testName] = result
                } else {
                    result.update(values)
                }
            }
            cursor.close()
        } finally {
            db.close()
        }
        return testsResults
    }

    @Throws(IOException::class)
    fun exportToCsv() {
        val path = mContext.filesDir.toString() + "/results-" + System.currentTimeMillis() + ".csv"
        val db = readableDatabase

        // stats across metrics for each run and each test
        val stats = HashMap<String, DescriptiveStatistics>()
        val runIdCursor = db.query(
                UI_RESULTS_TABLE, arrayOf<String>("run_id"), null, null, "run_id", null, null)
        while (runIdCursor.moveToNext()) {
            val runId = runIdCursor.getInt(runIdCursor.getColumnIndexOrThrow("run_id"))
            val detailedResults = loadDetailedResults(runId)
            writeRawResults(runId, detailedResults)
            val overall = DescriptiveStatistics()
            FileWriter(path, true).use { writer ->
                writer.write("Run ID, $runId\n")
                writer.write("""
    Test, Iteration, Score, Jank Penalty, Consistency Bonus, 95th, 90th
    
    """.trimIndent())
                for (testName in detailedResults.keys) {
                    val results = detailedResults[testName]!!
                    val scoreStats = DescriptiveStatistics()
                    val jankPenalty = DescriptiveStatistics()
                    val consistencyBonus = DescriptiveStatistics()
                    for (i in results.indices) {
                        val result = results[i]
                        val score = result.score
                        scoreStats.addValue(score.toDouble())
                        overall.addValue(score.toDouble())
                        jankPenalty.addValue(result.jankPenalty.toDouble())
                        consistencyBonus.addValue(result.consistencyBonus.toDouble())
                        writer.write(testName)
                        writer.write(",")
                        writer.write(i.toString())
                        writer.write(",")
                        writer.write(score.toString())
                        writer.write(",")
                        writer.write(result.jankPenalty.toString())
                        writer.write(",")
                        writer.write(result.consistencyBonus.toString())
                        writer.write(",")
                        writer.write(java.lang.Double.toString(
                                result.getPercentile(FrameMetrics.TOTAL_DURATION, 95)))
                        writer.write(",")
                        writer.write(java.lang.Double.toString(
                                result.getPercentile(FrameMetrics.TOTAL_DURATION, 90)))
                        writer.write("\n")
                    }
                    writer.write("Score CV," +
                            (100 * scoreStats.standardDeviation
                                    / scoreStats.mean) + "%\n")
                    writer.write("Jank Penalty CV, " +
                            (100 * jankPenalty.standardDeviation
                                    / jankPenalty.mean) + "%\n")
                    writer.write("Consistency Bonus CV, " +
                            (100 * consistencyBonus.standardDeviation
                                    / consistencyBonus.mean) + "%\n")
                    writer.write("\n")
                }
                writer.write("Overall Score CV," + 100 * overall.standardDeviation / overall.mean + "%\n")
                writer.flush()
            }
        }
        runIdCursor.close()
    }

    private fun writeRawResults(runId: Int,
                                detailedResults: HashMap<String, ArrayList<UiBenchmarkResult>>) {
        val path = mContext.filesDir.toString() +
                "/" +
                runId +
                ".csv"
        try {
            FileWriter(path).use { writer ->
                for (test in detailedResults.keys) {
                    writer.write("Test, $test\n")
                    writer.write("""
    iteration, unknown delay, input, animation, layout, draw, sync, command issue, swap buffers
    
    """.trimIndent())
                    val runs = detailedResults[test]!!
                    for (i in runs.indices) {
                        val run = runs[i]
                        for (j in 0 until run.totalFrameCount) {
                            writer.write("""
    $i,${run.getMetricAtIndex(j, FrameMetrics.UNKNOWN_DELAY_DURATION)},${run.getMetricAtIndex(j, FrameMetrics.INPUT_HANDLING_DURATION)},${run.getMetricAtIndex(j, FrameMetrics.ANIMATION_DURATION)},${run.getMetricAtIndex(j, FrameMetrics.LAYOUT_MEASURE_DURATION)},${run.getMetricAtIndex(j, FrameMetrics.DRAW_DURATION)},${run.getMetricAtIndex(j, FrameMetrics.SYNC_DURATION)},${run.getMetricAtIndex(j, FrameMetrics.COMMAND_ISSUE_DURATION)},${run.getMetricAtIndex(j, FrameMetrics.SWAP_BUFFERS_DURATION)},${run.getMetricAtIndex(j, FrameMetrics.TOTAL_DURATION)}
    
    """.trimIndent())
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, currentVersion: Int) {
        if (VERSION > oldVersion) {
            sqLiteDatabase.execSQL("ALTER TABLE "
                    + UI_RESULTS_TABLE + " ADD COLUMN timestamp TEXT;")
        }
    }

    companion object {
        private const val VERSION = 2
        private val sInstance: GlobalResultsStore? = null
        private const val UI_RESULTS_TABLE = "ui_results"
        private const val REFRESH_RATE_TABLE = "refresh_rates"
        fun getInstance(context: Context): GlobalResultsStore {
            if (null == sInstance) {
                sInstance = GlobalResultsStore(context.applicationContext)
            }
            return sInstance
        }
    }
}
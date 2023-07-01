package com.android.benchmark.apiimport

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.view.FrameMetrics
import com.android.benchmark.config.Constants
import com.android.benchmark.models.Entry
import com.android.benchmark.results.GlobalResultsStore
import com.android.benchmark.results.UiBenchmarkResult
import com.topjohnwu.superuser.Shell
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

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

enum class JankBenchAPI {
    ;

    companion object {
        fun uploadResults(context: Context?, baseUrl: String): Boolean {
            var success = false
            val entry: Entry = JankBenchAPI.Companion.createEntry(context)
            try {
                success = JankBenchAPI.Companion.upload(entry, baseUrl)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return success
        }

        @Throws(IOException::class)
        private fun upload(entry: Entry, url: String): Boolean {
            val retrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            val resource = retrofit.create(JankBenchService::class.java)
            val call = resource.uploadEntry(entry)
            val response = call.execute()
            return response.isSuccessful
        }

        private fun createEntry(context: Context): Entry {
            val lastRunId: Int = GlobalResultsStore.Companion.getInstance(context).getLastRunId()
            val db: SQLiteDatabase = GlobalResultsStore.Companion.getInstance(context).getReadableDatabase()
            val lastRunRefreshRate: Int
            lastRunRefreshRate = try {
                GlobalResultsStore.Companion.getInstance(context).loadRefreshRate(lastRunId, db)
            } finally {
                db.close()
            }
            val resultsMap: HashMap<String, UiBenchmarkResult> = GlobalResultsStore.Companion.getInstance(context).loadDetailedAggregatedResults(lastRunId)
            val entry = Entry()
            entry.runId = lastRunId
            entry.benchmarkVersion = Constants.Companion.BENCHMARK_VERSION
            entry.deviceName = Build.DEVICE
            entry.deviceModel = Build.MODEL
            entry.deviceProduct = Build.PRODUCT
            entry.deviceBoard = Build.BOARD
            entry.deviceManufacturer = Build.MANUFACTURER
            entry.deviceBrand = Build.BRAND
            entry.deviceHardware = Build.HARDWARE
            entry.androidVersion = Build.VERSION.RELEASE
            entry.buildType = Build.TYPE
            entry.buildTime = Build.TIME.toString()
            entry.fingerprint = Build.FINGERPRINT
            entry.refreshRate = lastRunRefreshRate
            val kernel_version: String = JankBenchAPI.Companion.getKernelVersion()
            entry.kernelVersion = kernel_version
            val results: MutableList<Result> = ArrayList()
            for ((testName, uiResult) in resultsMap) {
                val result = Result()
                result.testName = testName
                result.score = uiResult.score
                result.jankPenalty = uiResult.jankPenalty
                result.consistencyBonus = uiResult.consistencyBonus
                result.jankPct = 100 * uiResult.numJankFrames / uiResult.totalFrameCount.toDouble()
                result.badFramePct = 100 * uiResult.numBadFrames / uiResult.totalFrameCount.toDouble()
                result.totalFrames = uiResult.totalFrameCount
                result.msAvg = uiResult.getAverage(FrameMetrics.TOTAL_DURATION)
                result.ms10thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 10)
                result.ms20thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 20)
                result.ms30thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 30)
                result.ms40thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 40)
                result.ms50thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 50)
                result.ms60thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 60)
                result.ms70thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 70)
                result.ms80thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 80)
                result.ms90thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 90)
                result.ms95thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 95)
                result.ms99thPctl = uiResult.getPercentile(FrameMetrics.TOTAL_DURATION, 99)
                results.add(result)
            }
            entry.results = results
            return entry
        }

        private val kernelVersion: String?
            private get() {
                val unameOutput = Shell.sh("uname -a").exec().out
                return if (0 == unameOutput.size) null else unameOutput[0]
            }
    }
}
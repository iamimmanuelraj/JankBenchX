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
package com.android.benchmark.registryimport

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.SparseArray
import android.util.Xml
import com.android.benchmark.R
import com.android.benchmark.registry.BenchmarkGroup.Benchmark
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
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

/**
 */
class BenchmarkRegistry(private val mContext: Context) {
    private val mGroups: MutableList<BenchmarkGroup>

    init {
        mGroups = ArrayList()
        loadBenchmarks()
    }

    private fun getIntentFromInfo(inf: ActivityInfo): Intent {
        val intent = Intent()
        intent.setClassName(inf.packageName, inf.name)
        return intent
    }

    fun loadBenchmarks() {
        val intent = Intent(ACTION_BENCHMARK)
        intent.setPackage(mContext.packageName)
        val pm = mContext.packageManager
        val resolveInfos = pm.queryIntentActivities(intent,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA)
        for (inf in resolveInfos) {
            val groups = parseBenchmarkGroup(inf.activityInfo)
            if (null != groups) {
                mGroups.addAll(groups)
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun seekToTag(parser: XmlPullParser, tag: String): Boolean {
        var eventType = parser.eventType
        while (XmlPullParser.START_TAG != eventType && XmlPullParser.END_DOCUMENT != eventType) {
            eventType = parser.next()
        }
        return XmlPullParser.END_DOCUMENT != eventType && tag == parser.name
    }

    @BenchmarkCategory
    fun getCategory(category: Int): Int {
        return if (BenchmarkCategory.Companion.COMPUTE == category) {
            BenchmarkCategory.Companion.COMPUTE
        } else if (BenchmarkCategory.Companion.UI == category) {
            BenchmarkCategory.Companion.UI
        } else {
            BenchmarkCategory.Companion.GENERIC
        }
    }

    private fun parseBenchmarkGroup(activityInfo: ActivityInfo): List<BenchmarkGroup>? {
        val pm = mContext.packageManager
        val componentName = ComponentName(
                activityInfo.packageName, activityInfo.name)
        val benchmarks = SparseArray<MutableList<Benchmark>>()
        var groupName: String
        var groupDescription: String
        try {
            activityInfo.loadXmlMetaData(pm, BENCHMARK_GROUP_META_KEY).use { parser ->
                if (!seekToTag(parser, Companion.TAG_BENCHMARK_GROUP)) {
                    return null
                }
                val res = pm.getResourcesForActivity(componentName)
                val attributeSet = Xml.asAttributeSet(parser)
                val groupAttribs = res.obtainAttributes(attributeSet, R.styleable.BenchmarkGroup)
                groupName = groupAttribs.getString(R.styleable.BenchmarkGroup_name)
                groupDescription = groupAttribs.getString(R.styleable.BenchmarkGroup_description)
                groupAttribs.recycle()
                parser.next()
                while (seekToTag(parser, TAG_BENCHMARK)) {
                    val benchAttribs = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Benchmark)
                    val id = benchAttribs.getResourceId(R.styleable.Benchmark_id, -1)
                    val testName = benchAttribs.getString(R.styleable.Benchmark_name)
                    val testDescription = benchAttribs.getString(R.styleable.Benchmark_description)
                    val testCategory = benchAttribs.getInt(R.styleable.Benchmark_category,
                            BenchmarkCategory.Companion.GENERIC)
                    val category = getCategory(testCategory)
                    val benchmark = Benchmark(
                            id, testName, category, testDescription)
                    var benches = benchmarks[category]
                    if (null == benches) {
                        benches = ArrayList()
                        benchmarks.append(category, benches)
                    }
                    benches.add(benchmark)
                    benchAttribs.recycle()
                    parser.next()
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        } catch (e: XmlPullParserException) {
            return null
        } catch (e: IOException) {
            return null
        }
        val result: MutableList<BenchmarkGroup> = ArrayList()
        val testIntent = getIntentFromInfo(activityInfo)
        for (i in 0 until benchmarks.size()) {
            val cat = benchmarks.keyAt(i)
            val thisGroup: List<Benchmark> = benchmarks[cat]
            val benchmarkArray = arrayOfNulls<Benchmark>(thisGroup.size)
            thisGroup.toArray<Benchmark>(benchmarkArray)
            result.add(BenchmarkGroup(componentName,
                    groupName + " - " + getCategoryString(cat), groupDescription, benchmarkArray,
                    testIntent))
        }
        return result
    }

    fun getGroupCount(): Int {
        return mGroups.size
    }

    fun getBenchmarkCount(benchmarkIndex: Int): Int {
        val group = getBenchmarkGroup(benchmarkIndex)
        return group?.benchmarks?.size ?: 0
    }

    fun getBenchmarkGroup(benchmarkIndex: Int): BenchmarkGroup? {
        return if (benchmarkIndex >= mGroups.size) {
            null
        } else mGroups[benchmarkIndex]
    }

    companion object {
        /** Metadata key for benchmark XML data  */
        private const val BENCHMARK_GROUP_META_KEY = "com.android.benchmark.benchmark_group"

        /** Intent action specifying an activity that runs a single benchmark test.  */
        private const val ACTION_BENCHMARK = "com.android.benchmark.ACTION_BENCHMARK"
        const val EXTRA_ID = "com.android.benchmark.EXTRA_ID"
        private const val TAG_BENCHMARK_GROUP = "com.android.benchmark.BenchmarkGroup"
        private const val TAG_BENCHMARK = "com.android.benchmark.Benchmark"
        fun getCategoryString(category: Int): String {
            return if (BenchmarkCategory.Companion.COMPUTE == category) {
                "Compute"
            } else if (BenchmarkCategory.Companion.UI == category) {
                "UI"
            } else if (BenchmarkCategory.Companion.GENERIC == category) {
                "Generic"
            } else {
                ""
            }
        }

        fun getBenchmarkName(context: Context, benchmarkId: Int): String {
            return if (benchmarkId == R.id.benchmark_list_view_scroll) {
                context.getString(R.string.list_view_scroll_name)
            } else if (benchmarkId == R.id.benchmark_image_list_view_scroll) {
                context.getString(R.string.image_list_view_scroll_name)
            } else if (benchmarkId == R.id.benchmark_shadow_grid) {
                context.getString(R.string.shadow_grid_name)
            } else if (benchmarkId == R.id.benchmark_text_high_hitrate) {
                context.getString(R.string.text_high_hitrate_name)
            } else if (benchmarkId == R.id.benchmark_text_low_hitrate) {
                context.getString(R.string.text_low_hitrate_name)
            } else if (benchmarkId == R.id.benchmark_edit_text_input) {
                context.getString(R.string.edit_text_input_name)
            } else if (benchmarkId == R.id.benchmark_memory_bandwidth) {
                context.getString(R.string.memory_bandwidth_name)
            } else if (benchmarkId == R.id.benchmark_memory_latency) {
                context.getString(R.string.memory_latency_name)
            } else if (benchmarkId == R.id.benchmark_power_management) {
                context.getString(R.string.power_management_name)
            } else if (benchmarkId == R.id.benchmark_cpu_heat_soak) {
                context.getString(R.string.cpu_heat_soak_name)
            } else if (benchmarkId == R.id.benchmark_cpu_gflops) {
                context.getString(R.string.cpu_gflops_name)
            } else if (benchmarkId == R.id.benchmark_overdraw) {
                context.getString(R.string.overdraw_name)
            } else if (benchmarkId == R.id.benchmark_bitmap_upload) {
                context.getString(R.string.bitmap_upload_name)
            } else {
                "Some Benchmark"
            }
        }
    }
}
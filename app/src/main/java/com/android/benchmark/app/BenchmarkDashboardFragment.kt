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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.benchmark.R

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
 * Fragment for the Benchmark dashboard
 */
class BenchmarkDashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
}
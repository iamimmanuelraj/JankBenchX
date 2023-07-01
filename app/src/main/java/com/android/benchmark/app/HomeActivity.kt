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
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.android.benchmark.R
import com.android.benchmark.api.JankBenchAPI
import com.android.benchmark.config.Constants
import com.android.benchmark.registry.BenchmarkRegistry
import com.android.benchmark.results.GlobalResultsStore
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.topjohnwu.superuser.Shell
import java.io.IOException
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

class HomeActivity : AppCompatActivity(), View.OnClickListener {
    private var mStartButton: FloatingActionButton? = null
    private var mRegistry: BenchmarkRegistry? = null
    private var mRunnableBenchmarks: Queue<Intent>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        mStartButton = findViewById(R.id.start_button)
        mStartButton.setActivated(true)
        mStartButton.setOnClickListener(this)
        mRegistry = BenchmarkRegistry(this)
        mRunnableBenchmarks = LinkedList()
        val listView = findViewById<ExpandableListView>(R.id.test_list)
        val adapter = BenchmarkListAdapter(LayoutInflater.from(this), mRegistry!!)
        listView.setAdapter(adapter)
        adapter.notifyDataSetChanged()
        val layoutParams = listView.layoutParams
        layoutParams.height = 2048
        listView.layoutParams = layoutParams
        listView.requestLayout()
        println(System.getProperties().stringPropertyNames())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_settings) {
            object : AsyncTask<Void?, Void?, Void?>() {
                protected override fun doInBackground(vararg voids: Void): Void? {
                    try {
                        runOnUiThread { Toast.makeText(this@HomeActivity, "Exporting...", Toast.LENGTH_LONG).show() }
                        GlobalResultsStore.Companion.getInstance(this@HomeActivity).exportToCsv()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return null
                }

                protected override fun onPostExecute(aVoid: Void) {
                    runOnUiThread { Toast.makeText(this@HomeActivity, "Done", Toast.LENGTH_LONG).show() }
                }
            }.execute()
            return true
        } else if (id == R.id.action_upload) {
            object : AsyncTask<Void?, Void?, Void?>() {
                var success = false
                protected override fun doInBackground(vararg voids: Void): Void? {
                    runOnUiThread { Toast.makeText(this@HomeActivity, "Uploading results...", Toast.LENGTH_LONG).show() }
                    success = JankBenchAPI.Companion.uploadResults(this@HomeActivity, Constants.Companion.BASE_URL)
                    return null
                }

                protected override fun onPostExecute(aVoid: Void) {
                    runOnUiThread { Toast.makeText(this@HomeActivity, if (success) "Upload succeeded" else "Upload failed", Toast.LENGTH_LONG).show() }
                }
            }.execute()
            return true
        } else if (id == R.id.action_view_results) {
            val webpage = Uri.parse("https://jankbenchx.vercel.app")
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            if (null != intent.resolveActivity(this.packageManager)) {
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        val groupCount = mRegistry!!.groupCount
        for (i in 0 until groupCount) {
            val intent = mRegistry!!.getBenchmarkGroup(i)!!.intent
            if (null != intent) {
                mRunnableBenchmarks!!.add(intent)
            }
        }
        handleNextBenchmark()
    }

    private fun handleNextBenchmark() {
        val nextIntent = mRunnableBenchmarks!!.peek()
        startActivityForResult(nextIntent, 0)
    }

    companion object {
        init {
            /* Shell.Config methods shall be called before any shell is created
         * This is the why in this example we call it in a static block
         * The followings are some examples, check Javadoc for more details */
            Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR or Shell.FLAG_NON_ROOT_SHELL)
            Shell.Config.setTimeout(10)
        }
    }
}
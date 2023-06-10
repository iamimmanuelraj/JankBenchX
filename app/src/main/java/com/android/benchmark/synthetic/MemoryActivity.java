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

package com.android.benchmark.synthetic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.benchmark.R;
import com.android.benchmark.app.PerfTimeline;


public class MemoryActivity extends Activity {
    private TextView mTextStatus;
    private TextView mTextMin;
    private TextView mTextMax;
    private TextView mTextTypical;
    private PerfTimeline mTimeline;

    TestInterface mTI;
    int mActiveTest;

    private class SyntheticTestCallback extends TestInterface.TestResultCallback {
        @Override
        void onTestResult(final int command, final float result) {
            final Intent resultIntent = new Intent();
            resultIntent.putExtra("com.android.benchmark.synthetic.TEST_RESULT", result);
            MemoryActivity.this.setResult(Activity.RESULT_OK, resultIntent);
            MemoryActivity.this.finish();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_memory);

        this.mTextStatus = this.findViewById(R.id.textView_status);
        this.mTextMin = this.findViewById(R.id.textView_min);
        this.mTextMax = this.findViewById(R.id.textView_max);
        this.mTextTypical = this.findViewById(R.id.textView_typical);

        this.mTimeline = this.findViewById(R.id.mem_timeline);

        this.mTI = new TestInterface(this.mTimeline, 2, new SyntheticTestCallback());
        this.mTI.mTextMax = this.mTextMax;
        this.mTI.mTextMin = this.mTextMin;
        this.mTI.mTextStatus = this.mTextStatus;
        this.mTI.mTextTypical = this.mTextTypical;

        this.mTimeline.mLinesLow = this.mTI.mLinesLow;
        this.mTimeline.mLinesHigh = this.mTI.mLinesHigh;
        this.mTimeline.mLinesValue = this.mTI.mLinesValue;

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Intent i = this.getIntent();
        this.mActiveTest = i.getIntExtra("test", 0);

        if (0 == mActiveTest) {
            this.mTI.runMemoryBandwidth();
        } else if (1 == mActiveTest) {
            this.mTI.runMemoryLatency();
        } else if (2 == mActiveTest) {
            this.mTI.runPowerManagement();
        } else if (3 == mActiveTest) {
            this.mTI.runCPUHeatSoak();
        } else if (4 == mActiveTest) {
            this.mTI.runCPUGFlops();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.menu_memory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onCpuBandwidth(final View v) {


    }




}

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

package com.android.benchmark.app;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.benchmark.R;
import com.android.benchmark.api.JankBenchAPI;
import com.android.benchmark.config.Constants;
import com.android.benchmark.registry.BenchmarkGroup;
import com.android.benchmark.registry.BenchmarkRegistry;
import com.android.benchmark.results.GlobalResultsStore;
import com.android.benchmark.results.UiBenchmarkResult;
import com.android.benchmark.synthetic.MemoryActivity;
import com.android.benchmark.ui.BitmapUploadActivity;
import com.android.benchmark.ui.EditTextInputActivity;
import com.android.benchmark.ui.FullScreenOverdrawActivity;
import com.android.benchmark.ui.ImageListViewScrollActivity;
import com.android.benchmark.ui.ListViewScrollActivity;
import com.android.benchmark.ui.ShadowGridActivity;
import com.android.benchmark.ui.TextScrollActivity;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.HashMap;

public class RunLocalBenchmarksActivity extends AppCompatActivity {

    public static final int RUN_COUNT = 5;

    private ArrayList<LocalBenchmark> mBenchmarksToRun;
    private int mBenchmarkCursor;
    private int mCurrentRunId;
    private boolean mFinish;

    private final Handler mHandler = new Handler();

    private static final int[] ALL_TESTS = {
            R.id.benchmark_list_view_scroll,
            R.id.benchmark_image_list_view_scroll,
            R.id.benchmark_shadow_grid,
            R.id.benchmark_text_high_hitrate,
            R.id.benchmark_text_low_hitrate,
            R.id.benchmark_edit_text_input,
            R.id.benchmark_overdraw,
            R.id.benchmark_bitmap_upload,
    };

    public static class LocalBenchmarksList extends ListFragment {
        private ArrayList<LocalBenchmark> mBenchmarks;
        private int mRunId;

        public void setBenchmarks(final ArrayList<LocalBenchmark> benchmarks) {
            this.mBenchmarks = benchmarks;
        }

        public void setRunId(final int id) {
            this.mRunId = id;
        }

        @Override
        public void onListItemClick(final ListView l, final View v, final int position, final long id) {
            if (null != getActivity().findViewById(R.id.list_fragment_container)) {
                final FragmentManager fm = this.getActivity().getSupportFragmentManager();
                final UiResultsFragment resultsView = new UiResultsFragment();
                final String testName = BenchmarkRegistry.getBenchmarkName(v.getContext(),
                        this.mBenchmarks.get(position).id);
                resultsView.setRunInfo(testName, this.mRunId);
                final FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(R.id.list_fragment_container, resultsView);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }
    }


    private class LocalBenchmark {
        int id;
        int runCount;
        int totalCount;
        ArrayList<String> mResultsUri = new ArrayList<>();

        LocalBenchmark(final int id, final int runCount) {
            this.id = id;
            this.runCount = 0;
            totalCount = runCount;
        }

    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_running_list);

        this.initLocalBenchmarks(this.getIntent());

        if (null != findViewById(R.id.list_fragment_container)) {
            final FragmentManager fm = this.getSupportFragmentManager();
            final LocalBenchmarksList listView = new LocalBenchmarksList();
            listView.setListAdapter(new LocalBenchmarksListAdapter(LayoutInflater.from(this)));
            listView.setBenchmarks(this.mBenchmarksToRun);
            listView.setRunId(this.mCurrentRunId);
            fm.beginTransaction().add(R.id.list_fragment_container, listView).commit();
        }

        final TextView scoreView = this.findViewById(R.id.score_text_view);
        scoreView.setText("Running tests!");
    }

    private int translateBenchmarkIndex(final int index) {
        if (0 <= index && index < RunLocalBenchmarksActivity.ALL_TESTS.length) {
            return RunLocalBenchmarksActivity.ALL_TESTS[index];
        }

        return -1;
    }

    private void initLocalBenchmarks(final Intent intent) {
        this.mBenchmarksToRun = new ArrayList<>();
        int[] enabledIds = intent.getIntArrayExtra(BenchmarkGroup.BENCHMARK_EXTRA_ENABLED_TESTS);
        final int runCount = intent.getIntExtra(BenchmarkGroup.BENCHMARK_EXTRA_RUN_COUNT, RunLocalBenchmarksActivity.RUN_COUNT);
        this.mFinish = intent.getBooleanExtra(BenchmarkGroup.BENCHMARK_EXTRA_FINISH, false);

        if (null == enabledIds) {
            // run all tests
            enabledIds = RunLocalBenchmarksActivity.ALL_TESTS;
        }

        final StringBuilder idString = new StringBuilder();
        idString.append(runCount);
        idString.append(System.currentTimeMillis());

        for (int i = 0; i < enabledIds.length; i++) {
            int id = enabledIds[i];
            System.out.println("considering " + id);
            if (!RunLocalBenchmarksActivity.isValidBenchmark(id)) {
                System.out.println("not valid " + id);
                id = this.translateBenchmarkIndex(id);
                System.out.println("got out " + id);
                System.out.println("expected: " + R.id.benchmark_overdraw);
            }

            if (RunLocalBenchmarksActivity.isValidBenchmark(id)) {
                int localRunCount = runCount;
                if (this.isCompute(id)) {
                    localRunCount = 1;
                }
                this.mBenchmarksToRun.add(new LocalBenchmark(id, localRunCount));
                idString.append(id);
            }
        }

        this.mBenchmarkCursor = 0;
        this.mCurrentRunId = idString.toString().hashCode();
    }

    private boolean isCompute(final int id) {
        return id == R.id.benchmark_cpu_gflops || id == R.id.benchmark_cpu_heat_soak || id == R.id.benchmark_memory_bandwidth || id == R.id.benchmark_memory_latency || id == R.id.benchmark_power_management;
    }

    private static boolean isValidBenchmark(final int benchmarkId) {
        return benchmarkId == R.id.benchmark_cpu_gflops || benchmarkId == R.id.benchmark_cpu_heat_soak || benchmarkId == R.id.benchmark_memory_bandwidth || benchmarkId == R.id.benchmark_memory_latency || benchmarkId == R.id.benchmark_power_management;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RunLocalBenchmarksActivity.this.runNextBenchmark();
            }
        }, 1000);
    }

    private void computeOverallScore() {
        TextView scoreView = this.findViewById(R.id.score_text_view);
        scoreView.setText("Computing score...");
        new AsyncTask<Void, Void, Integer>()  {
            @Override
            protected Integer doInBackground(final Void... voids) {
                final GlobalResultsStore gsr =
                        GlobalResultsStore.getInstance(RunLocalBenchmarksActivity.this);
                final ArrayList<Double> testLevelScores = new ArrayList<>();
                SummaryStatistics stats = new SummaryStatistics();
                for (final LocalBenchmark b : RunLocalBenchmarksActivity.this.mBenchmarksToRun) {
                    final HashMap<String, ArrayList<UiBenchmarkResult>> detailedResults =
                            gsr.loadDetailedResults(RunLocalBenchmarksActivity.this.mCurrentRunId);
                    for (final ArrayList<UiBenchmarkResult> testResult : detailedResults.values()) {
                        for (final UiBenchmarkResult res : testResult) {
                            int score = res.getScore();
                            if (0 == score) {
                                score = 1;
                            }
                            stats.addValue(score);
                        }

                        testLevelScores.add(stats.getGeometricMean());
                        stats.clear();
                    }

                }

                for (final double score : testLevelScores) {
                    stats.addValue(score);
                }

                return (int)Math.round(stats.getGeometricMean());
            }

            @Override
            protected void onPostExecute(final Integer score) {
                final TextView view = findViewById(R.id.score_text_view);
                view.setText("Score: " + score);
            }
        }.execute();
    }

    private void runNextBenchmark() {
        LocalBenchmark benchmark = this.mBenchmarksToRun.get(this.mBenchmarkCursor);
        final boolean runAgain = false;

        if (benchmark.runCount < benchmark.totalCount) {
            this.runBenchmarkForId(this.mBenchmarksToRun.get(this.mBenchmarkCursor).id, benchmark.runCount);
            benchmark.runCount++;
        } else if (this.mBenchmarkCursor + 1 < this.mBenchmarksToRun.size()) {
            this.mBenchmarkCursor++;
            benchmark = this.mBenchmarksToRun.get(this.mBenchmarkCursor);
            this.runBenchmarkForId(benchmark.id, benchmark.runCount);
            benchmark.runCount++;
        } else if (runAgain) {
            this.mBenchmarkCursor = 0;
            this.initLocalBenchmarks(this.getIntent());

            this.runBenchmarkForId(this.mBenchmarksToRun.get(this.mBenchmarkCursor).id, benchmark.runCount);
        } else if (this.mFinish) {
            this.finish();
        } else {
            Log.i("BENCH", "BenchmarkDone!");
            this.computeOverallScore();

            new AsyncTask<Void, Void, Void>() {
                boolean success;

                @Override
                protected Void doInBackground(final Void... voids) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RunLocalBenchmarksActivity.this, "Uploading results...", Toast.LENGTH_LONG).show();
                        }
                    });

                    this.success = JankBenchAPI.uploadResults(RunLocalBenchmarksActivity.this, Constants.BASE_URL);

                    return null;
                }

                @Override
                protected void onPostExecute(final Void aVoid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RunLocalBenchmarksActivity.this, success ? "Upload succeeded" : "Upload failed", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }.execute();
        }
    }

    private void runBenchmarkForId(final int id, final int iteration) {
        final Intent intent;
        int syntheticTestId = -1;

        System.out.println("iteration: " + iteration);

        if (id == R.id.benchmark_list_view_scroll) {
            intent = new Intent(this.getApplicationContext(), ListViewScrollActivity.class);
        } else if (id == R.id.benchmark_image_list_view_scroll) {
            intent = new Intent(this.getApplicationContext(), ImageListViewScrollActivity.class);
        } else if (id == R.id.benchmark_shadow_grid) {
            intent = new Intent(this.getApplicationContext(), ShadowGridActivity.class);
        } else if (id == R.id.benchmark_text_high_hitrate) {
            intent = new Intent(this.getApplicationContext(), TextScrollActivity.class);
            intent.putExtra(TextScrollActivity.EXTRA_HIT_RATE, 80);
            intent.putExtra(BenchmarkRegistry.EXTRA_ID, id);
        } else if (id == R.id.benchmark_text_low_hitrate) {
            intent = new Intent(this.getApplicationContext(), TextScrollActivity.class);
            intent.putExtra(TextScrollActivity.EXTRA_HIT_RATE, 20);
            intent.putExtra(BenchmarkRegistry.EXTRA_ID, id);
        } else if (id == R.id.benchmark_edit_text_input) {
            intent = new Intent(this.getApplicationContext(), EditTextInputActivity.class);
        } else if (id == R.id.benchmark_overdraw) {
            intent = new Intent(this.getApplicationContext(), FullScreenOverdrawActivity.class);
        } else if (id == R.id.benchmark_bitmap_upload) {
            intent = new Intent(this.getApplicationContext(), BitmapUploadActivity.class);
        } else if (id == R.id.benchmark_memory_bandwidth) {
            syntheticTestId = 0;
            intent = new Intent(this.getApplicationContext(), MemoryActivity.class);
            intent.putExtra("test", syntheticTestId);
        } else if (id == R.id.benchmark_memory_latency) {
            syntheticTestId = 1;
            intent = new Intent(this.getApplicationContext(), MemoryActivity.class);
            intent.putExtra("test", syntheticTestId);
        } else if (id == R.id.benchmark_power_management) {
            syntheticTestId = 2;
            intent = new Intent(this.getApplicationContext(), MemoryActivity.class);
            intent.putExtra("test", syntheticTestId);
        } else if (id == R.id.benchmark_cpu_heat_soak) {
            syntheticTestId = 3;
            intent = new Intent(this.getApplicationContext(), MemoryActivity.class);
            intent.putExtra("test", syntheticTestId);
        } else if (id == R.id.benchmark_cpu_gflops) {
            syntheticTestId = 4;
            intent = new Intent(this.getApplicationContext(), MemoryActivity.class);
            intent.putExtra("test", syntheticTestId);
        } else {
            intent = null;
        }

        if (null != intent) {
            intent.putExtra("com.android.benchmark.RUN_ID", this.mCurrentRunId);
            intent.putExtra("com.android.benchmark.ITERATION", iteration);
            this.startActivityForResult(intent, id & 0xffff, null);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == R.id.benchmark_shadow_grid || requestCode == R.id.benchmark_list_view_scroll || requestCode == R.id.benchmark_image_list_view_scroll || requestCode == R.id.benchmark_text_high_hitrate || requestCode == R.id.benchmark_text_low_hitrate || requestCode == R.id.benchmark_edit_text_input) {
            // Do something
        } else {
            // Do something else
        }
    }

    class LocalBenchmarksListAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;

        LocalBenchmarksListAdapter(final LayoutInflater inflater) {
            this.mInflater = inflater;
        }

        @Override
        public int getCount() {
            return RunLocalBenchmarksActivity.this.mBenchmarksToRun.size();
        }

        @Override
        public Object getItem(final int i) {
            return RunLocalBenchmarksActivity.this.mBenchmarksToRun.get(i);
        }

        @Override
        public long getItemId(final int i) {
            return RunLocalBenchmarksActivity.this.mBenchmarksToRun.get(i).id;
        }

        @Override
        public View getView(final int i, View convertView, final ViewGroup parent) {
            if (null == convertView) {
                convertView = this.mInflater.inflate(R.layout.running_benchmark_list_item, null);
            }

            final TextView name = convertView.findViewById(R.id.benchmark_name);
            name.setText(BenchmarkRegistry.getBenchmarkName(
                    RunLocalBenchmarksActivity.this, RunLocalBenchmarksActivity.this.mBenchmarksToRun.get(i).id));
            return convertView;
        }

    }
}

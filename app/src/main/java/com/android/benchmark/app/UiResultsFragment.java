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

package com.android.benchmark.app;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import android.util.Log;
import android.view.FrameMetrics;
import android.widget.SimpleAdapter;

import com.android.benchmark.R;
import com.android.benchmark.registry.BenchmarkGroup;
import com.android.benchmark.registry.BenchmarkRegistry;
import com.android.benchmark.results.GlobalResultsStore;
import com.android.benchmark.results.UiBenchmarkResult;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@TargetApi(24)
public class UiResultsFragment extends ListFragment {
    private static final String TAG = "UiResultsFragment";
    private static final int NUM_FIELDS = 20;

    private ArrayList<UiBenchmarkResult> mResults = new ArrayList<>();

    private final AsyncTask<Void, Void, ArrayList<Map<String, String>>> mLoadScoresTask =
            new AsyncTask<Void, Void, ArrayList<Map<String, String>>>() {
        @NonNull
        @Override
        protected ArrayList<Map<String, String>> doInBackground(final Void... voids) {
            final String[] data;
            if (0 == mResults.size() || null == mResults.get(0)) {
                data = new String[] {
                        "No metrics reported", ""
                };
            } else {
                data = new String[UiResultsFragment.NUM_FIELDS * (1 + UiResultsFragment.this.mResults.size()) + 2];
                final SummaryStatistics stats = new SummaryStatistics();
                int totalFrameCount = 0;
                double totalAvgFrameDuration = 0;
                double total99FrameDuration = 0;
                double total95FrameDuration = 0;
                double total90FrameDuration = 0;
                double totalLongestFrame = 0;
                double totalShortestFrame = 0;

                for (int i = 0; i < UiResultsFragment.this.mResults.size(); i++) {
                    int start = (i * UiResultsFragment.NUM_FIELDS) + +UiResultsFragment.NUM_FIELDS;
                    data[(start)] = "Iteration";
                    start++;
                    data[(start)] = String.valueOf(i);
                    start++;
                    data[(start)] = "Total Frames";
                    start++;
                    final int currentFrameCount = UiResultsFragment.this.mResults.get(i).getTotalFrameCount();
                    totalFrameCount += currentFrameCount;
                    data[(start)] = Integer.toString(currentFrameCount);
                    start++;
                    data[(start)] = "Average frame duration:";
                    start++;
                    final double currentAvgFrameDuration = UiResultsFragment.this.mResults.get(i).getAverage(FrameMetrics.TOTAL_DURATION);
                    totalAvgFrameDuration += currentAvgFrameDuration;
                    data[(start)] = String.format("%.2f", currentAvgFrameDuration);
                    start++;
                    data[(start)] = "Frame duration 99th:";
                    start++;
                    final double current99FrameDuration = UiResultsFragment.this.mResults.get(i).getPercentile(FrameMetrics.TOTAL_DURATION, 99);
                    total99FrameDuration += current99FrameDuration;
                    data[(start)] = String.format("%.2f", current99FrameDuration);
                    start++;
                    data[(start)] = "Frame duration 95th:";
                    start++;
                    final double current95FrameDuration = UiResultsFragment.this.mResults.get(i).getPercentile(FrameMetrics.TOTAL_DURATION, 95);
                    total95FrameDuration += current95FrameDuration;
                    data[(start)] = String.format("%.2f", current95FrameDuration);
                    start++;
                    data[(start)] = "Frame duration 90th:";
                    start++;
                    final double current90FrameDuration = UiResultsFragment.this.mResults.get(i).getPercentile(FrameMetrics.TOTAL_DURATION, 90);
                    total90FrameDuration += current90FrameDuration;
                    data[(start)] = String.format("%.2f", current90FrameDuration);
                    start++;
                    data[(start)] = "Longest frame:";
                    start++;
                    final double longestFrame = UiResultsFragment.this.mResults.get(i).getMaximum(FrameMetrics.TOTAL_DURATION);
                    if (0 == totalLongestFrame || longestFrame > totalLongestFrame) {
                        totalLongestFrame = longestFrame;
                    }
                    data[(start)] = String.format("%.2f", longestFrame);
                    start++;
                    data[(start)] = "Shortest frame:";
                    start++;
                    final double shortestFrame = UiResultsFragment.this.mResults.get(i).getMinimum(FrameMetrics.TOTAL_DURATION);
                    if (0 == totalShortestFrame || totalShortestFrame > shortestFrame) {
                        totalShortestFrame = shortestFrame;
                    }
                    data[(start)] = String.format("%.2f", shortestFrame);
                    start++;
                    data[(start)] = "Score:";
                    start++;
                    final double score = UiResultsFragment.this.mResults.get(i).getScore();
                    stats.addValue(score);
                    data[(start)] = String.format("%.2f", score);
                    start++;
                    data[(start)] = "==============";
                    start++;
                    data[(start)] = "============================";
                    start++;
                }

                int start = 0;
                data[0] = "Overall: ";
                data[1] = "";
                data[(start)] = "Total Frames";
                start++;
                data[(start)] = Integer.toString(totalFrameCount);
                start++;
                data[(start)] = "Average frame duration:";
                start++;
                data[(start)] = String.format("%.2f", totalAvgFrameDuration / UiResultsFragment.this.mResults.size());
                start++;
                data[(start)] = "Frame duration 99th:";
                start++;
                data[(start)] = String.format("%.2f", total99FrameDuration / UiResultsFragment.this.mResults.size());
                start++;
                data[(start)] = "Frame duration 95th:";
                start++;
                data[(start)] = String.format("%.2f", total95FrameDuration / UiResultsFragment.this.mResults.size());
                start++;
                data[(start)] = "Frame duration 90th:";
                start++;
                data[(start)] = String.format("%.2f", total90FrameDuration / UiResultsFragment.this.mResults.size());
                start++;
                data[(start)] = "Longest frame:";
                start++;
                data[(start)] = String.format("%.2f", totalLongestFrame);
                start++;
                data[(start)] = "Shortest frame:";
                start++;
                data[(start)] = String.format("%.2f", totalShortestFrame);
                start++;
                data[(start)] = "Score:";
                start++;
                data[(start)] = String.format("%.2f", stats.getGeometricMean());
                start++;
                data[(start)] = "==============";
                start++;
                data[(start)] = "============================";
                start++;
            }

            final ArrayList<Map<String, String>> dataMap = new ArrayList<>();
            for (int i = 0; i < data.length - 1; i += 2) {
                final HashMap<String, String> map = new HashMap<>();
                map.put("name", data[i]);
                map.put("value", data[i + 1]);
                dataMap.add(map);
            }

            return dataMap;
        }

        @Override
        protected void onPostExecute(final ArrayList<Map<String, String>> dataMap) {
            UiResultsFragment.this.setListAdapter(new SimpleAdapter(UiResultsFragment.this.getActivity(), dataMap, R.layout.results_list_item,
                    new String[] {"name", "value"}, new int[] { R.id.result_name, R.id.result_value }));
            UiResultsFragment.this.setListShown(true);
        }
    };

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListShown(false);
        this.mLoadScoresTask.execute();
    }

    public void setRunInfo(final String name, final int runId) {
        this.mResults = GlobalResultsStore.getInstance(this.getActivity()).loadTestResults(name, runId);
    }
}

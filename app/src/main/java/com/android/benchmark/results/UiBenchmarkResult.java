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

package com.android.benchmark.results;

import android.annotation.TargetApi;
import android.view.FrameMetrics;

import androidx.annotation.NonNull;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for storing and analyzing UI benchmark results.
 */
@TargetApi(24)
public class UiBenchmarkResult {
    private final int BASE_SCORE = 100;
    private final int CONSISTENCY_BONUS_MAX = 100;

    private int FRAME_PERIOD_MS = 16;
    private int ZERO_SCORE_TOTAL_DURATION_MS = 2 * this.FRAME_PERIOD_MS;
    private int JANK_PENALTY_THRESHOLD_MS = (int) Math.floor(0.75 * this.FRAME_PERIOD_MS);
    private int ZERO_SCORE_ABOVE_THRESHOLD_MS =
            this.ZERO_SCORE_TOTAL_DURATION_MS - this.JANK_PENALTY_THRESHOLD_MS;
    private double JANK_PENALTY_PER_MS_ABOVE_THRESHOLD =
            this.BASE_SCORE / (double) this.ZERO_SCORE_ABOVE_THRESHOLD_MS;

    private static final int METRIC_WAS_JANKY = -1;

    private static final int[] METRICS = {
            FrameMetrics.UNKNOWN_DELAY_DURATION,
            FrameMetrics.INPUT_HANDLING_DURATION,
            FrameMetrics.ANIMATION_DURATION,
            FrameMetrics.LAYOUT_MEASURE_DURATION,
            FrameMetrics.DRAW_DURATION,
            FrameMetrics.SYNC_DURATION,
            FrameMetrics.COMMAND_ISSUE_DURATION,
            FrameMetrics.SWAP_BUFFERS_DURATION,
            FrameMetrics.TOTAL_DURATION,
    };

    @NonNull
    private final DescriptiveStatistics[] mStoredStatistics;

    public UiBenchmarkResult(@NonNull final List<FrameMetrics> instances, final int refresh_rate) {
        this.initializeThresholds(refresh_rate);
        this.mStoredStatistics = new DescriptiveStatistics[UiBenchmarkResult.METRICS.length];
        this.insertMetrics(instances);
    }

    public UiBenchmarkResult(@NonNull final double[] values, final int refresh_rate) {
        this.initializeThresholds(refresh_rate);
        this.mStoredStatistics = new DescriptiveStatistics[UiBenchmarkResult.METRICS.length];
        this.insertValues(values);
    }

    // Dynamically set threshold values based on display refresh rate
    private void initializeThresholds(final int refresh_rate) {
        this.FRAME_PERIOD_MS = Math.floorDiv(1000, refresh_rate);
        this.ZERO_SCORE_TOTAL_DURATION_MS = this.FRAME_PERIOD_MS * 2;
        this.JANK_PENALTY_THRESHOLD_MS = (int) Math.floor(0.75 * this.FRAME_PERIOD_MS);
        this.ZERO_SCORE_ABOVE_THRESHOLD_MS = this.ZERO_SCORE_TOTAL_DURATION_MS - this.JANK_PENALTY_THRESHOLD_MS;
        this.JANK_PENALTY_PER_MS_ABOVE_THRESHOLD = this.BASE_SCORE / (double) this.ZERO_SCORE_ABOVE_THRESHOLD_MS;
    }

    public void update(@NonNull final List<FrameMetrics> instances) {
        this.insertMetrics(instances);
    }

    public void update(@NonNull final double[] values) {
        this.insertValues(values);
    }

    public double getAverage(final int id) {
        final int pos = this.getMetricPosition(id);
        return this.mStoredStatistics[pos].getMean();
    }

    public double getMinimum(final int id) {
        final int pos = this.getMetricPosition(id);
        return this.mStoredStatistics[pos].getMin();
    }

    public double getMaximum(final int id) {
        final int pos = this.getMetricPosition(id);
        return this.mStoredStatistics[pos].getMax();
    }

    public int getMaximumIndex(final int id) {
        final int pos = this.getMetricPosition(id);
        final double[] storedMetrics = this.mStoredStatistics[pos].getValues();
        int maxIdx = 0;
        for (int i = 0; i < storedMetrics.length; i++) {
            if (storedMetrics[i] >= storedMetrics[maxIdx]) {
                maxIdx = i;
            }
        }

        return maxIdx;
    }

    public double getMetricAtIndex(final int index, final int metricId) {
        return this.mStoredStatistics[this.getMetricPosition(metricId)].getElement(index);
    }

    public double getPercentile(final int id, int percentile) {
        if (100 < percentile) percentile = 100;
        if (0 > percentile) percentile = 0;

        final int metricPos = this.getMetricPosition(id);
        return this.mStoredStatistics[metricPos].getPercentile(percentile);
    }

    public int getTotalFrameCount() {
        if (0 == mStoredStatistics.length) {
            return 0;
        }

        return (int) this.mStoredStatistics[0].getN();
    }

    public int getScore() {
        final SummaryStatistics badFramesStats = new SummaryStatistics();

        final int totalFrameCount = this.getTotalFrameCount();
        for (int i = 0; i < totalFrameCount; i++) {
            final double totalDuration = this.getMetricAtIndex(i, FrameMetrics.TOTAL_DURATION);
            if (totalDuration >= this.JANK_PENALTY_THRESHOLD_MS) {
                badFramesStats.addValue(totalDuration);
            }
        }

        final int length = this.getSortedJankFrameIndices().length;
        final double jankFrameCount = 100 * length / (double) totalFrameCount;

        System.out.println("Mean: " + badFramesStats.getMean() + " JankP: " + jankFrameCount
                + " StdDev: " + badFramesStats.getStandardDeviation() +
                " Count Bad: " + badFramesStats.getN() + " Count Jank: " + length);

        return (int) Math.round(
                (badFramesStats.getMean()) * jankFrameCount * badFramesStats.getStandardDeviation());
    }

    public int getNumJankFrames() {
        return this.getSortedJankFrameIndices().length;
    }

    public int getNumBadFrames() {
        int num_bad_frames = 0;
        final int totalFrameCount = this.getTotalFrameCount();
        for (int i = 0; i < totalFrameCount; i++) {
            final double totalDuration = this.getMetricAtIndex(i, FrameMetrics.TOTAL_DURATION);
            if (totalDuration >= this.JANK_PENALTY_THRESHOLD_MS) {
                num_bad_frames++;
            }
        }

        return num_bad_frames;
    }


    public int getJankPenalty() {
        final double total95th = this.mStoredStatistics[this.getMetricPosition(FrameMetrics.TOTAL_DURATION)]
                .getPercentile(95);
        System.out.println("95: " + total95th);
        final double aboveThreshold = total95th - this.JANK_PENALTY_THRESHOLD_MS;
        if (0 >= aboveThreshold) {
            return 0;
        }

        if (aboveThreshold > this.ZERO_SCORE_ABOVE_THRESHOLD_MS) {
            return this.BASE_SCORE;
        }

        return (int) Math.ceil(this.JANK_PENALTY_PER_MS_ABOVE_THRESHOLD * aboveThreshold);
    }

    public int getConsistencyBonus() {
        final DescriptiveStatistics totalDurationStats =
                this.mStoredStatistics[this.getMetricPosition(FrameMetrics.TOTAL_DURATION)];

        final double standardDeviation = totalDurationStats.getStandardDeviation();
        if (0 == standardDeviation) {
            return this.CONSISTENCY_BONUS_MAX;
        }

        // 1 / CV of the total duration.
        final double bonus = totalDurationStats.getMean() / standardDeviation;
        return (int) Math.min(Math.round(bonus), this.CONSISTENCY_BONUS_MAX);
    }

    public int[] getSortedJankFrameIndices() {
        final ArrayList<Integer> jankFrameIndices = new ArrayList<>();
        boolean tripleBuffered = false;
        final int totalFrameCount = this.getTotalFrameCount();
        final int totalDurationPos = this.getMetricPosition(FrameMetrics.TOTAL_DURATION);

        for (int i = 0; i < totalFrameCount; i++) {
            final double thisDuration = this.mStoredStatistics[totalDurationPos].getElement(i);
            if (!tripleBuffered) {
                if (thisDuration > this.FRAME_PERIOD_MS) {
                    tripleBuffered = true;
                    jankFrameIndices.add(i);
                }
            } else {
                if (thisDuration > 2 * this.FRAME_PERIOD_MS) {
                    tripleBuffered = false;
                    jankFrameIndices.add(i);
                }
            }
        }

        final int[] res = new int[jankFrameIndices.size()];
        int i = 0;
        for (final Integer index : jankFrameIndices) {
            res[i] = index;
            i++;
        }
        return res;
    }

    private int getMetricPosition(final int id) {
        for (int i = 0; i < UiBenchmarkResult.METRICS.length; i++) {
            if (id == UiBenchmarkResult.METRICS[i]) {
                return i;
            }
        }

        return -1;
    }

    private void insertMetrics(@NonNull final List<FrameMetrics> instances) {
        for (final FrameMetrics frame : instances) {
            for (int i = 0; i < UiBenchmarkResult.METRICS.length; i++) {
                DescriptiveStatistics stats = this.mStoredStatistics[i];
                if (null == stats) {
                    stats = new DescriptiveStatistics();
                    this.mStoredStatistics[i] = stats;
                }

                this.mStoredStatistics[i].addValue(frame.getMetric(UiBenchmarkResult.METRICS[i]) / (double) 1000000);
            }
        }
    }

    private void insertValues(@NonNull final double[] values) {
        if (values.length != UiBenchmarkResult.METRICS.length) {
            throw new IllegalArgumentException("invalid values array");
        }

        for (int i = 0; i < values.length; i++) {
            DescriptiveStatistics stats = this.mStoredStatistics[i];
            if (null == stats) {
                stats = new DescriptiveStatistics();
                this.mStoredStatistics[i] = stats;
            }

            this.mStoredStatistics[i].addValue(values[i]);
        }
    }
 }

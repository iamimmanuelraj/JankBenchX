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

package com.android.benchmark.registry;

import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Logical grouping of benchmarks
 */
public class BenchmarkGroup {
    public static final String BENCHMARK_EXTRA_ENABLED_TESTS =
            "com.android.benchmark.EXTRA_ENABLED_BENCHMARK_IDS";

    public static final String BENCHMARK_EXTRA_RUN_COUNT =
            "com.android.benchmark.EXTRA_RUN_COUNT";
    public static final String BENCHMARK_EXTRA_FINISH = "com.android.benchmark.FINISH_WHEN_DONE";

    public static class Benchmark implements View.OnClickListener {
        /** The name of this individual benchmark test */
        private final String mName;

        /** The category of this individual benchmark test */
        @BenchmarkCategory
        private final int mCategory;

        /** Human-readable description of the benchmark */
        private final String mDescription;

        private final int mId;

        private boolean mEnabled;

        Benchmark(final int id, final String name, @BenchmarkCategory final int category, final String description) {
            this.mId = id;
            this.mName = name;
            this.mCategory = category;
            this.mDescription = description;
            this.mEnabled = true;
        }

        public boolean isEnabled() { return this.mEnabled; }

        public void setEnabled(final boolean enabled) {
            this.mEnabled = enabled; }

        public int getId() { return this.mId; }

        public String getDescription() { return this.mDescription; }

        @BenchmarkCategory
        public int getCategory() { return this.mCategory; }

        public String getName() { return this.mName; }

        @Override
        public void onClick(@NonNull final View view) {
            mEnabled = ((CheckBox) view).isChecked();
        }
    }

    /**
     * Component for this benchmark group.
     */
    private final ComponentName mComponentName;

    /**
     * Benchmark title, showed in the {@link android.widget.ListView}
     */
    private final String mTitle;

    /**
     * List of all benchmarks exported by this group
     */
    private final Benchmark[] mBenchmarks;

    /**
     * The intent to launch the benchmark
     */
    private final Intent mIntent;

    /** Human-readable description of the benchmark group */
    private final String mDescription;

    BenchmarkGroup(final ComponentName componentName, final String title,
                   final String description, final Benchmark[] benchmarks, final Intent intent) {
        this.mComponentName = componentName;
        this.mTitle = title;
        this.mBenchmarks = benchmarks;
        this.mDescription = description;
        this.mIntent = intent;
    }

    @Nullable
    public Intent getIntent() {
        final int[] enabledBenchmarksIds = this.getEnabledBenchmarksIds();
        if (0 != enabledBenchmarksIds.length) {
            this.mIntent.putExtra(BenchmarkGroup.BENCHMARK_EXTRA_ENABLED_TESTS, enabledBenchmarksIds);
            return this.mIntent;
        }

        return null;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public Benchmark[] getBenchmarks() {
        return this.mBenchmarks;
    }

    public String getDescription() {
        return this.mDescription;
    }

    private int[] getEnabledBenchmarksIds() {
        int enabledBenchmarkCount = 0;
        for (int i = 0; i < this.mBenchmarks.length; i++) {
            if (this.mBenchmarks[i].isEnabled()) {
                enabledBenchmarkCount++;
            }
        }

        int writeIndex = 0;
        final int[] enabledBenchmarks = new int[enabledBenchmarkCount];
        for (int i = 0; i < this.mBenchmarks.length; i++) {
            if (this.mBenchmarks[i].isEnabled()) {
                enabledBenchmarks[writeIndex] = this.mBenchmarks[i].getId();
                writeIndex++;
            }
        }

        return enabledBenchmarks;
    }
}

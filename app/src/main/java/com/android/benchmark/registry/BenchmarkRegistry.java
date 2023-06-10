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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.Xml;

import com.android.benchmark.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class BenchmarkRegistry {

    /** Metadata key for benchmark XML data */
    private static final String BENCHMARK_GROUP_META_KEY =
            "com.android.benchmark.benchmark_group";

    /** Intent action specifying an activity that runs a single benchmark test. */
    private static final String ACTION_BENCHMARK = "com.android.benchmark.ACTION_BENCHMARK";
    public static final String EXTRA_ID = "com.android.benchmark.EXTRA_ID";

    private static final String TAG_BENCHMARK_GROUP = "com.android.benchmark.BenchmarkGroup";
    private static final String TAG_BENCHMARK = "com.android.benchmark.Benchmark";

    private final List<BenchmarkGroup> mGroups;

    private final Context mContext;

    public BenchmarkRegistry(final Context context) {
        this.mContext = context;
        this.mGroups = new ArrayList<>();
        this.loadBenchmarks();
    }

    private Intent getIntentFromInfo(final ActivityInfo inf) {
        final Intent intent = new Intent();
        intent.setClassName(inf.packageName, inf.name);
        return intent;
    }

    public void loadBenchmarks() {
        final Intent intent = new Intent(BenchmarkRegistry.ACTION_BENCHMARK);
        intent.setPackage(this.mContext.getPackageName());

        final PackageManager pm = this.mContext.getPackageManager();
        final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent,
                PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);

        for (final ResolveInfo inf : resolveInfos) {
            final List<BenchmarkGroup> groups = this.parseBenchmarkGroup(inf.activityInfo);
            if (null != groups) {
                this.mGroups.addAll(groups);
            }
        }
    }

    private boolean seekToTag(final XmlPullParser parser, final String tag)
            throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (XmlPullParser.START_TAG != eventType && XmlPullParser.END_DOCUMENT != eventType) {
            eventType = parser.next();
        }
        return XmlPullParser.END_DOCUMENT != eventType && tag.equals(parser.getName());
    }

    @BenchmarkCategory int getCategory(final int category) {
        if (BenchmarkCategory.COMPUTE == category) {
            return BenchmarkCategory.COMPUTE;
        } else if (BenchmarkCategory.UI == category) {
            return BenchmarkCategory.UI;
        } else {
            return BenchmarkCategory.GENERIC;
        }
    }

    private List<BenchmarkGroup> parseBenchmarkGroup(final ActivityInfo activityInfo) {
        final PackageManager pm = this.mContext.getPackageManager();

        final ComponentName componentName = new ComponentName(
                activityInfo.packageName, activityInfo.name);

        final SparseArray<List<BenchmarkGroup.Benchmark>> benchmarks = new SparseArray<>();
        final String groupName;
        final String groupDescription;
        try (final XmlResourceParser parser = activityInfo.loadXmlMetaData(pm, BenchmarkRegistry.BENCHMARK_GROUP_META_KEY)) {

            if (!this.seekToTag(parser, BenchmarkRegistry.TAG_BENCHMARK_GROUP)) {
                return null;
            }

            final Resources res = pm.getResourcesForActivity(componentName);
            final AttributeSet attributeSet = Xml.asAttributeSet(parser);
            final TypedArray groupAttribs = res.obtainAttributes(attributeSet, R.styleable.BenchmarkGroup);

            groupName = groupAttribs.getString(R.styleable.BenchmarkGroup_name);
            groupDescription = groupAttribs.getString(R.styleable.BenchmarkGroup_description);
            groupAttribs.recycle();
            parser.next();

            while (this.seekToTag(parser, BenchmarkRegistry.TAG_BENCHMARK)) {
                final TypedArray benchAttribs =
                        res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Benchmark);
                final int id = benchAttribs.getResourceId(R.styleable.Benchmark_id, -1);
                final String testName = benchAttribs.getString(R.styleable.Benchmark_name);
                final String testDescription = benchAttribs.getString(R.styleable.Benchmark_description);
                final int testCategory = benchAttribs.getInt(R.styleable.Benchmark_category,
                        BenchmarkCategory.GENERIC);
                final int category = this.getCategory(testCategory);
                final BenchmarkGroup.Benchmark benchmark = new BenchmarkGroup.Benchmark(
                        id, testName, category, testDescription);
                List<BenchmarkGroup.Benchmark> benches = benchmarks.get(category);
                if (null == benches) {
                    benches = new ArrayList<>();
                    benchmarks.append(category, benches);
                }

                benches.add(benchmark);

                benchAttribs.recycle();
                parser.next();
            }
        } catch (final PackageManager.NameNotFoundException | XmlPullParserException | IOException e) {
            return null;
        }

        final List<BenchmarkGroup> result = new ArrayList<>();
        final Intent testIntent = this.getIntentFromInfo(activityInfo);
        for (int i = 0; i < benchmarks.size(); i++) {
            final int cat = benchmarks.keyAt(i);
            final List<BenchmarkGroup.Benchmark> thisGroup = benchmarks.get(cat);
            final BenchmarkGroup.Benchmark[] benchmarkArray =
                    new BenchmarkGroup.Benchmark[thisGroup.size()];
            thisGroup.toArray(benchmarkArray);
            result.add(new BenchmarkGroup(componentName,
                    groupName + " - " + BenchmarkRegistry.getCategoryString(cat), groupDescription, benchmarkArray,
                    testIntent));
        }

        return result;
    }

    public int getGroupCount() {
        return this.mGroups.size();
    }

    public int getBenchmarkCount(final int benchmarkIndex) {
        final BenchmarkGroup group = this.getBenchmarkGroup(benchmarkIndex);
        if (null != group) {
            return group.getBenchmarks().length;
        }
        return 0;
    }

    public BenchmarkGroup getBenchmarkGroup(final int benchmarkIndex) {
        if (benchmarkIndex >= this.mGroups.size()) {
            return null;
        }

        return this.mGroups.get(benchmarkIndex);
    }

    public static String getCategoryString(final int category) {
        if (BenchmarkCategory.COMPUTE == category) {
            return "Compute";
        } else if (BenchmarkCategory.UI == category) {
            return "UI";
        } else if (BenchmarkCategory.GENERIC == category) {
            return "Generic";
        } else {
            return "";
        }
    }

    public static String getBenchmarkName(final Context context, final int benchmarkId) {
        if (benchmarkId == R.id.benchmark_list_view_scroll) {
            return context.getString(R.string.list_view_scroll_name);
        } else if (benchmarkId == R.id.benchmark_image_list_view_scroll) {
            return context.getString(R.string.image_list_view_scroll_name);
        } else if (benchmarkId == R.id.benchmark_shadow_grid) {
            return context.getString(R.string.shadow_grid_name);
        } else if (benchmarkId == R.id.benchmark_text_high_hitrate) {
            return context.getString(R.string.text_high_hitrate_name);
        } else if (benchmarkId == R.id.benchmark_text_low_hitrate) {
            return context.getString(R.string.text_low_hitrate_name);
        } else if (benchmarkId == R.id.benchmark_edit_text_input) {
            return context.getString(R.string.edit_text_input_name);
        } else if (benchmarkId == R.id.benchmark_memory_bandwidth) {
            return context.getString(R.string.memory_bandwidth_name);
        } else if (benchmarkId == R.id.benchmark_memory_latency) {
            return context.getString(R.string.memory_latency_name);
        } else if (benchmarkId == R.id.benchmark_power_management) {
            return context.getString(R.string.power_management_name);
        } else if (benchmarkId == R.id.benchmark_cpu_heat_soak) {
            return context.getString(R.string.cpu_heat_soak_name);
        } else if (benchmarkId == R.id.benchmark_cpu_gflops) {
            return context.getString(R.string.cpu_gflops_name);
        } else if (benchmarkId == R.id.benchmark_overdraw) {
            return context.getString(R.string.overdraw_name);
        } else if (benchmarkId == R.id.benchmark_bitmap_upload) {
            return context.getString(R.string.bitmap_upload_name);
        } else {
            return "Some Benchmark";
        }
    }
}

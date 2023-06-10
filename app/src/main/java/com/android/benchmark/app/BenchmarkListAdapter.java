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

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.benchmark.registry.BenchmarkGroup;
import com.android.benchmark.registry.BenchmarkRegistry;
import com.android.benchmark.R;

/**
 *
 */
public class BenchmarkListAdapter extends BaseExpandableListAdapter {

    private final LayoutInflater mInflater;
    private final BenchmarkRegistry mRegistry;

    BenchmarkListAdapter(final LayoutInflater inflater,
                         final BenchmarkRegistry registry) {
        this.mInflater = inflater;
        this.mRegistry = registry;
    }

    @Override
    public int getGroupCount() {
        return this.mRegistry.getGroupCount();
    }

    @Override
    public int getChildrenCount(final int groupPosition) {
        return this.mRegistry.getBenchmarkCount(groupPosition);
    }

    @Override
    public Object getGroup(final int groupPosition) {
        return this.mRegistry.getBenchmarkGroup(groupPosition);
    }

    @Override
    public Object getChild(final int groupPosition, final int childPosition) {
        final BenchmarkGroup benchmarkGroup = this.mRegistry.getBenchmarkGroup(groupPosition);

        if (null != benchmarkGroup) {
           return benchmarkGroup.getBenchmarks()[childPosition];
        }

        return null;
    }

    @Override
    public long getGroupId(final int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(final int groupPosition, final int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, final ViewGroup parent) {
        final BenchmarkGroup group = (BenchmarkGroup) this.getGroup(groupPosition);
        if (null == convertView) {
            convertView = this.mInflater.inflate(R.layout.benchmark_list_group_row, null);
        }

        final TextView title = convertView.findViewById(R.id.group_name);
        title.setTypeface(null, Typeface.BOLD);
        title.setText(group.getTitle());
        return convertView;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
                             View convertView, final ViewGroup parent) {
        final BenchmarkGroup.Benchmark benchmark =
                (BenchmarkGroup.Benchmark) this.getChild(groupPosition, childPosition);
        if (null == convertView) {
            convertView = this.mInflater.inflate(R.layout.benchmark_list_item, null);
        }

        final TextView name = convertView.findViewById(R.id.benchmark_name);
        name.setText(benchmark.getName());
        final CheckBox enabledBox = convertView.findViewById(R.id.benchmark_enable_checkbox);
        enabledBox.setOnClickListener(benchmark);
        enabledBox.setChecked(benchmark.isEnabled());

        return convertView;
    }

    @Override
    public boolean isChildSelectable(final int groupPosition, final int childPosition) {
        return true;
    }

    public int getChildrenHeight() {
        // TODO
        return 1024;
    }
}

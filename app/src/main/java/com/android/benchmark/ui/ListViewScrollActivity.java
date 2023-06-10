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

package com.android.benchmark.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import android.view.FrameMetrics;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import com.android.benchmark.R;
import com.android.benchmark.ui.automation.Automator;
import com.android.benchmark.ui.automation.Interaction;

import java.io.File;
import java.util.List;

public class ListViewScrollActivity extends ListActivityBase {

    private static final int LIST_SIZE = 400;
    private static final int INTERACTION_COUNT = 4;

    @Nullable
    private Automator mAutomator;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int runId = this.getIntent().getIntExtra("com.android.benchmark.RUN_ID", 0);
        int iteration = this.getIntent().getIntExtra("com.android.benchmark.ITERATION", -1);

        final ActionBar actionBar = this.getSupportActionBar();
        if (null != actionBar) {
            actionBar.setTitle(this.getTitle());
        }

        this.mAutomator = new Automator(this.getName(), runId, iteration, this.getWindow(),
                new Automator.AutomateCallback() {
            @Override
            public void onPostAutomate() {
                final Intent result = new Intent();
                ListViewScrollActivity.this.setResult(Activity.RESULT_OK, result);
                ListViewScrollActivity.this.finish();
            }

            @Override
            public void onPostInteraction(final List<FrameMetrics> metrics) {}

            @Override
            public void onAutomate() {
                final FrameLayout v = ListViewScrollActivity.this.findViewById(R.id.list_fragment_container);

                final int[] coordinates = new int[2];
                v.getLocationOnScreen(coordinates);

                final int x = coordinates[0];
                final int y = coordinates[1];

                final float width = v.getWidth();
                final float height = v.getHeight();

                final float middleX = (x + width) / 5;
                final float middleY = (y + height) / 5;

                final Interaction flingUp = Interaction.newFlingUp(middleX, middleY);
                final Interaction flingDown = Interaction.newFlingDown(middleX, middleY);

                for (int i = 0; INTERACTION_COUNT > i; i++) {
                    this.addInteraction(flingUp);
                    this.addInteraction(flingDown);
                }
            }
        });

        this.mAutomator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mAutomator) {
            this.mAutomator.cancel();
            this.mAutomator = null;
        }
    }

    @Override
    protected ListAdapter createListAdapter() {
        return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Utils.buildStringList(ListViewScrollActivity.LIST_SIZE));
    }

    @Override
    protected String getName() {
        return this.getString(R.string.list_view_scroll_name);
    }
}

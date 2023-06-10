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

package com.android.benchmark.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.benchmark.registry.BenchmarkRegistry;
import com.android.benchmark.ui.automation.Automator;
import com.android.benchmark.ui.automation.Interaction;

import java.io.File;

public class TextScrollActivity extends ListActivityBase {

    public static final String EXTRA_HIT_RATE = ".TextScrollActivity.EXTRA_HIT_RATE";

    private static final int PARAGRAPH_COUNT = 200;

    private int mHitPercentage = 100;
    @Nullable
    private Automator mAutomator;
    private String mName;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        this.mHitPercentage = this.getIntent().getIntExtra(TextScrollActivity.EXTRA_HIT_RATE,
                this.mHitPercentage);
        super.onCreate(savedInstanceState);
        int runId = this.getIntent().getIntExtra("com.android.benchmark.RUN_ID", 0);
        int iteration = this.getIntent().getIntExtra("com.android.benchmark.ITERATION", -1);
        int id = this.getIntent().getIntExtra(BenchmarkRegistry.EXTRA_ID, -1);

        if (-1 == id) {
            this.finish();
            return;
        }

        this.mName = BenchmarkRegistry.getBenchmarkName(this, id);

        this.mAutomator = new Automator(mName, runId, iteration, this.getWindow(),
                new Automator.AutomateCallback() {
            @Override
            public void onPostAutomate() {
                final Intent result = new Intent();
                TextScrollActivity.this.setResult(Activity.RESULT_OK, result);
                TextScrollActivity.this.finish();
            }

            @Override
            public void onAutomate() {
                final ListView v = TextScrollActivity.this.findViewById(android.R.id.list);

                final int[] coordinates = new int[2];
                v.getLocationOnScreen(coordinates);

                final int x = coordinates[0];
                final int y = coordinates[1];

                final float width = v.getWidth();
                final float height = v.getHeight();

                final float middleX = (x + width) / 2;
                final float middleY = (y + height) / 2;

                final Interaction flingUp = Interaction.newFlingUp(middleX, middleY);
                final Interaction flingDown = Interaction.newFlingDown(middleX, middleY);

                this.addInteraction(flingUp);
                this.addInteraction(flingDown);
                this.addInteraction(flingUp);
                this.addInteraction(flingDown);
                this.addInteraction(flingUp);
                this.addInteraction(flingDown);
                this.addInteraction(flingUp);
                this.addInteraction(flingDown);
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

    @NonNull
    @Override
    protected ListAdapter createListAdapter() {
        return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Utils.buildParagraphListWithHitPercentage(TextScrollActivity.PARAGRAPH_COUNT, 80));
    }

    @Override
    protected String getName() {
        return this.mName;
    }
}

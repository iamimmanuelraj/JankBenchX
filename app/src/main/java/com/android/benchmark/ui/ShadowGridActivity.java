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
package com.android.benchmark.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.ListFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.benchmark.R;
import com.android.benchmark.ui.automation.Automator;
import com.android.benchmark.ui.automation.Interaction;

public class ShadowGridActivity extends AppCompatActivity {
    private Automator mAutomator;
    public static class MyListFragment extends ListFragment {
	    @Override
	    public void onViewCreated(final View view, final Bundle savedInstanceState) {
		    super.onViewCreated(view, savedInstanceState);
            this.getListView().setDivider(null);
	    }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int runId = this.getIntent().getIntExtra("com.android.benchmark.RUN_ID", 0);
        int iteration = this.getIntent().getIntExtra("com.android.benchmark.ITERATION", -1);

        final FragmentManager fm = this.getSupportFragmentManager();
        if (null == fm.findFragmentById(android.R.id.content)) {
            final ListFragment listFragment = new MyListFragment();

            listFragment.setListAdapter(new ArrayAdapter<>(this,
                    R.layout.card_row, R.id.card_text, Utils.buildStringList(200)));
            fm.beginTransaction().add(android.R.id.content, listFragment).commit();

            final String testName = this.getString(R.string.shadow_grid_name);

            this.mAutomator = new Automator(testName, runId, iteration, this.getWindow(),
                    new Automator.AutomateCallback() {
                @Override
                public void onPostAutomate() {
                    final Intent result = new Intent();
                    ShadowGridActivity.this.setResult(Activity.RESULT_OK, result);
                    ShadowGridActivity.this.finish();
                }

                @Override
                public void onAutomate() {
                    final ListView v = ShadowGridActivity.this.findViewById(android.R.id.list);

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mAutomator) {
            this.mAutomator.cancel();
            this.mAutomator = null;
        }
    }
}

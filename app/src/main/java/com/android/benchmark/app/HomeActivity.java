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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.benchmark.R;
import com.android.benchmark.api.JankBenchAPI;
import com.android.benchmark.config.Constants;
import com.android.benchmark.registry.BenchmarkRegistry;
import com.android.benchmark.results.GlobalResultsStore;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.topjohnwu.superuser.Shell;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    static {
        /* Shell.Config methods shall be called before any shell is created
         * This is the why in this example we call it in a static block
         * The followings are some examples, check Javadoc for more details */
        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR | Shell.FLAG_NON_ROOT_SHELL);
        Shell.Config.setTimeout(10);
    }

    private FloatingActionButton mStartButton;
    private BenchmarkRegistry mRegistry;
    private Queue<Intent> mRunnableBenchmarks;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_home);

        final Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        this.mStartButton = this.findViewById(R.id.start_button);
        this.mStartButton.setActivated(true);
        this.mStartButton.setOnClickListener(this);

        this.mRegistry = new BenchmarkRegistry(this);

        this.mRunnableBenchmarks = new LinkedList<>();

        final ExpandableListView listView = this.findViewById(R.id.test_list);
        final BenchmarkListAdapter adapter =
                new BenchmarkListAdapter(LayoutInflater.from(this), this.mRegistry);
        listView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
        final ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
        layoutParams.height = 2048;
        listView.setLayoutParams(layoutParams);
        listView.requestLayout();
        System.out.println(System.getProperties().stringPropertyNames());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            new AsyncTask<Void, Void, Void>() {
                @Nullable
                @Override
                protected Void doInBackground(final Void... voids) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(HomeActivity.this, "Exporting...", Toast.LENGTH_LONG).show();
                            }
                        });
                        GlobalResultsStore.getInstance(HomeActivity.this).exportToCsv();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(final Void aVoid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this, "Done", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }.execute();

            return true;
        } else if (id == R.id.action_upload) {

            new AsyncTask<Void, Void, Void>() {
                boolean success;

                @Nullable
                @Override
                protected Void doInBackground(final Void... voids) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this, "Uploading results...", Toast.LENGTH_LONG).show();
                        }
                    });

                    this.success = JankBenchAPI.uploadResults(HomeActivity.this, Constants.BASE_URL);

                    return null;
                }

                @Override
                protected void onPostExecute(final Void aVoid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this, success ? "Upload succeeded" : "Upload failed", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }.execute();

            return true;
        } else if (id == R.id.action_view_results) {
            final Uri webpage = Uri.parse("https://jankbenchx.vercel.app");
            final Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            if (null != intent.resolveActivity(getPackageManager())) {
                this.startActivity(intent);
            }
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(final View v) {
        int groupCount = this.mRegistry.getGroupCount();
        for (int i = 0; i < groupCount; i++) {

            final Intent intent = this.mRegistry.getBenchmarkGroup(i).getIntent();
            if (null != intent) {
                this.mRunnableBenchmarks.add(intent);
            }
        }

        this.handleNextBenchmark();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleNextBenchmark() {
        final Intent nextIntent = this.mRunnableBenchmarks.peek();
        this.startActivityForResult(nextIntent, 0);
    }
}

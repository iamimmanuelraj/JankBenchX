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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.FrameMetrics;
import android.widget.Toast;

import com.android.benchmark.models.Result;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class GlobalResultsStore extends SQLiteOpenHelper {
    private static final int VERSION = 2;

    private static GlobalResultsStore sInstance;
    private static final String UI_RESULTS_TABLE = "ui_results";
    private static final String REFRESH_RATE_TABLE = "refresh_rates";

    private final Context mContext;

    private GlobalResultsStore(final Context context) {
        super(context, "BenchmarkResults", null, GlobalResultsStore.VERSION);
        this.mContext = context;
    }

    public static GlobalResultsStore getInstance(final Context context) {
        if (null == sInstance) {
            GlobalResultsStore.sInstance = new GlobalResultsStore(context.getApplicationContext());
        }

        return GlobalResultsStore.sInstance;
    }

    @Override
    public void onCreate(final SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + GlobalResultsStore.UI_RESULTS_TABLE + " (" +
                " _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " name TEXT," +
                " run_id INTEGER," +
                " iteration INTEGER," +
                " timestamp TEXT,"  +
                " unknown_delay REAL," +
                " input REAL," +
                " animation REAL," +
                " layout REAL," +
                " draw REAL," +
                " sync REAL," +
                " command_issue REAL," +
                " swap_buffers REAL," +
                " total_duration REAL," +
                " jank_frame BOOLEAN, " +
                " device_charging INTEGER);");

        sqLiteDatabase.execSQL("CREATE TABLE " + GlobalResultsStore.REFRESH_RATE_TABLE + " (" +
                " _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " run_id INTEGER," +
                " refresh_rate INTEGER);");
    }

    public void storeRunResults(final String testName, final int runId, final int iteration,
                                final UiBenchmarkResult result, final float refresh_rate) {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            final String date = DateFormat.getDateTimeInstance().format(new Date());
            int jankIndexIndex = 0;
            final int[] sortedJankIndices = result.getSortedJankFrameIndices();
            final int totalFrameCount = result.getTotalFrameCount();
            for (int frameIdx = 0; frameIdx < totalFrameCount; frameIdx++) {
                final ContentValues cv = new ContentValues();
                cv.put("name", testName);
                cv.put("run_id", runId);
                cv.put("iteration", iteration);
                cv.put("timestamp", date);
                cv.put("unknown_delay",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.UNKNOWN_DELAY_DURATION));
                cv.put("input",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.INPUT_HANDLING_DURATION));
                cv.put("animation",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.ANIMATION_DURATION));
                cv.put("layout",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.LAYOUT_MEASURE_DURATION));
                cv.put("draw",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.DRAW_DURATION));
                cv.put("sync",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.SYNC_DURATION));
                cv.put("command_issue",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.COMMAND_ISSUE_DURATION));
                cv.put("swap_buffers",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.SWAP_BUFFERS_DURATION));
                cv.put("total_duration",
                        result.getMetricAtIndex(frameIdx, FrameMetrics.TOTAL_DURATION));
                if (jankIndexIndex < sortedJankIndices.length &&
                        sortedJankIndices[jankIndexIndex] == frameIdx) {
                    jankIndexIndex++;
                    cv.put("jank_frame", true);
                } else {
                    cv.put("jank_frame", false);
                }
                db.insert(GlobalResultsStore.UI_RESULTS_TABLE, null, cv);
            }

            // Store Display Refresh Rate
            final ContentValues cv = new ContentValues();
            cv.put("run_id", runId);
            cv.put("refresh_rate", Math.round(refresh_rate));
            db.insert(GlobalResultsStore.REFRESH_RATE_TABLE, null, cv);

            db.setTransactionSuccessful();
            Toast.makeText(this.mContext, "Score: " + result.getScore()
                    + " Jank: " + (100 * sortedJankIndices.length) / (float) totalFrameCount + "%",
                    Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

    }

    public ArrayList<UiBenchmarkResult> loadTestResults(final String testName, final int runId) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final ArrayList<UiBenchmarkResult> resultList = new ArrayList<>();
        try {
            final String[] columnsToQuery = {
                    "name",
                    "run_id",
                    "iteration",
                    "unknown_delay",
                    "input",
                    "animation",
                    "layout",
                    "draw",
                    "sync",
                    "command_issue",
                    "swap_buffers",
                    "total_duration",
            };

            final Cursor cursor = db.query(
                    GlobalResultsStore.UI_RESULTS_TABLE, columnsToQuery, "run_id=? AND name=?",
                    new String[] { Integer.toString(runId), testName }, null, null, "iteration");

            final double[] values = new double[columnsToQuery.length - 3];

            while (cursor.moveToNext()) {
                final int iteration = cursor.getInt(cursor.getColumnIndexOrThrow("iteration"));

                values[0] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("unknown_delay"));
                values[1] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("input"));
                values[2] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("animation"));
                values[3] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("layout"));
                values[4] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("draw"));
                values[5] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("sync"));
                values[6] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("command_issue"));
                values[7] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("swap_buffers"));
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"));

                final UiBenchmarkResult iterationResult;
                if (resultList.size() == iteration) {
                    final int refresh_rate = this.loadRefreshRate(runId, db);
                    iterationResult = new UiBenchmarkResult(values, refresh_rate);
                    resultList.add(iteration, iterationResult);
                } else {
                    iterationResult = resultList.get(iteration);
                    iterationResult.update(values);
                }
            }

            cursor.close();
        } finally {
            db.close();
        }

        final int total = resultList.get(0).getTotalFrameCount();
        for (int i = 0; i < total; i++) {
            System.out.println(String.valueOf(resultList.get(0).getMetricAtIndex(0, FrameMetrics.TOTAL_DURATION)));
        }

        return resultList;
    }

    public HashMap<String, ArrayList<UiBenchmarkResult>> loadDetailedResults(final int runId) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final HashMap<String, ArrayList<UiBenchmarkResult>> results = new HashMap<>();
        try {
            final String[] columnsToQuery = {
                    "name",
                    "run_id",
                    "iteration",
                    "unknown_delay",
                    "input",
                    "animation",
                    "layout",
                    "draw",
                    "sync",
                    "command_issue",
                    "swap_buffers",
                    "total_duration",
            };

            final Cursor cursor = db.query(
                    GlobalResultsStore.UI_RESULTS_TABLE, columnsToQuery, "run_id=?",
                    new String[] { Integer.toString(runId) }, null, null, "name, iteration");

            final double[] values = new double[columnsToQuery.length - 3];
            while (cursor.moveToNext()) {
                final int iteration = cursor.getInt(cursor.getColumnIndexOrThrow("iteration"));
                final String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                ArrayList<UiBenchmarkResult> resultList = results.get(name);
                if (null == resultList) {
                    resultList = new ArrayList<>();
                    results.put(name, resultList);
                }

                values[0] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("unknown_delay"));
                values[1] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("input"));
                values[2] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("animation"));
                values[3] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("layout"));
                values[4] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("draw"));
                values[5] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("sync"));
                values[6] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("command_issue"));
                values[7] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("swap_buffers"));
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"));
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"));

                final UiBenchmarkResult iterationResult;
                if (resultList.size() == iteration) {
                    final int refresh_rate = this.loadRefreshRate(runId, db);
                    iterationResult = new UiBenchmarkResult(values, refresh_rate);
                    resultList.add(iterationResult);
                } else {
                    iterationResult = resultList.get(iteration);
                    iterationResult.update(values);
                }
            }

            cursor.close();
        } finally {
            db.close();
        }

        return results;
    }

    public int getLastRunId() {
        int runId = 0;
        final SQLiteDatabase db = this.getReadableDatabase();
        try {
            final String query = "SELECT run_id FROM " + GlobalResultsStore.UI_RESULTS_TABLE + " WHERE _id = (SELECT MAX(_id) FROM " + GlobalResultsStore.UI_RESULTS_TABLE + ")";
            final Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                runId = cursor.getInt(0);
            }
            cursor.close();
        } finally {
            db.close();
        }

        return runId;
    }

    public int loadRefreshRate(final int runId, final SQLiteDatabase db) {
        int refresh_rate = -1;

        String[] columnsToQuery = new String[] {
                "run_id",
                "refresh_rate"
        };
        Cursor cursor = db.query(REFRESH_RATE_TABLE, columnsToQuery, "run_id=?", new String[] { Integer.toString(runId) }, null, null, null);
        if (cursor.moveToFirst()) {
            refresh_rate = cursor.getInt((1));
        }
        cursor.close();

        return refresh_rate;
    }

    public HashMap<String, UiBenchmarkResult> loadDetailedAggregatedResults(final int runId) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final HashMap<String, UiBenchmarkResult> testsResults = new HashMap<>();
        try {
            final String[] columnsToQuery = {
                    "name",
                    "run_id",
                    "iteration",
                    "unknown_delay",
                    "input",
                    "animation",
                    "layout",
                    "draw",
                    "sync",
                    "command_issue",
                    "swap_buffers",
                    "total_duration",
            };

            final Cursor cursor = db.query(
                    GlobalResultsStore.UI_RESULTS_TABLE, columnsToQuery, "run_id=?",
                    new String[] { Integer.toString(runId) }, null, null, "name");

            final double[] values = new double[columnsToQuery.length - 3];
            while (cursor.moveToNext()) {
                final String testName = cursor.getString(cursor.getColumnIndexOrThrow("name"));

                values[0] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("unknown_delay"));
                values[1] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("input"));
                values[2] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("animation"));
                values[3] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("layout"));
                values[4] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("draw"));
                values[5] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("sync"));
                values[6] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("command_issue"));
                values[7] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("swap_buffers"));
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"));
                values[8] = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_duration"));

                UiBenchmarkResult result = testsResults.get(testName);
                if (null == result) {
                    final int refresh_rate = this.loadRefreshRate(runId, db);
                    result = new UiBenchmarkResult(values, refresh_rate);
                    testsResults.put(testName, result);
                } else {
                    result.update(values);
                }
            }

            cursor.close();
        } finally {
            db.close();
        }

        return testsResults;
    }

    public void exportToCsv() throws IOException {
        final String path = this.mContext.getFilesDir() + "/results-" + System.currentTimeMillis() + ".csv";
        final SQLiteDatabase db = this.getReadableDatabase();

        // stats across metrics for each run and each test
        final HashMap<String, DescriptiveStatistics> stats = new HashMap<>();

        final Cursor runIdCursor = db.query(
                GlobalResultsStore.UI_RESULTS_TABLE, new String[] { "run_id" }, null, null, "run_id", null, null);

        while (runIdCursor.moveToNext()) {

            final int runId = runIdCursor.getInt(runIdCursor.getColumnIndexOrThrow("run_id"));
            final HashMap<String, ArrayList<UiBenchmarkResult>> detailedResults =
                    this.loadDetailedResults(runId);

            this.writeRawResults(runId, detailedResults);

            final DescriptiveStatistics overall = new DescriptiveStatistics();
            try (final FileWriter writer = new FileWriter(path, true)) {
                writer.write("Run ID, " + runId + "\n");
                writer.write("Test, Iteration, Score, Jank Penalty, Consistency Bonus, 95th, " +
                        "90th\n");
                for (final String testName : detailedResults.keySet()) {
                    final ArrayList<UiBenchmarkResult> results = detailedResults.get(testName);
                    final DescriptiveStatistics scoreStats = new DescriptiveStatistics();
                    final DescriptiveStatistics jankPenalty = new DescriptiveStatistics();
                    final DescriptiveStatistics consistencyBonus = new DescriptiveStatistics();
                    for (int i = 0; i < results.size(); i++) {
                        final UiBenchmarkResult result = results.get(i);
                        final int score = result.getScore();
                        scoreStats.addValue(score);
                        overall.addValue(score);
                        jankPenalty.addValue(result.getJankPenalty());
                        consistencyBonus.addValue(result.getConsistencyBonus());

                        writer.write(testName);
                        writer.write(",");
                        writer.write(String.valueOf(i));
                        writer.write(",");
                        writer.write(String.valueOf(score));
                        writer.write(",");
                        writer.write(String.valueOf(result.getJankPenalty()));
                        writer.write(",");
                        writer.write(String.valueOf(result.getConsistencyBonus()));
                        writer.write(",");
                        writer.write(Double.toString(
                                result.getPercentile(FrameMetrics.TOTAL_DURATION, 95)));
                        writer.write(",");
                        writer.write(Double.toString(
                                result.getPercentile(FrameMetrics.TOTAL_DURATION, 90)));
                        writer.write("\n");
                    }

                    writer.write("Score CV," +
                            (100 * scoreStats.getStandardDeviation()
                                    / scoreStats.getMean()) + "%\n");
                    writer.write("Jank Penalty CV, " +
                            (100 * jankPenalty.getStandardDeviation()
                                    / jankPenalty.getMean()) + "%\n");
                    writer.write("Consistency Bonus CV, " +
                            (100 * consistencyBonus.getStandardDeviation()
                                    / consistencyBonus.getMean()) + "%\n");
                    writer.write("\n");
                }

                writer.write("Overall Score CV,"  +
                        (100 * overall.getStandardDeviation() / overall.getMean()) + "%\n");
                writer.flush();
            }
        }

        runIdCursor.close();
    }

    private void writeRawResults(final int runId,
                                 final HashMap<String, ArrayList<UiBenchmarkResult>> detailedResults) {
        String path = this.mContext.getFilesDir() +
                "/" +
                runId +
                ".csv";
        try (final FileWriter writer = new FileWriter(path)) {
            for (final String test : detailedResults.keySet()) {
                writer.write("Test, " + test + "\n");
                writer.write("iteration, unknown delay, input, animation, layout, draw, sync, " +
                        "command issue, swap buffers\n");
                final ArrayList<UiBenchmarkResult> runs = detailedResults.get(test);
                for (int i = 0; i < runs.size(); i++) {
                    final UiBenchmarkResult run = runs.get(i);
                    for (int j = 0; j < run.getTotalFrameCount(); j++) {
                        writer.write(i + "," +
                                run.getMetricAtIndex(j, FrameMetrics.UNKNOWN_DELAY_DURATION) + "," +
                                run.getMetricAtIndex(j, FrameMetrics.INPUT_HANDLING_DURATION) + "," +
                                run.getMetricAtIndex(j, FrameMetrics.ANIMATION_DURATION) + "," +
                                run.getMetricAtIndex(j, FrameMetrics.LAYOUT_MEASURE_DURATION) + "," +
                                run.getMetricAtIndex(j, FrameMetrics.DRAW_DURATION) + "," +
                                run.getMetricAtIndex(j, FrameMetrics.SYNC_DURATION) + "," +
                                run.getMetricAtIndex(j, FrameMetrics.COMMAND_ISSUE_DURATION) + "," +
                                run.getMetricAtIndex(j, FrameMetrics.SWAP_BUFFERS_DURATION) + "," +
                                run.getMetricAtIndex(j, FrameMetrics.TOTAL_DURATION) + "\n");
                    }
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int oldVersion, final int currentVersion) {
        if (VERSION > oldVersion) {
            sqLiteDatabase.execSQL("ALTER TABLE "
                    + GlobalResultsStore.UI_RESULTS_TABLE + " ADD COLUMN timestamp TEXT;");
        }
    }
}

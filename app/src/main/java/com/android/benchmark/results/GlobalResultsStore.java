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

import androidx.annotation.NonNull;

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

    private GlobalResultsStore(Context context) {
        super(context, "BenchmarkResults", null, VERSION);
        mContext = context;
    }

    @NonNull
    public static GlobalResultsStore getInstance(@NonNull Context context) {
        if (null == GlobalResultsStore.sInstance) {
            sInstance = new GlobalResultsStore(context.getApplicationContext());
        }

        return sInstance;
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + UI_RESULTS_TABLE + " (" +
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

        sqLiteDatabase.execSQL("CREATE TABLE " + REFRESH_RATE_TABLE + " (" +
                " _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " run_id INTEGER," +
                " refresh_rate INTEGER);");
    }

    public void storeRunResults(String testName, int runId, int iteration,
                                @NonNull UiBenchmarkResult result, float refresh_rate) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            String date = DateFormat.getDateTimeInstance().format(new Date());
            int jankIndexIndex = 0;
            int[] sortedJankIndices = result.getSortedJankFrameIndices();
            int totalFrameCount = result.getTotalFrameCount();
            for (int frameIdx = 0; frameIdx < totalFrameCount; frameIdx++) {
                ContentValues cv = new ContentValues();
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
                db.insert(UI_RESULTS_TABLE, null, cv);
            }

            // Store Display Refresh Rate
            ContentValues cv = new ContentValues();
            cv.put("run_id", runId);
            cv.put("refresh_rate", Math.round(refresh_rate));
            db.insert(REFRESH_RATE_TABLE, null, cv);

            db.setTransactionSuccessful();
            Toast.makeText(mContext, "Score: " + result.getScore()
                    + " Jank: " + (100 * sortedJankIndices.length) / (float) totalFrameCount + "%",
                    Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

    }

    @NonNull
    public ArrayList<UiBenchmarkResult> loadTestResults(String testName, int runId) {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<UiBenchmarkResult> resultList = new ArrayList<>();
        try {
            String[] columnsToQuery = {
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

            Cursor cursor = db.query(
                    UI_RESULTS_TABLE, columnsToQuery, "run_id=? AND name=?",
                    new String[] { Integer.toString(runId), testName }, null, null, "iteration");

            double[] values = new double[columnsToQuery.length - 3];

            while (cursor.moveToNext()) {
                int iteration = cursor.getInt(cursor.getColumnIndexOrThrow("iteration"));

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

                UiBenchmarkResult iterationResult;
                if (resultList.size() == iteration) {
                    int refresh_rate = loadRefreshRate(runId, db);
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

        int total = resultList.get(0).getTotalFrameCount();
        for (int i = 0; i < total; i++) {
            System.out.println(resultList.get(0).getMetricAtIndex(0, FrameMetrics.TOTAL_DURATION));
        }

        return resultList;
    }

    @NonNull
    public HashMap<String, ArrayList<UiBenchmarkResult>> loadDetailedResults(int runId) {
        SQLiteDatabase db = getReadableDatabase();
        HashMap<String, ArrayList<UiBenchmarkResult>> results = new HashMap<>();
        try {
            String[] columnsToQuery = {
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

            Cursor cursor = db.query(
                    UI_RESULTS_TABLE, columnsToQuery, "run_id=?",
                    new String[] { Integer.toString(runId) }, null, null, "name, iteration");

            double[] values = new double[columnsToQuery.length - 3];
            while (cursor.moveToNext()) {
                int iteration = cursor.getInt(cursor.getColumnIndexOrThrow("iteration"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
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

                UiBenchmarkResult iterationResult;
                if (resultList.size() == iteration) {
                    int refresh_rate = loadRefreshRate(runId, db);
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
        SQLiteDatabase db = getReadableDatabase();
        try {
            final String query = "SELECT run_id FROM " + UI_RESULTS_TABLE + " WHERE _id = (SELECT MAX(_id) FROM " + UI_RESULTS_TABLE + ")";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                runId = cursor.getInt(0);
            }
            cursor.close();
        } finally {
            db.close();
        }

        return runId;
    }

    public int loadRefreshRate(int runId, @NonNull SQLiteDatabase db) {
        int refresh_rate = -1;

        final String[] columnsToQuery = {
                "run_id",
                "refresh_rate"
        };
        final Cursor cursor = db.query(GlobalResultsStore.REFRESH_RATE_TABLE, columnsToQuery, "run_id=?", new String[] { Integer.toString(runId) }, null, null, null);
        if (cursor.moveToFirst()) {
            refresh_rate = cursor.getInt((1));
        }
        cursor.close();

        return refresh_rate;
    }

    @NonNull
    public HashMap<String, UiBenchmarkResult> loadDetailedAggregatedResults(int runId) {
        SQLiteDatabase db = getReadableDatabase();
        HashMap<String, UiBenchmarkResult> testsResults = new HashMap<>();
        try {
            String[] columnsToQuery = {
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

            Cursor cursor = db.query(
                    UI_RESULTS_TABLE, columnsToQuery, "run_id=?",
                    new String[] { Integer.toString(runId) }, null, null, "name");

            double[] values = new double[columnsToQuery.length - 3];
            while (cursor.moveToNext()) {
                String testName = cursor.getString(cursor.getColumnIndexOrThrow("name"));

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
                    int refresh_rate = loadRefreshRate(runId, db);
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
        String path = mContext.getFilesDir() + "/results-" + System.currentTimeMillis() + ".csv";
        SQLiteDatabase db = getReadableDatabase();

        // stats across metrics for each run and each test
        HashMap<String, DescriptiveStatistics> stats = new HashMap<>();

        Cursor runIdCursor = db.query(
                UI_RESULTS_TABLE, new String[] { "run_id" }, null, null, "run_id", null, null);

        while (runIdCursor.moveToNext()) {

            int runId = runIdCursor.getInt(runIdCursor.getColumnIndexOrThrow("run_id"));
            HashMap<String, ArrayList<UiBenchmarkResult>> detailedResults =
                    loadDetailedResults(runId);

            writeRawResults(runId, detailedResults);

            DescriptiveStatistics overall = new DescriptiveStatistics();
            try (FileWriter writer = new FileWriter(path, true)) {
                writer.write("Run ID, " + runId + "\n");
                writer.write("Test, Iteration, Score, Jank Penalty, Consistency Bonus, 95th, " +
                        "90th\n");
                for (String testName : detailedResults.keySet()) {
                    ArrayList<UiBenchmarkResult> results = detailedResults.get(testName);
                    DescriptiveStatistics scoreStats = new DescriptiveStatistics();
                    DescriptiveStatistics jankPenalty = new DescriptiveStatistics();
                    DescriptiveStatistics consistencyBonus = new DescriptiveStatistics();
                    for (int i = 0; i < results.size(); i++) {
                        UiBenchmarkResult result = results.get(i);
                        int score = result.getScore();
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

    private void writeRawResults(int runId,
                                 @NonNull HashMap<String, ArrayList<UiBenchmarkResult>> detailedResults) {
        final String path = mContext.getFilesDir() +
                "/" +
                runId +
                ".csv";
        try (FileWriter writer = new FileWriter(path)) {
            for (String test : detailedResults.keySet()) {
                writer.write("Test, " + test + "\n");
                writer.write("iteration, unknown delay, input, animation, layout, draw, sync, " +
                        "command issue, swap buffers\n");
                ArrayList<UiBenchmarkResult> runs = detailedResults.get(test);
                for (int i = 0; i < runs.size(); i++) {
                    UiBenchmarkResult run = runs.get(i);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase sqLiteDatabase, int oldVersion, int currentVersion) {
        if (GlobalResultsStore.VERSION > oldVersion) {
            sqLiteDatabase.execSQL("ALTER TABLE "
                    + UI_RESULTS_TABLE + " ADD COLUMN timestamp TEXT;");
        }
    }
}

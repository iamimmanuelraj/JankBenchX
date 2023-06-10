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

package com.android.benchmark.synthetic;

import android.view.View;
import android.widget.TextView;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.LinkedList;
import java.util.Queue;


public class TestInterface {
    native long nInit(long options);
    native long nDestroy(long b);
    native float nGetData(long b, float[] data);
    native boolean nRunPowerManagementTest(long b, long options);
    native boolean nRunCPUHeatSoakTest(long b, long options);

    native boolean nMemTestStart(long b);
    native float nMemTestBandwidth(long b, long size);
    native float nMemTestLatency(long b, long size);
    native void nMemTestEnd(long b);

    native float nGFlopsTest(long b, long opt);

    public static class TestResultCallback {
        void onTestResult(final int command, final float result) { }
    }

    static {
        System.loadLibrary("nativebench");
    }

    float[] mLinesLow;
    float[] mLinesHigh;
    float[] mLinesValue;
    TextView mTextStatus;
    TextView mTextMin;
    TextView mTextMax;
    TextView mTextTypical;

    private final View mViewToUpdate;

    private final LooperThread mLT;

    TestInterface(final View v, final int runtimeSeconds, final TestResultCallback callback) {
        final int buckets = runtimeSeconds * 1000;
        this.mLinesLow = new float[buckets * 4];
        this.mLinesHigh = new float[buckets * 4];
        this.mLinesValue = new float[buckets * 4];
        this.mViewToUpdate = v;

        this.mLT = new LooperThread(this, callback);
        this.mLT.start();
    }

    static class LooperThread extends Thread {
        public static final int CommandExit = 1;
        public static final int TestPowerManagement = 2;
        public static final int TestMemoryBandwidth = 3;
        public static final int TestMemoryLatency = 4;
        public static final int TestHeatSoak = 5;
        public static final int TestGFlops = 6;

        private volatile boolean mRun = true;
        private final TestInterface mTI;
        private final TestResultCallback mCallback;

        Queue<Integer> mCommandQueue = new LinkedList<Integer>();

        LooperThread(final TestInterface ti, final TestResultCallback callback) {
            super("BenchmarkTestThread");
            this.mTI = ti;
            this.mCallback = callback;
        }

        void runCommand(final int command) {
            final Integer i = Integer.valueOf(command);

            synchronized (this) {
                this.mCommandQueue.add(i);
                this.notifyAll();
            }
        }

        public void run() {
            final long b = this.mTI.nInit(0);
            if (0 == b) {
                return;
            }

            while (this.mRun) {
                int command = 0;
                synchronized (this) {
                    if (this.mCommandQueue.isEmpty()) {
                        try {
                            this.wait();
                        } catch (final InterruptedException e) {
                        }
                    }

                    if (!this.mCommandQueue.isEmpty()) {
                        command = this.mCommandQueue.remove();
                    }
                }

                if (CommandExit == command) {
                    this.mRun = false;
                } else if (TestPowerManagement == command) {
                    final float score = this.mTI.testPowerManagement(b);
                    this.mCallback.onTestResult(TestPowerManagement, 0);
                } else if (TestMemoryBandwidth == command) {
                    this.mTI.testCPUMemoryBandwidth(b);
                } else if (TestMemoryLatency == command) {
                    this.mTI.testCPUMemoryLatency(b);
                } else if (TestHeatSoak == command) {
                    this.mTI.testCPUHeatSoak(b);
                } else if (TestGFlops == command) {
                    this.mTI.testCPUGFlops(b);
                }

                //mViewToUpdate.post(new Runnable() {
                  //  public void run() {
                   //     mViewToUpdate.invalidate();
                    //}
                //});
            }

            this.mTI.nDestroy(b);
        }

        void exit() {
            this.mRun = false;
        }
    }

    void postTextToView(final TextView v, final String s) {
        TextView tv = v;
        String ts = s;

        v.post(new Runnable() {
            public void run() {
                tv.setText(ts);
            }
        });

    }

    float calcAverage(final float[] data) {
        float total = 0.0f;
        for (int ct=0; ct < data.length; ct++) {
            total += data[ct];
        }
        return total / data.length;
    }

    void makeGraph(final float[] data, final float[] lines) {
        for (int ct = 0; ct < data.length; ct++) {
            lines[ct * 4] = ct;
            lines[ct * 4 + 1] = 500.0f - data[ct];
            lines[ct * 4 + 2] = ct;
            lines[ct * 4 + 3] = 500.0f;
        }
    }

    float testPowerManagement(final long b) {
        final float[] dat = new float[this.mLinesLow.length / 4];
        this.postTextToView(this.mTextStatus, "Running single-threaded");
        this.nRunPowerManagementTest(b, 1);
        this.nGetData(b, dat);
        this.makeGraph(dat, this.mLinesLow);
        this.mViewToUpdate.postInvalidate();
        final float avgMin = this.calcAverage(dat);

        this.postTextToView(this.mTextMin, "Single threaded " + avgMin + " per second");

        this.postTextToView(this.mTextStatus, "Running multi-threaded");
        this.nRunPowerManagementTest(b, 4);
        this.nGetData(b, dat);
        this.makeGraph(dat, this.mLinesHigh);
        this.mViewToUpdate.postInvalidate();
        final float avgMax = this.calcAverage(dat);
        this.postTextToView(this.mTextMax, "Multi threaded " + avgMax + " per second");

        this.postTextToView(this.mTextStatus, "Running typical");
        this.nRunPowerManagementTest(b, 0);
        this.nGetData(b, dat);
        this.makeGraph(dat, this.mLinesValue);
        this.mViewToUpdate.postInvalidate();
        final float avgTypical = this.calcAverage(dat);

        final float ofIdeal = avgTypical / (avgMax + avgMin) * 200.0f;
        this.postTextToView(this.mTextTypical, String.format("Typical mix (50/50) %%%2.0f of ideal", ofIdeal));
        return ofIdeal * (avgMax + avgMin);
    }

    float testCPUHeatSoak(final long b) {
        final float[] dat = new float[1000];
        this.postTextToView(this.mTextStatus, "Running heat soak test");
        for (int t = 0; 1000 > t; t++) {
            this.mLinesLow[t * 4] = t;
            this.mLinesLow[t * 4 + 1] = 498.0f;
            this.mLinesLow[t * 4 + 2] = t;
            this.mLinesLow[t * 4 + 3] = 500.0f;
        }

        float peak = 0.0f;
        float total = 0.0f;
        float dThroughput = 0;
        float prev = 0;
        final SummaryStatistics stats = new SummaryStatistics();
        for (int t = 0; 1000 > t; t++) {
            this.nRunCPUHeatSoakTest(b, 1);
            this.nGetData(b, dat);

            final float p = this.calcAverage(dat);
            if (0 != prev) {
                dThroughput += (prev - p);
            }

            prev = p;

            this.mLinesLow[t * 4 + 1] = 499.0f - p;
            if (peak < p) {
                peak = p;
            }
            for (final float f : dat) {
                stats.addValue(f);
            }

            total += p;

            this.mViewToUpdate.postInvalidate();
            this.postTextToView(this.mTextMin, "Peak " + peak + " per second");
            this.postTextToView(this.mTextMax, "Current " + p + " per second");
            this.postTextToView(this.mTextTypical, "Average " + (total / (t + 1)) + " per second");
        }


        final float decreaseOverTime = dThroughput / 1000;

        System.out.println("dthroughput/dt: " + decreaseOverTime);

        final float score = (float) (stats.getMean() / (stats.getStandardDeviation() * decreaseOverTime));

        this.postTextToView(this.mTextStatus, "Score: " + score);
        return score;
    }

    void testCPUMemoryBandwidth(final long b) {
        final int[] sizeK = {1, 2, 3, 4, 5, 6, 7,
                    8, 10, 12, 14, 16, 20, 24, 28,
                    32, 40, 48, 56, 64, 80, 96, 112,
                    128, 160, 192, 224, 256, 320, 384, 448,
                    512, 640, 768, 896, 1024, 1280, 1536, 1792,
                    2048, 2560, 3584, 4096, 5120, 6144, 7168,
                    8192, 10240, 12288, 14336, 16384
        };
        final int subSteps = 15;
        final float[] results = new float[sizeK.length * subSteps];

        this.nMemTestStart(b);

        final float[] dat = new float[1000];
        this.postTextToView(this.mTextStatus, "Running Memory Bandwidth test");
        for (int t = 0; 1000 > t; t++) {
            this.mLinesLow[t * 4] = t;
            this.mLinesLow[t * 4 + 1] = 498.0f;
            this.mLinesLow[t * 4 + 2] = t;
            this.mLinesLow[t * 4 + 3] = 500.0f;
        }

        for (int i = 0; i < sizeK.length; i++) {
            this.postTextToView(this.mTextStatus, "Running " + sizeK[i] + " K");

            float rtot = 0.0f;
            for (int j = 0; subSteps > j; j++) {
                final float ret = this.nMemTestBandwidth(b, sizeK[i] * 1024);
                rtot += ret;
                results[i * subSteps + j] = ret;
                this.mLinesLow[(i * subSteps + j) * 4 + 1] = 499.0f - (results[i*15+j] * 20.0f);
                this.mViewToUpdate.postInvalidate();
            }
            rtot /= subSteps;

            if (2 == sizeK[i]) {
                this.postTextToView(this.mTextMin, "2K " + rtot + " GB/s");
            }
            if (128 == sizeK[i]) {
                this.postTextToView(this.mTextMax, "128K " + rtot + " GB/s");
            }
            if (8192 == sizeK[i]) {
                this.postTextToView(this.mTextTypical, "8M " + rtot + " GB/s");
            }

        }

        this.nMemTestEnd(b);
        this.postTextToView(this.mTextStatus, "Done");
    }

    void testCPUMemoryLatency(final long b) {
        final int[] sizeK = {1, 2, 3, 4, 5, 6, 7,
                8, 10, 12, 14, 16, 20, 24, 28,
                32, 40, 48, 56, 64, 80, 96, 112,
                128, 160, 192, 224, 256, 320, 384, 448,
                512, 640, 768, 896, 1024, 1280, 1536, 1792,
                2048, 2560, 3584, 4096, 5120, 6144, 7168,
                8192, 10240, 12288, 14336, 16384
        };
        final int subSteps = 15;
        final float[] results = new float[sizeK.length * subSteps];

        this.nMemTestStart(b);

        final float[] dat = new float[1000];
        this.postTextToView(this.mTextStatus, "Running Memory Latency test");
        for (int t = 0; 1000 > t; t++) {
            this.mLinesLow[t * 4] = t;
            this.mLinesLow[t * 4 + 1] = 498.0f;
            this.mLinesLow[t * 4 + 2] = t;
            this.mLinesLow[t * 4 + 3] = 500.0f;
        }

        for (int i = 0; i < sizeK.length; i++) {
            this.postTextToView(this.mTextStatus, "Running " + sizeK[i] + " K");

            float rtot = 0.0f;
            for (int j = 0; subSteps > j; j++) {
                float ret = this.nMemTestLatency(b, sizeK[i] * 1024);
                rtot += ret;
                results[i * subSteps + j] = ret;

                if (400.0f < ret) ret = 400.0f;
                if (0.0f > ret) ret = 0.0f;
                this.mLinesLow[(i * subSteps + j) * 4 + 1] = 499.0f - ret;
                //android.util.Log.e("bench", "test bw " + sizeK[i] + " - " + ret);
                this.mViewToUpdate.postInvalidate();
            }
            rtot /= subSteps;

            if (2 == sizeK[i]) {
                this.postTextToView(this.mTextMin, "2K " + rtot + " ns");
            }
            if (128 == sizeK[i]) {
                this.postTextToView(this.mTextMax, "128K " + rtot + " ns");
            }
            if (8192 == sizeK[i]) {
                this.postTextToView(this.mTextTypical, "8M " + rtot + " ns");
            }

        }

        this.nMemTestEnd(b);
        this.postTextToView(this.mTextStatus, "Done");
    }

    void testCPUGFlops(final long b) {
        final int[] sizeK = {1, 2, 3, 4, 5, 6, 7
        };
        final int subSteps = 15;
        final float[] results = new float[sizeK.length * subSteps];

        this.nMemTestStart(b);

        final float[] dat = new float[1000];
        this.postTextToView(this.mTextStatus, "Running Memory Latency test");
        for (int t = 0; 1000 > t; t++) {
            this.mLinesLow[t * 4] = t;
            this.mLinesLow[t * 4 + 1] = 498.0f;
            this.mLinesLow[t * 4 + 2] = t;
            this.mLinesLow[t * 4 + 3] = 500.0f;
        }

        for (int i = 0; i < sizeK.length; i++) {
            this.postTextToView(this.mTextStatus, "Running " + sizeK[i] + " K");

            float rtot = 0.0f;
            for (int j = 0; subSteps > j; j++) {
                float ret = this.nGFlopsTest(b, sizeK[i] * 1024);
                rtot += ret;
                results[i * subSteps + j] = ret;

                if (400.0f < ret) ret = 400.0f;
                if (0.0f > ret) ret = 0.0f;
                this.mLinesLow[(i * subSteps + j) * 4 + 1] = 499.0f - ret;
                this.mViewToUpdate.postInvalidate();
            }
            rtot /= subSteps;

            if (2 == sizeK[i]) {
                this.postTextToView(this.mTextMin, "2K " + rtot + " ns");
            }
            if (128 == sizeK[i]) {
                this.postTextToView(this.mTextMax, "128K " + rtot + " ns");
            }
            if (8192 == sizeK[i]) {
                this.postTextToView(this.mTextTypical, "8M " + rtot + " ns");
            }

        }

        this.nMemTestEnd(b);
        this.postTextToView(this.mTextStatus, "Done");
    }

    public void runPowerManagement() {
        this.mLT.runCommand(LooperThread.TestPowerManagement);
    }

    public void runMemoryBandwidth() {
        this.mLT.runCommand(LooperThread.TestMemoryBandwidth);
    }

    public void runMemoryLatency() {
        this.mLT.runCommand(LooperThread.TestMemoryLatency);
    }

    public void runCPUHeatSoak() {
        this.mLT.runCommand(LooperThread.TestHeatSoak);
    }

    public void runCPUGFlops() {
        this.mLT.runCommand(LooperThread.TestGFlops);
    }
}

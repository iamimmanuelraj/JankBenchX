/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.benchmark.ui.automation;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Display;
import android.view.FrameMetrics;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.benchmark.results.GlobalResultsStore;
import com.android.benchmark.results.UiBenchmarkResult;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@TargetApi(24)
public class Automator extends HandlerThread
        implements ViewTreeObserver.OnGlobalLayoutListener, CollectorThread.CollectorListener {
    public static final long FRAME_PERIOD_MILLIS = 16;

    private static final int PRE_READY_STATE_COUNT = 3;
    private static final String TAG = "Benchmark.Automator";
    @NonNull
    private final AtomicInteger mReadyState;

    @Nullable
    private AutomateCallback mCallback;
    @Nullable
    private Window mWindow;
    @Nullable
    private AutomatorHandler mHandler;
    @Nullable
    private CollectorThread mCollectorThread;
    private int mRunId;
    private int mIteration;
    @Nullable
    private String mTestName;

    public static class AutomateCallback {
        public void onAutomate() {}
        public void onPostInteraction(List<FrameMetrics> metrics) {}
        public void onPostAutomate() {}

        protected final void addInteraction(Interaction interaction) {
            if (null == this.mInteractions) {
                return;
            }

            mInteractions.add(interaction);
        }

        protected final void setInteractions(List<Interaction> interactions) {
            mInteractions = interactions;
        }

        private List<Interaction> mInteractions;
    }

    private static final class AutomatorHandler extends Handler {
        public static final int MSG_NEXT_INTERACTION = 0;
        public static final int MSG_ON_AUTOMATE = 1;
        public static final int MSG_ON_POST_INTERACTION = 2;
        private final String mTestName;
        private final int mRunId;
        private final int mIteration;

        @NonNull
        private final Instrumentation mInstrumentation;
        private volatile boolean mCancelled;
        private final CollectorThread mCollectorThread;
        private final AutomateCallback mCallback;
        private final Window mWindow;

        @NonNull
        LinkedList<Interaction> mInteractions = new LinkedList<>();
        private UiBenchmarkResult mResults;

        AutomatorHandler(Looper looper, Window window, CollectorThread collectorThread,
                         AutomateCallback callback, String testName, int runId, int iteration) {
            super(looper);

            mInstrumentation = new Instrumentation();

            mCallback = callback;
            mWindow = window;
            mCollectorThread = collectorThread;
            mTestName = testName;
            mRunId = runId;
            mIteration = iteration;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (mCancelled) {
                return;
            }

            if (AutomatorHandler.MSG_NEXT_INTERACTION == msg.what) {
                if (!nextInteraction()) {
                    stopCollector();
                    writeResults();
                    mCallback.onPostAutomate();
                }
            } else if (AutomatorHandler.MSG_ON_AUTOMATE == msg.what) {
                mCollectorThread.attachToWindow(mWindow);
                mCallback.setInteractions(mInteractions);
                mCallback.onAutomate();
                postNextInteraction();
            } else if (AutomatorHandler.MSG_ON_POST_INTERACTION == msg.what) {
                List<FrameMetrics> collectedStats = (List<FrameMetrics>)msg.obj;
                persistResults(collectedStats);
                mCallback.onPostInteraction(collectedStats);
                postNextInteraction();
            }
        }

        public void cancel() {
            mCancelled = true;
            stopCollector();
        }

        private void stopCollector() {
            mCollectorThread.quitCollector();
        }

        private boolean nextInteraction() {

            Interaction interaction = mInteractions.poll();
            if (null != interaction) {
                doInteraction(interaction);
                return true;
            }
            return false;
        }

        private void doInteraction(@NonNull Interaction interaction) {
            if (mCancelled) {
                return;
            }

            mCollectorThread.markInteractionStart();

            if (Interaction.Type.KEY_EVENT == interaction.getType()) {
                for (int code : interaction.getKeyCodes()) {
                    if (!mCancelled) {
                        mInstrumentation.sendKeyDownUpSync(code);
                    } else {
                        break;
                    }
                }
            } else {
                for (MotionEvent event : interaction.getEvents()) {
                    if (!mCancelled) {
                        mInstrumentation.sendPointerSync(event);
                    } else {
                        break;
                    }
                }
            }
        }

        private void postNextInteraction() {
            final Message msg = obtainMessage(AutomatorHandler.MSG_NEXT_INTERACTION);
            sendMessage(msg);
        }

        private void persistResults(@NonNull List<FrameMetrics> stats) {
            if (stats.isEmpty()) {
                return;
            }

            if (null == this.mResults) {
                float refresh_rate = getFrameRate(mWindow.getContext());
                mResults = new UiBenchmarkResult(stats, (int) refresh_rate);
            } else {
                mResults.update(stats);
            }
        }

        private void writeResults() {
            float refresh_rate = getFrameRate(mWindow.getContext());

            GlobalResultsStore.getInstance(mWindow.getContext())
                    .storeRunResults(mTestName, mRunId, mIteration, mResults, refresh_rate);
        }
    }

    private static float getFrameRate(@NonNull Context context) {
        final DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        return display.getRefreshRate();
    }

    private void initHandler() {
        mHandler = new AutomatorHandler(getLooper(), mWindow, mCollectorThread, mCallback,
                mTestName, mRunId, mIteration);
        mWindow = null;
        mCallback = null;
        mCollectorThread = null;
        mTestName = null;
        mRunId = 0;
        mIteration = 0;
    }

    @Override
    public final void onGlobalLayout() {
        if (!mCollectorThread.isAlive()) {
            mCollectorThread.start();
            mWindow.getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
            mReadyState.decrementAndGet();
        }
    }

    @Override
    public void onCollectorThreadReady() {
        if (0 == this.mReadyState.decrementAndGet()) {
            initHandler();
            postOnAutomate();
        }
    }

    @Override
    protected void onLooperPrepared() {
        if (0 == this.mReadyState.decrementAndGet()) {
            initHandler();
            postOnAutomate();
        }
    }

    @Override
    public void onPostInteraction(List<FrameMetrics> stats) {
        Message m = mHandler.obtainMessage(AutomatorHandler.MSG_ON_POST_INTERACTION, stats);
        mHandler.sendMessage(m);
    }

    protected void postOnAutomate() {
        final Message msg = mHandler.obtainMessage(AutomatorHandler.MSG_ON_AUTOMATE);
        mHandler.sendMessage(msg);
    }

    public void cancel() {
        mHandler.removeMessages(AutomatorHandler.MSG_NEXT_INTERACTION);
        mHandler.cancel();
        mHandler = null;
    }

    public Automator(String testName, int runId, int iteration,
                     Window window, AutomateCallback callback) {
        super("AutomatorThread");

        mTestName = testName;
        mRunId = runId;
        mIteration = iteration;
        mCallback = callback;
        mWindow = window;
        mWindow.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);
        mCollectorThread = new CollectorThread(this);
        mReadyState = new AtomicInteger(PRE_READY_STATE_COUNT);
    }
}

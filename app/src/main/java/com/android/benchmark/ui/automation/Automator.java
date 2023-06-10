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
        public void onPostInteraction(final List<FrameMetrics> metrics) {}
        public void onPostAutomate() {}

        protected final void addInteraction(final Interaction interaction) {
            if (null == mInteractions) {
                return;
            }

            this.mInteractions.add(interaction);
        }

        protected final void setInteractions(final List<Interaction> interactions) {
            this.mInteractions = interactions;
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

        AutomatorHandler(final Looper looper, final Window window, final CollectorThread collectorThread,
                         final AutomateCallback callback, final String testName, final int runId, final int iteration) {
            super(looper);

            this.mInstrumentation = new Instrumentation();

            this.mCallback = callback;
            this.mWindow = window;
            this.mCollectorThread = collectorThread;
            this.mTestName = testName;
            this.mRunId = runId;
            this.mIteration = iteration;
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
            if (this.mCancelled) {
                return;
            }

            if (MSG_NEXT_INTERACTION == msg.what) {
                if (!this.nextInteraction()) {
                    this.stopCollector();
                    this.writeResults();
                    this.mCallback.onPostAutomate();
                }
            } else if (MSG_ON_AUTOMATE == msg.what) {
                this.mCollectorThread.attachToWindow(this.mWindow);
                this.mCallback.setInteractions(this.mInteractions);
                this.mCallback.onAutomate();
                this.postNextInteraction();
            } else if (MSG_ON_POST_INTERACTION == msg.what) {
                final List<FrameMetrics> collectedStats = (List<FrameMetrics>)msg.obj;
                this.persistResults(collectedStats);
                this.mCallback.onPostInteraction(collectedStats);
                this.postNextInteraction();
            }
        }

        public void cancel() {
            this.mCancelled = true;
            this.stopCollector();
        }

        private void stopCollector() {
            this.mCollectorThread.quitCollector();
        }

        private boolean nextInteraction() {

            final Interaction interaction = this.mInteractions.poll();
            if (null != interaction) {
                this.doInteraction(interaction);
                return true;
            }
            return false;
        }

        private void doInteraction(@NonNull final Interaction interaction) {
            if (this.mCancelled) {
                return;
            }

            this.mCollectorThread.markInteractionStart();

            if (Interaction.Type.KEY_EVENT == interaction.getType()) {
                for (final int code : interaction.getKeyCodes()) {
                    if (!this.mCancelled) {
                        this.mInstrumentation.sendKeyDownUpSync(code);
                    } else {
                        break;
                    }
                }
            } else {
                for (final MotionEvent event : interaction.getEvents()) {
                    if (!this.mCancelled) {
                        this.mInstrumentation.sendPointerSync(event);
                    } else {
                        break;
                    }
                }
            }
        }

        private void postNextInteraction() {
            Message msg = this.obtainMessage(MSG_NEXT_INTERACTION);
            this.sendMessage(msg);
        }

        private void persistResults(@NonNull final List<FrameMetrics> stats) {
            if (stats.isEmpty()) {
                return;
            }

            if (null == mResults) {
                final float refresh_rate = Automator.getFrameRate(this.mWindow.getContext());
                this.mResults = new UiBenchmarkResult(stats, (int) refresh_rate);
            } else {
                this.mResults.update(stats);
            }
        }

        private void writeResults() {
            final float refresh_rate = Automator.getFrameRate(this.mWindow.getContext());

            GlobalResultsStore.getInstance(this.mWindow.getContext())
                    .storeRunResults(this.mTestName, this.mRunId, this.mIteration, this.mResults, refresh_rate);
        }
    }

    private static float getFrameRate(@NonNull final Context context) {
        DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        final Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        return display.getRefreshRate();
    }

    private void initHandler() {
        this.mHandler = new AutomatorHandler(this.getLooper(), this.mWindow, this.mCollectorThread, this.mCallback,
                this.mTestName, this.mRunId, this.mIteration);
        this.mWindow = null;
        this.mCallback = null;
        this.mCollectorThread = null;
        this.mTestName = null;
        this.mRunId = 0;
        this.mIteration = 0;
    }

    @Override
    public final void onGlobalLayout() {
        if (!this.mCollectorThread.isAlive()) {
            this.mCollectorThread.start();
            this.mWindow.getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
            this.mReadyState.decrementAndGet();
        }
    }

    @Override
    public void onCollectorThreadReady() {
        if (0 == mReadyState.decrementAndGet()) {
            this.initHandler();
            this.postOnAutomate();
        }
    }

    @Override
    protected void onLooperPrepared() {
        if (0 == mReadyState.decrementAndGet()) {
            this.initHandler();
            this.postOnAutomate();
        }
    }

    @Override
    public void onPostInteraction(final List<FrameMetrics> stats) {
        final Message m = this.mHandler.obtainMessage(AutomatorHandler.MSG_ON_POST_INTERACTION, stats);
        this.mHandler.sendMessage(m);
    }

    protected void postOnAutomate() {
        Message msg = this.mHandler.obtainMessage(AutomatorHandler.MSG_ON_AUTOMATE);
        this.mHandler.sendMessage(msg);
    }

    public void cancel() {
        this.mHandler.removeMessages(AutomatorHandler.MSG_NEXT_INTERACTION);
        this.mHandler.cancel();
        this.mHandler = null;
    }

    public Automator(final String testName, final int runId, final int iteration,
                     final Window window, final AutomateCallback callback) {
        super("AutomatorThread");

        this.mTestName = testName;
        this.mRunId = runId;
        this.mIteration = iteration;
        this.mCallback = callback;
        this.mWindow = window;
        this.mWindow.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);
        this.mCollectorThread = new CollectorThread(this);
        this.mReadyState = new AtomicInteger(Automator.PRE_READY_STATE_COUNT);
    }
}

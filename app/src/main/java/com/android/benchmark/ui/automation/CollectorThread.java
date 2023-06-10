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

package com.android.benchmark.ui.automation;

import android.annotation.TargetApi;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.view.FrameMetrics;
import android.view.Window;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
final class CollectorThread extends HandlerThread {
    private FrameStatsCollector mCollector;
    private Window mAttachedWindow;
    private final List<FrameMetrics> mFrameTimingStats;
    private long mLastFrameTime;
    private WatchdogHandler mWatchdog;
    private final WeakReference<CollectorListener> mListener;

    private volatile boolean mCollecting;


    interface CollectorListener {
        void onCollectorThreadReady();
        void onPostInteraction(List<FrameMetrics> stats);
    }

    private final class WatchdogHandler extends Handler {
        private static final long SCHEDULE_INTERVAL_MILLIS = 20 * Automator.FRAME_PERIOD_MILLIS;

        private static final int MSG_SCHEDULE = 0;

        @Override
        public void handleMessage(final Message msg) {
            if (!CollectorThread.this.mCollecting) {
                return;
            }

            final long currentTime = SystemClock.uptimeMillis();
            if (CollectorThread.this.mLastFrameTime + WatchdogHandler.SCHEDULE_INTERVAL_MILLIS <= currentTime) {
                // haven't seen a frame in a while, interaction is probably done
                CollectorThread.this.mCollecting = false;
                final CollectorListener listener = CollectorThread.this.mListener.get();
                if (null != listener) {
                    listener.onPostInteraction(CollectorThread.this.mFrameTimingStats);
                }
            } else {
                this.schedule();
            }
        }

        public void schedule() {
            this.sendMessageDelayed(this.obtainMessage(WatchdogHandler.MSG_SCHEDULE), WatchdogHandler.SCHEDULE_INTERVAL_MILLIS);
        }

        public void deschedule() {
            this.removeMessages(WatchdogHandler.MSG_SCHEDULE);
        }
    }

    static boolean tripleBuffered;
    static int janks;
    static int total;
    @TargetApi(24)
    private class FrameStatsCollector implements Window.OnFrameMetricsAvailableListener {
        @Override
        public void onFrameMetricsAvailable(final Window window, final FrameMetrics frameMetrics, final int dropCount) {
            if (!CollectorThread.this.mCollecting) {
                return;
            }
            CollectorThread.this.mFrameTimingStats.add(new FrameMetrics(frameMetrics));
            CollectorThread.this.mLastFrameTime = SystemClock.uptimeMillis();
        }
    }

    public CollectorThread(final CollectorListener listener) {
        super("FrameStatsCollectorThread");
        this.mFrameTimingStats = new LinkedList<>();
        this.mListener = new WeakReference<>(listener);
    }

    @TargetApi(24)
    public void attachToWindow(final Window window) {
        if (null != mAttachedWindow) {
            this.mAttachedWindow.removeOnFrameMetricsAvailableListener(this.mCollector);
        }

        this.mAttachedWindow = window;
        window.addOnFrameMetricsAvailableListener(this.mCollector, new Handler(this.getLooper()));
    }

    @TargetApi(24)
    public synchronized void detachFromWindow() {
        if (null != mAttachedWindow) {
            this.mAttachedWindow.removeOnFrameMetricsAvailableListener(this.mCollector);
        }

        this.mAttachedWindow = null;
    }

    @TargetApi(24)
    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        this.mCollector = new FrameStatsCollector();
        this.mWatchdog = new WatchdogHandler();

        final CollectorListener listener = this.mListener.get();
        if (null != listener) {
            listener.onCollectorThreadReady();
        }
    }

    public boolean quitCollector() {
        this.stopCollecting();
        this.detachFromWindow();
        System.out.println("Jank Percentage: " + (100 * CollectorThread.janks / (double) CollectorThread.total) + "%");
        CollectorThread.tripleBuffered = false;
        CollectorThread.total = 0;
        CollectorThread.janks = 0;
        return this.quit();
    }

    void stopCollecting() {
        if (!this.mCollecting) {
            return;
        }

        this.mCollecting = false;
        this.mWatchdog.deschedule();


    }

    public void markInteractionStart() {
        this.mLastFrameTime = 0;
        this.mFrameTimingStats.clear();
        this.mCollecting = true;

        this.mWatchdog.schedule();
    }
}

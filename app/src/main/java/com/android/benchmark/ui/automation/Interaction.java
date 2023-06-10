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

package com.android.benchmark.ui.automation;

import android.os.SystemClock;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Encodes a UI interaction as a series of MotionEvents
 */
public class Interaction {
    private static final int STEP_COUNT = 20;
    // TODO: scale to device display density
    private static final int DEFAULT_FLING_SIZE_PX = 500;
    private static final int DEFAULT_FLING_DURATION_MS = 20;
    private static final int DEFAULT_TAP_DURATION_MS = 20;
    private List<MotionEvent> mEvents;

    // Interaction parameters
    @Nullable
    private final float[] mXPositions;
    @Nullable
    private final float[] mYPositions;
    private final long mDuration;
    @Nullable
    private final int[] mKeyCodes;
    @Type
    private final int mType;

    @IntDef({
            Interaction.Type.TAP,
            Interaction.Type.FLING,
            Interaction.Type.PINCH,
            Interaction.Type.KEY_EVENT})
    public @interface Type {
        int TAP = 0;
        int FLING = 1;
        int PINCH = 2;
        int KEY_EVENT = 3;
    }

    @NonNull
    public static Interaction newFling(final float startX, final float startY,
                                       final float endX, final float endY, final long duration) {
       return new Interaction(Interaction.Type.FLING, new float[]{startX, endX},
               new float[]{startY, endY}, duration);
    }

    @NonNull
    public static Interaction newFlingDown(final float startX, final float startY) {
        return new Interaction(Interaction.Type.FLING,
                new float[]{startX, startX},
                new float[]{startY, startY + Interaction.DEFAULT_FLING_SIZE_PX}, Interaction.DEFAULT_FLING_DURATION_MS);
    }

    @NonNull
    public static Interaction newFlingUp(final float startX, final float startY) {
        return new Interaction(Interaction.Type.FLING,
                new float[]{startX, startX}, new float[]{startY, startY - Interaction.DEFAULT_FLING_SIZE_PX},
                Interaction.DEFAULT_FLING_DURATION_MS);
    }

    @NonNull
    public static Interaction newTap(final float startX, final float startY) {
        return new Interaction(Interaction.Type.TAP,
                new float[]{startX, startX}, new float[]{startY, startY},
                Interaction.DEFAULT_FLING_DURATION_MS);
    }

    @NonNull
    public static Interaction newKeyInput(final int[] keyCodes) {
        return new Interaction(keyCodes);
    }

    public List<MotionEvent> getEvents() {
        if (Type.FLING == mType) {
            this.mEvents = Interaction.createInterpolatedEventList(this.mXPositions, this.mYPositions, this.mDuration);
        } else if (Type.TAP == mType) {
            this.mEvents = Interaction.createInterpolatedEventList(this.mXPositions, this.mYPositions, this.mDuration);
        } else if (Type.PINCH == mType) {
        }

        return this.mEvents;
    }

    public int getType() {
        return this.mType;
    }

    public int[] getKeyCodes() {
        return this.mKeyCodes;
    }

    @NonNull
    private static List<MotionEvent> createInterpolatedEventList(
            @NonNull final float[] xPos, @NonNull final float[] yPos, final long duration) {
        final long startTime = SystemClock.uptimeMillis() + 100;
        final List<MotionEvent> result = new ArrayList<>();

        float startX = xPos[0];
        float startY = yPos[0];

        final MotionEvent downEvent = MotionEvent.obtain(
                startTime, startTime, MotionEvent.ACTION_DOWN, startX, startY, 0);
        result.add(downEvent);

        for (int i = 1; i < xPos.length; i++) {
            final float endX = xPos[i];
            final float endY = yPos[i];
            final float stepX = (endX - startX) / Interaction.STEP_COUNT;
            final float stepY = (endY - startY) / Interaction.STEP_COUNT;
            final float stepT = duration / Interaction.STEP_COUNT;

            for (int j = 0; STEP_COUNT > j; j++) {
                final long deltaT = Math.round(j * stepT);
                final long deltaX = Math.round(j * stepX);
                final long deltaY = Math.round(j * stepY);
                final MotionEvent moveEvent = MotionEvent.obtain(startTime, startTime + deltaT,
                        MotionEvent.ACTION_MOVE, startX + deltaX, startY + deltaY, 0);
                result.add(moveEvent);
            }

            startX = endX;
            startY = endY;
        }

        final float lastX = xPos[xPos.length - 1];
        final float lastY = yPos[yPos.length - 1];
        final MotionEvent lastEvent = MotionEvent.obtain(startTime, startTime + duration,
                MotionEvent.ACTION_UP, lastX, lastY, 0);
        result.add(lastEvent);

        return result;
    }

    private Interaction(@Interaction.Type final int type,
                        final float[] xPos, final float[] yPos, final long duration) {
        this.mType = type;
        this.mXPositions = xPos;
        this.mYPositions = yPos;
        this.mDuration = duration;
        this.mKeyCodes = null;
    }

    private Interaction(final int[] codes) {
        this.mKeyCodes = codes;
        this.mType = Type.KEY_EVENT;
        this.mYPositions = null;
        this.mXPositions = null;
        this.mDuration = 0;
    }

    private Interaction(@Interaction.Type final int type,
                        @NonNull final List<Float> xPositions, @NonNull final List<Float> yPositions, final long duration) {
        if (xPositions.size() != yPositions.size()) {
            throw new IllegalArgumentException("must have equal number of x and y positions");
        }

        int current = 0;
        this.mXPositions = new float[xPositions.size()];
        for (final float p : xPositions) {
            this.mXPositions[current] = p;
            current++;
        }

        current = 0;
        this.mYPositions = new float[yPositions.size()];
        for (final float p : xPositions) {
            this.mXPositions[current] = p;
            current++;
        }

        this.mType = type;
        this.mDuration = duration;
        this.mKeyCodes = null;
    }
}

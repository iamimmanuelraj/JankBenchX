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

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.android.benchmark.R;
import com.android.benchmark.registry.BenchmarkRegistry;
import com.android.benchmark.ui.automation.Automator;
import com.android.benchmark.ui.automation.Interaction;

/**
 *
 */
public class BitmapUploadActivity extends AppCompatActivity {
    private Automator mAutomator;

    public static class UploadView extends View {
        private int mColorValue;
        private Bitmap mBitmap;
        private final DisplayMetrics mMetrics = new DisplayMetrics();
        private final Rect mRect = new Rect();

        public UploadView(final Context context, final AttributeSet attrs) {
            super(context, attrs);
        }

        @SuppressWarnings("unused")
        public void setColorValue(final int colorValue) {
            if (colorValue == this.mColorValue) return;

            this.mColorValue = colorValue;

            // modify the bitmap's color to ensure it's uploaded to the GPU
            this.mBitmap.eraseColor(Color.rgb(this.mColorValue, 255 - this.mColorValue, 255));

            this.invalidate();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();

            this.getDisplay().getMetrics(this.mMetrics);
            final int minDisplayDimen = Math.min(this.mMetrics.widthPixels, this.mMetrics.heightPixels);
            final int bitmapSize = Math.min((int) (minDisplayDimen * 0.75), 720);
            if (null == mBitmap
                    || this.mBitmap.getWidth() != bitmapSize
                    || this.mBitmap.getHeight() != bitmapSize) {
                this.mBitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
            }
        }

        @Override
        protected void onDraw(@NonNull final Canvas canvas) {
            if (null != mBitmap) {
                this.mRect.set(0, 0, this.getWidth(), this.getHeight());
                canvas.drawBitmap(this.mBitmap, null, this.mRect, null);
            }
        }

        @Override
        public boolean onTouchEvent(final MotionEvent event) {
            // animate color to force bitmap uploads
            return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_bitmap_upload);

        View uploadRoot = this.findViewById(R.id.upload_root);
        uploadRoot.setKeepScreenOn(true);
        uploadRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent motionEvent) {
                final UploadView uploadView = BitmapUploadActivity.this.findViewById(R.id.upload_view);
                final ObjectAnimator colorValueAnimator =
                        ObjectAnimator.ofInt(uploadView, "colorValue", 0, 255);
                colorValueAnimator.setRepeatMode(ValueAnimator.REVERSE);
                colorValueAnimator.setRepeatCount(100);
                colorValueAnimator.start();

                // animate scene root to guarantee there's a minimum amount of GPU rendering work
                final ObjectAnimator yAnimator = ObjectAnimator.ofFloat(
                        view, "translationY", 0, 100);
                yAnimator.setRepeatMode(ValueAnimator.REVERSE);
                yAnimator.setRepeatCount(100);
                yAnimator.start();

                return true;
            }
        });

        UploadView uploadView = this.findViewById(R.id.upload_view);
        int runId = this.getIntent().getIntExtra("com.android.benchmark.RUN_ID", 0);
        int iteration = this.getIntent().getIntExtra("com.android.benchmark.ITERATION", -1);

        final String name = BenchmarkRegistry.getBenchmarkName(this, R.id.benchmark_bitmap_upload);

        this.mAutomator = new Automator(name, runId, iteration, this.getWindow(),
                new Automator.AutomateCallback() {
                    @Override
                    public void onPostAutomate() {
                        final Intent result = new Intent();
                        BitmapUploadActivity.this.setResult(Activity.RESULT_OK, result);
                        BitmapUploadActivity.this.finish();
                    }

                    @Override
                    public void onAutomate() {
                        final int[] coordinates = new int[2];
                        uploadRoot.getLocationOnScreen(coordinates);

                        final int x = coordinates[0];
                        final int y = coordinates[1];

                        final float width = uploadRoot.getWidth();
                        final float height = uploadRoot.getHeight();

                        final float middleX = (x + width) / 5;
                        final float middleY = (y + height) / 5;

                        this.addInteraction(Interaction.newTap(middleX, middleY));
                    }
                });

        this.mAutomator.start();
    }

}

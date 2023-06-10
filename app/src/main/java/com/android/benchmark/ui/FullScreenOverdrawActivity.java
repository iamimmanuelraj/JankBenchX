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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.android.benchmark.R;
import com.android.benchmark.registry.BenchmarkRegistry;
import com.android.benchmark.ui.automation.Automator;
import com.android.benchmark.ui.automation.Interaction;

public class FullScreenOverdrawActivity extends AppCompatActivity {

    private Automator mAutomator;

    private class OverdrawView extends View {
        @NonNull
        Paint paint = new Paint();
        int mColorValue;

        public OverdrawView(final Context context) {
            super(context);
        }

        @Keep
        @SuppressWarnings("unused")
        public void setColorValue(final int colorValue) {
            this.mColorValue = colorValue;
            this.invalidate();
        }

        @Override
        public boolean onTouchEvent(final MotionEvent event) {
            final ObjectAnimator objectAnimator = ObjectAnimator.ofInt(this, "colorValue", 0, 255);
            objectAnimator.setRepeatMode(ValueAnimator.REVERSE);
            objectAnimator.setRepeatCount(100);
            objectAnimator.start();
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(@NonNull final Canvas canvas) {
            this.paint.setColor(Color.rgb(this.mColorValue, 255 - this.mColorValue, 255));

            for (int i = 0; 10 > i; i++) {
                canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), this.paint);
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OverdrawView overdrawView = new OverdrawView(this);
        overdrawView.setKeepScreenOn(true);
        this.setContentView(overdrawView);

        int runId = this.getIntent().getIntExtra("com.android.benchmark.RUN_ID", 0);
        int iteration = this.getIntent().getIntExtra("com.android.benchmark.ITERATION", -1);

        final String name = BenchmarkRegistry.getBenchmarkName(this, R.id.benchmark_overdraw);

        this.mAutomator = new Automator(name, runId, iteration, this.getWindow(),
                new Automator.AutomateCallback() {
                    @Override
                    public void onPostAutomate() {
                        final Intent result = new Intent();
                        FullScreenOverdrawActivity.this.setResult(Activity.RESULT_OK, result);
                        FullScreenOverdrawActivity.this.finish();
                    }

                    @Override
                    public void onAutomate() {
                        final int[] coordinates = new int[2];
                        overdrawView.getLocationOnScreen(coordinates);

                        final int x = coordinates[0];
                        final int y = coordinates[1];

                        final float width = overdrawView.getWidth();
                        final float height = overdrawView.getHeight();

                        final float middleX = (x + width) / 5;
                        final float middleY = (y + height) / 5;

                        this.addInteraction(Interaction.newTap(middleX, middleY));
                    }
                });

        this.mAutomator.start();
    }
}

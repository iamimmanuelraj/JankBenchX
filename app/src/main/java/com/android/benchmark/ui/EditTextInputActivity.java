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

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.EditText;

import com.android.benchmark.R;
import com.android.benchmark.ui.automation.Automator;
import com.android.benchmark.ui.automation.Interaction;

public class EditTextInputActivity extends AppCompatActivity {

    private Automator mAutomator;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EditText editText = new EditText(this);
        int runId = this.getIntent().getIntExtra("com.android.benchmark.RUN_ID", 0);
        int iteration = this.getIntent().getIntExtra("com.android.benchmark.ITERATION", -1);

        editText.setWidth(400);
        editText.setHeight(200);
        this.setContentView(editText);

        final String testName = this.getString(R.string.edit_text_input_name);

        final ActionBar actionBar = this.getSupportActionBar();
        if (null != actionBar) {
            actionBar.setTitle(testName);
        }

        this.mAutomator = new Automator(testName, runId, iteration, this.getWindow(),
                new Automator.AutomateCallback() {
            @Override
            public void onPostAutomate() {
                final Intent result = new Intent();
                EditTextInputActivity.this.setResult(Activity.RESULT_OK, result);
                EditTextInputActivity.this.finish();
            }

            @Override
            public void onAutomate() {

                final int[] coordinates = new int[2];
                editText.getLocationOnScreen(coordinates);

                final int x = coordinates[0];
                final int y = coordinates[1];

                final float width = editText.getWidth();
                final float height = editText.getHeight();

                final float middleX = (x + width) / 2;
                final float middleY = (y + height) / 2;

                final Interaction tap = Interaction.newTap(middleX, middleY);
                this.addInteraction(tap);

                final int[] alphabet = {
                        KeyEvent.KEYCODE_A,
                        KeyEvent.KEYCODE_B,
                        KeyEvent.KEYCODE_C,
                        KeyEvent.KEYCODE_D,
                        KeyEvent.KEYCODE_E,
                        KeyEvent.KEYCODE_F,
                        KeyEvent.KEYCODE_G,
                        KeyEvent.KEYCODE_H,
                        KeyEvent.KEYCODE_I,
                        KeyEvent.KEYCODE_J,
                        KeyEvent.KEYCODE_K,
                        KeyEvent.KEYCODE_L,
                        KeyEvent.KEYCODE_M,
                        KeyEvent.KEYCODE_N,
                        KeyEvent.KEYCODE_O,
                        KeyEvent.KEYCODE_P,
                        KeyEvent.KEYCODE_Q,
                        KeyEvent.KEYCODE_R,
                        KeyEvent.KEYCODE_S,
                        KeyEvent.KEYCODE_T,
                        KeyEvent.KEYCODE_U,
                        KeyEvent.KEYCODE_V,
                        KeyEvent.KEYCODE_W,
                        KeyEvent.KEYCODE_X,
                        KeyEvent.KEYCODE_Y,
                        KeyEvent.KEYCODE_Z,
                        KeyEvent.KEYCODE_SPACE
                };
                final Interaction typeAlphabet = Interaction.newKeyInput(new int[] {
                        KeyEvent.KEYCODE_A,
                        KeyEvent.KEYCODE_B,
                        KeyEvent.KEYCODE_C,
                        KeyEvent.KEYCODE_D,
                        KeyEvent.KEYCODE_E,
                        KeyEvent.KEYCODE_F,
                        KeyEvent.KEYCODE_G,
                        KeyEvent.KEYCODE_H,
                        KeyEvent.KEYCODE_I,
                        KeyEvent.KEYCODE_J,
                        KeyEvent.KEYCODE_K,
                        KeyEvent.KEYCODE_L,
                        KeyEvent.KEYCODE_M,
                        KeyEvent.KEYCODE_N,
                        KeyEvent.KEYCODE_O,
                        KeyEvent.KEYCODE_P,
                        KeyEvent.KEYCODE_Q,
                        KeyEvent.KEYCODE_R,
                        KeyEvent.KEYCODE_S,
                        KeyEvent.KEYCODE_T,
                        KeyEvent.KEYCODE_U,
                        KeyEvent.KEYCODE_V,
                        KeyEvent.KEYCODE_W,
                        KeyEvent.KEYCODE_X,
                        KeyEvent.KEYCODE_Y,
                        KeyEvent.KEYCODE_Z,
                        KeyEvent.KEYCODE_SPACE,
                });

                for (int i = 0; 5 > i; i++) {
                    this.addInteraction(typeAlphabet);
                }
            }
        });
        this.mAutomator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mAutomator) {
            this.mAutomator.cancel();
            this.mAutomator = null;
        }
    }

    private String getRunFilename() {
        String builder = this.getClass().getSimpleName() +
                System.currentTimeMillis();
        return builder;
    }
}

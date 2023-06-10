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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import java.util.Random;

public enum Utils {
    ;

    private static final int RANDOM_WORD_LENGTH = 10;

    @NonNull
    public static String getRandomWord(@NonNull final Random random, final int length) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            final char base = random.nextBoolean() ? 'A' : 'a';
            final char nextChar = (char)(random.nextInt(26) + base);
            builder.append(nextChar);
        }
        return builder.toString();
    }

    @NonNull
    public static String[] buildStringList(final int count) {
        final Random random = new Random(0);
        final String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = Utils.getRandomWord(random, Utils.RANDOM_WORD_LENGTH);
        }

        return result;
    }

     // a small number of strings reused frequently, expected to hit
    // in the word-granularity text layout cache
    static final String[] CACHE_HIT_STRINGS = {
            "a",
            "small",
            "number",
            "of",
            "strings",
            "reused",
            "frequently"
    };

    private static final int WORDS_IN_PARAGRAPH = 150;

    // misses are fairly long 'words' to ensure they miss
    private static final int PARAGRAPH_MISS_MIN_LENGTH = 4;
    private static final int PARAGRAPH_MISS_MAX_LENGTH = 9;

    @NonNull
    static String[] buildParagraphListWithHitPercentage(final int paragraphCount, final int hitPercentage) {
        if (0 > hitPercentage || 100 < hitPercentage) throw new IllegalArgumentException();

        final String[] strings = new String[paragraphCount];
        final Random random = new Random(0);
        for (int i = 0; i < strings.length; i++) {
            final StringBuilder result = new StringBuilder();
            for (int word = 0; WORDS_IN_PARAGRAPH > word; word++) {
                if (0 != word) {
                    result.append(" ");
                }
                if (random.nextInt(100) < hitPercentage) {
                    // add a common word, which is very likely to hit in the cache
                    result.append(Utils.CACHE_HIT_STRINGS[random.nextInt(Utils.CACHE_HIT_STRINGS.length)]);
                } else {
                    // construct a random word, which will *most likely* miss
                    int length = Utils.PARAGRAPH_MISS_MIN_LENGTH;
                    length += random.nextInt(Utils.PARAGRAPH_MISS_MAX_LENGTH - Utils.PARAGRAPH_MISS_MIN_LENGTH);

                    result.append(Utils.getRandomWord(random, length));
                }
            }
            strings[i] = result.toString();
        }

        return strings;
    }


    public static int calculateInSampleSize(
            @NonNull final BitmapFactory.Options options, final int reqWidth, final int reqHeight) {
        // Raw height and width of image
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            int halfHeight = height / 2;
            int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(final Resources res, final int resId,
                                                         final int reqWidth, final int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = Utils.calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

}

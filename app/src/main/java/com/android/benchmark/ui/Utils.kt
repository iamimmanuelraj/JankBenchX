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
package com.android.benchmark.ui

android.graphics.Bitmapimport android.graphics.BitmapFactoryimport java.util.Random
enum class Utils {
    ;

    companion object {
        private const val RANDOM_WORD_LENGTH = 10
        fun getRandomWord(random: Random, length: Int): String {
            val builder = StringBuilder()
            for (i in 0 until length) {
                val base = if (random.nextBoolean()) 'A' else 'a'
                val nextChar = (random.nextInt(26) + base.code).toChar()
                builder.append(nextChar)
            }
            return builder.toString()
        }

        fun buildStringList(count: Int): Array<String?> {
            val random = Random(0)
            val result = arrayOfNulls<String>(count)
            for (i in 0 until count) {
                result[i] = getRandomWord(random, RANDOM_WORD_LENGTH)
            }
            return result
        }

        // a small number of strings reused frequently, expected to hit
        // in the word-granularity text layout cache
        val CACHE_HIT_STRINGS = arrayOf(
                "a",
                "small",
                "number",
                "of",
                "strings",
                "reused",
                "frequently"
        )
        private const val WORDS_IN_PARAGRAPH = 150

        // misses are fairly long 'words' to ensure they miss
        private const val PARAGRAPH_MISS_MIN_LENGTH = 4
        private const val PARAGRAPH_MISS_MAX_LENGTH = 9
        fun buildParagraphListWithHitPercentage(paragraphCount: Int, hitPercentage: Int): Array<String?> {
            require(!(0 > hitPercentage || 100 < hitPercentage))
            val strings = arrayOfNulls<String>(paragraphCount)
            val random = Random(0)
            for (i in strings.indices) {
                val result = StringBuilder()
                var word = 0
                while (WORDS_IN_PARAGRAPH > word) {
                    if (0 != word) {
                        result.append(" ")
                    }
                    if (random.nextInt(100) < hitPercentage) {
                        // add a common word, which is very likely to hit in the cache
                        result.append(CACHE_HIT_STRINGS[random.nextInt(CACHE_HIT_STRINGS.size)])
                    } else {
                        // construct a random word, which will *most likely* miss
                        var length = PARAGRAPH_MISS_MIN_LENGTH
                        length += random.nextInt(PARAGRAPH_MISS_MAX_LENGTH - PARAGRAPH_MISS_MIN_LENGTH)
                        result.append(getRandomWord(random, length))
                    }
                    word++
                }
                strings[i] = result.toString()
            }
            return strings
        }

        fun calculateInSampleSize(
                options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight
                        && halfWidth / inSampleSize > reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        fun decodeSampledBitmapFromResource(res: Resources?, resId: Int,
                                            reqWidth: Int, reqHeight: Int): Bitmap {

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(res, resId, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeResource(res, resId, options)
        }
    }
}
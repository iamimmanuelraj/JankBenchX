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

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.benchmark.R;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class ImageListViewScrollActivity extends ListViewScrollActivity {

    private static final int LIST_SIZE = 100;

    private static final int[] IMG_RES_ID = {
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4,
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4,
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4,
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4,
    };

    private static final Bitmap[] mBitmapCache = new Bitmap[ImageListViewScrollActivity.IMG_RES_ID.length];

    private static final String[] WORDS = Utils.buildStringList(ImageListViewScrollActivity.LIST_SIZE);

    private final HashMap<View, BitmapWorkerTask> mInFlight = new HashMap<>();

    @NonNull
    @Override
    protected ListAdapter createListAdapter() {
        return new ImageListAdapter();
    }

    @Override
    protected String getName() {
        return this.getString(R.string.image_list_view_scroll_name);
    }

    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        @NonNull
        private final WeakReference<ImageView> imageViewReference;
        private int data;
        private int cacheIdx;
        volatile boolean cancelled;

        public BitmapWorkerTask(final ImageView imageView, final int cacheIdx) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            this.imageViewReference = new WeakReference<>(imageView);
            this.cacheIdx = cacheIdx;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(@NonNull final Integer... params) {
            this.data = params[0];
            return Utils.decodeSampledBitmapFromResource(ImageListViewScrollActivity.this.getResources(), this.data, 100, 100);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(@Nullable final Bitmap bitmap) {
            if (null != bitmap) {
                ImageView imageView = this.imageViewReference.get();
                if (null != imageView) {
                    if (!this.cancelled) {
                        imageView.setImageBitmap(bitmap);
                    }
                    ImageListViewScrollActivity.mBitmapCache[this.cacheIdx] = bitmap;
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (int i = 0; i < ImageListViewScrollActivity.mBitmapCache.length; i++) {
            ImageListViewScrollActivity.mBitmapCache[i] = null;
        }
    }

    class ImageListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return ImageListViewScrollActivity.LIST_SIZE;
        }

        @Nullable
        @Override
        public Object getItem(final int postition) {
            return null;
        }

        @Override
        public long getItemId(final int postition) {
            return postition;
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, final ViewGroup parent) {
            if (null == convertView) {
                convertView = LayoutInflater.from(ImageListViewScrollActivity.this.getBaseContext())
                        .inflate(R.layout.image_scroll_list_item, parent, false);
            }

            final ImageView imageView = convertView.findViewById(R.id.image_scroll_image);
            final BitmapWorkerTask inFlight = ImageListViewScrollActivity.this.mInFlight.get(convertView);
            if (null != inFlight) {
                inFlight.cancelled = true;
                ImageListViewScrollActivity.this.mInFlight.remove(convertView);
            }

            final int cacheIdx = position % ImageListViewScrollActivity.IMG_RES_ID.length;
            final Bitmap bitmap = ImageListViewScrollActivity.mBitmapCache[(cacheIdx)];
            if (null == bitmap) {
                final BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(imageView, cacheIdx);
                bitmapWorkerTask.execute(ImageListViewScrollActivity.IMG_RES_ID[(cacheIdx)]);
                ImageListViewScrollActivity.this.mInFlight.put(convertView, bitmapWorkerTask);
            }

            imageView.setImageBitmap(bitmap);

            final TextView textView = convertView.findViewById(R.id.image_scroll_text);
            textView.setText(ImageListViewScrollActivity.WORDS[position]);

            return convertView;
        }
    }
}

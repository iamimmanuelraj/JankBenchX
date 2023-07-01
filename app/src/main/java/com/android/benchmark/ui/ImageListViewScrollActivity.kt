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

import android.graphics.Bitmap
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import com.android.benchmark.R
import java.lang.ref.WeakReference

class ImageListViewScrollActivity : ListViewScrollActivity() {
    private val mInFlight = HashMap<View?, BitmapWorkerTask>()
    override fun createListAdapter(): ListAdapter {
        return ImageListAdapter()
    }

    protected override val name: String?
        protected get() = getString(R.string.image_list_view_scroll_name)

    internal inner class BitmapWorkerTask(imageView: ImageView, cacheIdx: Int) : AsyncTask<Int?, Void?, Bitmap?>() {
        private val imageViewReference: WeakReference<ImageView>
        private var data = 0
        private val cacheIdx: Int

        @Volatile
        var cancelled = false

        init {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = WeakReference(imageView)
            this.cacheIdx = cacheIdx
        }

        // Decode image in background.
        protected override fun doInBackground(vararg params: Int): Bitmap {
            data = params[0]
            return Utils.Companion.decodeSampledBitmapFromResource(resources, data, 100, 100)
        }

        // Once complete, see if ImageView is still around and set bitmap.
        override fun onPostExecute(bitmap: Bitmap?) {
            if (null != bitmap) {
                val imageView = imageViewReference.get()
                if (null != imageView) {
                    if (!cancelled) {
                        imageView.setImageBitmap(bitmap)
                    }
                    mBitmapCache[cacheIdx] = bitmap
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        for (i in mBitmapCache.indices) {
            mBitmapCache[i] = null
        }
    }

    internal inner class ImageListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return LIST_SIZE
        }

        override fun getItem(postition: Int): Any? {
            return null
        }

        override fun getItemId(postition: Int): Long {
            return postition.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (null == convertView) {
                convertView = LayoutInflater.from(baseContext)
                        .inflate(R.layout.image_scroll_list_item, parent, false)
            }
            val imageView = convertView!!.findViewById<ImageView>(R.id.image_scroll_image)
            val inFlight = mInFlight[convertView]
            if (null != inFlight) {
                inFlight.cancelled = true
                mInFlight.remove(convertView)
            }
            val cacheIdx = position % IMG_RES_ID.size
            val bitmap = mBitmapCache[cacheIdx]
            if (null == bitmap) {
                val bitmapWorkerTask = BitmapWorkerTask(imageView, cacheIdx)
                bitmapWorkerTask.execute(IMG_RES_ID[cacheIdx])
                mInFlight[convertView] = bitmapWorkerTask
            }
            imageView.setImageBitmap(bitmap)
            val textView = convertView.findViewById<TextView>(R.id.image_scroll_text)
            textView.text = WORDS[position]
            return convertView
        }
    }

    companion object {
        private const val LIST_SIZE = 100
        private val IMG_RES_ID = intArrayOf(
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
                R.drawable.img4)
        private val mBitmapCache = arrayOfNulls<Bitmap>(IMG_RES_ID.size)
        private val WORDS: Array<String?> = Utils.Companion.buildStringList(LIST_SIZE)
    }
}
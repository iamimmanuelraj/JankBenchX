/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.benchmark.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.android.benchmark.R;


/**
 * TODO: document your custom view class.
 */
public class PerfTimeline extends View {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 300; // TODO: use a default from R.dimen...

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private Paint mPaintBaseLow;
    private Paint mPaintBaseHigh;
    private Paint mPaintValue;


    public float[] mLinesLow;
    public float[] mLinesHigh;
    public float[] mLinesValue;

    public PerfTimeline(final Context context) {
        super(context);
        this.init(null, 0);
    }

    public PerfTimeline(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.init(attrs, 0);
    }

    public PerfTimeline(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.init(attrs, defStyle);
    }

    private void init(final AttributeSet attrs, final int defStyle) {
        // Load attributes
        TypedArray a = this.getContext().obtainStyledAttributes(
                attrs, R.styleable.PerfTimeline, defStyle, 0);

        this.mExampleString = "xx";//a.getString(R.styleable.PerfTimeline_exampleString, "xx");
        this.mExampleColor = a.getColor(R.styleable.PerfTimeline_exampleColor, this.mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        this.mExampleDimension = a.getDimension(
                R.styleable.PerfTimeline_exampleDimension,
                this.mExampleDimension);

        a.recycle();

        // Set up a default TextPaint object
        this.mTextPaint = new TextPaint();
        this.mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        this.mTextPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        this.invalidateTextPaintAndMeasurements();

        this.mPaintBaseLow = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPaintBaseLow.setStyle(Paint.Style.FILL);
        this.mPaintBaseLow.setColor(0xff000000);

        this.mPaintBaseHigh = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPaintBaseHigh.setStyle(Paint.Style.FILL);
        this.mPaintBaseHigh.setColor(0x7f7f7f7f);

        this.mPaintValue = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPaintValue.setStyle(Paint.Style.FILL);
        this.mPaintValue.setColor(0x7fff0000);

    }

    private void invalidateTextPaintAndMeasurements() {
        this.mTextPaint.setTextSize(this.mExampleDimension);
        this.mTextPaint.setColor(this.mExampleColor);
        this.mTextWidth = this.mTextPaint.measureText(this.mExampleString);

        final Paint.FontMetrics fontMetrics = this.mTextPaint.getFontMetrics();
        this.mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        final int paddingLeft = this.getPaddingLeft();
        final int paddingTop = this.getPaddingTop();
        final int paddingRight = this.getPaddingRight();
        final int paddingBottom = this.getPaddingBottom();

        final int contentWidth = this.getWidth() - paddingLeft - paddingRight;
        final int contentHeight = this.getHeight() - paddingTop - paddingBottom;

        // Draw the text.
        //canvas.drawText(mExampleString,
        //        paddingLeft + (contentWidth - mTextWidth) / 2,
        //        paddingTop + (contentHeight + mTextHeight) / 2,
        //        mTextPaint);




        // Draw the shadow
        //RectF rf = new RectF(10.f, 10.f, 100.f, 100.f);
        //canvas.drawOval(rf, mShadowPaint);

        if (null != mLinesLow) {
            canvas.drawLines(this.mLinesLow, this.mPaintBaseLow);
        }
        if (null != mLinesHigh) {
            canvas.drawLines(this.mLinesHigh, this.mPaintBaseHigh);
        }
        if (null != mLinesValue) {
            canvas.drawLines(this.mLinesValue, this.mPaintValue);
        }


/*
        // Draw the pie slices
        for (int i = 0; i < mData.size(); ++i) {
            Item it = mData.get(i);
            mPiePaint.setShader(it.mShader);
            canvas.drawArc(mBounds,
                    360 - it.mEndAngle,
                    it.mEndAngle - it.mStartAngle,
                    true, mPiePaint);
        }
*/
        // Draw the pointer
        //canvas.drawLine(mTextX, mPointerY, mPointerX, mPointerY, mTextPaint);
        //canvas.drawCircle(mPointerX, mPointerY, mPointerSize, mTextPaint);
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return this.mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(final String exampleString) {
        this.mExampleString = exampleString;
        this.invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return this.mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(final int exampleColor) {
        this.mExampleColor = exampleColor;
        this.invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return this.mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(final float exampleDimension) {
        this.mExampleDimension = exampleDimension;
        this.invalidateTextPaintAndMeasurements();
    }
}

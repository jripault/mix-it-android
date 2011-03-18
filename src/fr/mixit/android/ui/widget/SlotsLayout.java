/*
 * Copyright 2010 Google Inc.
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

/*
 * Modified by Peter Kuterna to support dynamic number of columns.
 */
package fr.mixit.android.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import fr.mixit.android.R;
import fr.mixit.android.utils.UIUtils;

/**
 * Custom layout that contains and organizes a {@link TimeRulerView} and several
 * instances of {@link SlotView}. Also positions current "now" divider using
 * {@link R.id#slots_now} view when applicable.
 */
public class SlotsLayout extends ViewGroup {

    private TimeRulerView mRulerView;
    private View mNowView;
    private SparseIntArray columnArray = new SparseIntArray();

    public SlotsLayout(Context context) {
        this(context, null);
    }

    public SlotsLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlotsLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SlotsLayout, defStyle, 0);

        a.recycle();
    }

    private void ensureChildren() {
        mRulerView = (TimeRulerView) findViewById(R.id.slots_ruler);
        mRulerView.setDrawingCacheEnabled(true);
        if (mRulerView == null) {
            throw new IllegalStateException("Must include a R.id.slots_ruler view.");
        }

        mNowView = findViewById(R.id.slots_now);
        mNowView.setDrawingCacheEnabled(true);
        if (mNowView == null) {
            throw new IllegalStateException("Must include a R.id.slots_now view.");
        }
    }
    
	/**
     * Remove any {@link SlotView} instances, leaving only
     * {@link TimeRulerView} remaining.
     */
    public void removeAllSlots() {
        ensureChildren();
        removeAllViews();
        addView(mRulerView);
        addView(mNowView);
        columnArray.clear();
    }

    public void addSlot(SlotView slotView) {
        slotView.setDrawingCacheEnabled(true);
        final int columnIndex = slotView.getColumn();
        int curEntries = columnArray.get(columnIndex + 1, 0);
        columnArray.put(columnIndex, ++curEntries);
        addView(slotView, 1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ensureChildren();

        mRulerView.measure(widthMeasureSpec, heightMeasureSpec);
        mNowView.measure(widthMeasureSpec, heightMeasureSpec);

        final int width = mRulerView.getMeasuredWidth();
        final int height = mRulerView.getMeasuredHeight();

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ensureChildren();

        final TimeRulerView rulerView = mRulerView;
        final int nrColumns = getColumns();
        final int headerWidth = rulerView.getHeaderWidth();
	    // TODO : be independent pixels
        final int columnWidth = (getWidth() - headerWidth - UIUtils.dipToPixel(getContext(), 66)) / nrColumns;//30 - 60 - 100 - 150

        rulerView.layout(0, 0, getWidth(), getHeight());

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            if (child instanceof SlotView) {
                final SlotView slotView = (SlotView) child;
                final int columnIndex = getColumnIndex(slotView);
                final int top = rulerView.getTimeVerticalOffset(slotView.getStartTime());
                final int bottom = rulerView.getTimeVerticalOffset(slotView.getEndTime());
                final int left = headerWidth + (columnIndex * columnWidth) + UIUtils.dipToPixel(getContext(), 33);//((columnIndex + 1) * (30 / nrColumns+1)); //15 - 30 - 50 - 75
                final int right = left + columnWidth;
                child.layout(left, top, right, bottom);
            }
        }

        // Align now view to match current time
        final View nowView = mNowView;
        final long now = System.currentTimeMillis();

        final int top = rulerView.getTimeVerticalOffset(now);
        final int bottom = top + nowView.getMeasuredHeight();
        final int left = 0;
        final int right = getWidth();

        nowView.layout(left, top, right, bottom);
    }
    
    private int getColumns() {
    	return Math.max(1, columnArray.size());
    }
    
    private int getColumnIndex(SlotView slotView) {
    	final int column = slotView.getColumn();
    	final int diff = columnArray.keyAt(columnArray.indexOfKey(column)) - columnArray.indexOfKey(column);
    	return column - diff;
    }
    
}

/*
 * Copyright 2018 Marco Stornelli
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 * A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.balda.smartrecyclerview.touchhelper;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

@SuppressWarnings("unused")
public class TouchHelperCallback extends ItemTouchHelper.SimpleCallback {

    protected ItemTouchHelperAdapter adapter;
    private boolean isMoving;
    private MoveTracking moveTracking;
    private boolean dragEnabled;

    private static class MoveTracking {
        int start;
        int end;
    }

    public TouchHelperCallback(ItemTouchHelperAdapter adapter) {
        super(0, 0);
        this.adapter = adapter;
    }

    /**
     * By default the helper allows swipe and drag up/down. Override if needed.
     * @param recyclerView The view
     * @param viewHolder The view holder
     * @return Movement flags
     */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    /**
     * Enable or disable drag temporarelly
     * @param v True to enable, false otherwise
     */
    public void enableDrag(boolean v) {
        dragEnabled = v;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder
            target) {
        if (isMoving && dragEnabled) {
            if (moveTracking == null) {
                moveTracking = new MoveTracking();
                moveTracking.start = viewHolder.getAdapterPosition();
            } else {
                moveTracking.end = target.getAdapterPosition();
            }
        }
        return dragEnabled;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // We only want the active item
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            isMoving = true;
            if (viewHolder instanceof ItemTouchHelperViewHolder) {
                ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (isMoving) {
            isMoving = false;
            if (moveTracking != null && moveTracking.start != moveTracking.end)
                adapter.onItemMove(moveTracking.start, moveTracking.end);
            moveTracking = null;
        }
        if (viewHolder instanceof ItemTouchHelperViewHolder) {
            ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
            itemViewHolder.onItemClear();
        }
    }
}

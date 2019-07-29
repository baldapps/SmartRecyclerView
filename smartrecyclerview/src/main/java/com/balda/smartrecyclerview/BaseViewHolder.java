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
package com.balda.smartrecyclerview;

import android.annotation.SuppressLint;
import android.graphics.Color;
import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Checkable;

import com.balda.smartrecyclerview.touchhelper.ItemTouchHelperViewHolder;

/**
 * Base view holder class. Extend this class to customize the behavior
 * and keep references of your views.
 */
@SuppressWarnings("unused")
public abstract class BaseViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

    protected final RecyclerArrayAdapter adapter;

    public BaseViewHolder(RecyclerArrayAdapter adapter, View itemView) {
        super(itemView);
        this.adapter = adapter;
        init();
    }

    public BaseViewHolder(RecyclerArrayAdapter recViewAdapter, View itemView, @IdRes int dragViewId) {
        super(itemView);
        this.adapter = recViewAdapter;
        View v = itemView.findViewById(dragViewId);
        v.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (adapter != null)
                        adapter.onStartDrag(BaseViewHolder.this);
                }
                return false;
            }
        });
        init();
    }

    private void init() {
        if (isChoiceModeActive() || isChoiceModeModal()) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isChoiceModeActive()) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && adapter.getCheckableList() != null) {
                            adapter.getCheckableList().toggleItemChecked(position, false);
                            updateCheckedState(position);
                        }
                    }
                }
            });
        }
        if (isChoiceModeModal()) {
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (adapter.getCheckableList() == null || isChoiceModeActive()) {
                        return false;
                    }
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        adapter.getCheckableList().setItemChecked(position, true, false);
                        updateCheckedState(position);
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void onItemSelected() {
        if (!isChoiceModeActive())
            itemView.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onItemClear() {
        if (!isChoiceModeActive())
            itemView.setBackgroundColor(0);
    }

    void bindViewHolder(int position) {
        updateCheckedState(position);
        onBind(position);
    }

    protected abstract void onBind(int position);

    protected void updateCheckedState(int position) {
        if (adapter.getCheckableList() != null) {
            final boolean isChecked = adapter.getCheckableList().isItemChecked(position);
            if (itemView instanceof Checkable) {
                ((Checkable) itemView).setChecked(isChecked);
            } else {
                itemView.setActivated(isChecked);
            }
        }
    }

    protected boolean isChoiceModeActive() {
        return adapter.getCheckableList() != null && adapter.getCheckableList()
                .getChoiceMode() != CheckableList.NONE && (adapter.getCheckableList()
                .getChoiceMode() != CheckableList.MULTI_MODAL || adapter.getCheckableList().getCheckedItemCount() > 0);
    }

    protected boolean isChoiceModeModal() {
        return adapter.getCheckableList() != null && adapter.getCheckableList().getChoiceMode() == CheckableList.MULTI_MODAL;
    }
}

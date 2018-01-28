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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;

import com.balda.smartrecyclerview.touchhelper.ItemTouchHelperAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base recycler view adapter
 * @param <T> The content of array list
 * @param <VH> The view holder
 */
@SuppressWarnings("unused")
public abstract class RecyclerArrayAdapter<T, VH extends BaseViewHolder> extends RecyclerView.Adapter<VH>
        implements DragListener, ItemTouchHelperAdapter {

    private Context context;
    protected ArrayList<T> mObjects;
    @Nullable
    protected DragListener dragListener;
    @Nullable
    private CheckableList checkableList;

    public RecyclerArrayAdapter(@NonNull Context c, final ArrayList<T> objects) {
        mObjects = objects;
        context = c;
    }

    public RecyclerArrayAdapter(@NonNull Context c, final T[] objects) {
        mObjects = new ArrayList<>(Arrays.asList(objects));
        context = c;
    }

    public RecyclerArrayAdapter(@NonNull Context c) {
        context = c;
        mObjects = new ArrayList<>();
    }

    @Nullable
    public CheckableList getCheckableList() {
        return checkableList;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        checkableList = (CheckableList) recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        checkableList = null;
    }

    public void setDragListener(@Nullable DragListener listener) {
        dragListener = listener;
    }

    public Context getContext() {
        return context;
    }

    public void add(final T object) {
        mObjects.add(object);
        notifyItemInserted(getItemCount() - 1);
    }

    public void add(final Collection<? extends T> collection) {
        mObjects.addAll(collection);
        notifyItemInserted(getItemCount() - collection.size());
    }

    public void clear() {
        final int size = getItemCount();
        mObjects.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Utility method to reload a new dataset. It checks if items are new
     * or modified or deleted to update the views. To define custom criteria,
     * override {@see getDiffUtils} and extend DiffCallback class.
     * @param collection The new dataset
     */
    public void refresh(List<T> collection) {
        final DiffUtil.Callback diffCallback = getDiffUtil(mObjects, collection);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        mObjects.clear();
        mObjects.addAll(collection);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * By default it returns a DiffCallback class. Override if needed.
     * @param objects The old list
     * @param collection The new list
     * @return A DiffUtil.Callback object
     */
    protected DiffUtil.Callback getDiffUtil(ArrayList<T> objects, List<T> collection) {
        return new DiffCallback(objects, collection);
    }

    @Override
    public int getItemCount() {
        return mObjects.size();
    }

    public T getItem(final int position) {
        return mObjects.get(position);
    }

    public long getItemId(final int position) {
        return position;
    }

    public int getPosition(final T item) {
        return mObjects.indexOf(item);
    }

    public void insert(final T object, int index) {
        mObjects.add(index, object);
        notifyItemInserted(index);
    }

    public void remove(T object) {
        final int position = getPosition(object);
        mObjects.remove(object);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mObjects.size());
    }

    public void sort(Comparator<? super T> comparator) {
        Collections.sort(mObjects, comparator);
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mObjects, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mObjects, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        mObjects.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (dragListener != null)
            dragListener.onStartDrag(viewHolder);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bindChoiceState(position);
    }

    @Override
    public void onBindViewHolder(VH holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        holder.bindChoiceState(position);
    }

    /**
     * Very basic class to use as diffutil callback. It just uses
     * the method equals to compare objects (both id and contents).
     */
    protected class DiffCallback extends DiffUtil.Callback {

        private final List<T> mOldList;
        private final List<T> mNewList;

        public DiffCallback(List<T> oldCollection, List<T> collection) {
            this.mOldList = oldCollection;
            this.mNewList = collection;
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).equals(mNewList.get(
                    newItemPosition).toString());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }
    }
}
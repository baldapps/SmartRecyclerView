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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Filter;
import android.widget.Filterable;

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
        implements DragListener, ItemTouchHelperAdapter, Filterable {

    /**
     * Lock used to modify the content of {@link #objects}. Any write operation
     * performed on the array should be synchronized on this lock. This lock is also
     * used by the filter (see {@link #getFilter()} to make a synchronized copy of
     * the original array of data.
     */
    protected final Object lock = new Object();
    private Context context;
    protected List<T> objects;
    @Nullable
    protected DragListener dragListener;
    @Nullable
    private CheckableList checkableList;
    /**
     * A copy of the original objects array, initialized from and then used instead as soon as
     * the filter ArrayFilter is used. objects will then only contain the filtered values.
     */
    protected ArrayList<T> originalValues;
    protected ArrayFilter filter;

    public RecyclerArrayAdapter(@NonNull Context c, final List<T> objects) {
        this.objects = objects;
        context = c;
    }

    public RecyclerArrayAdapter(@NonNull Context c, final T[] objects) {
        this.objects = new ArrayList<>(Arrays.asList(objects));
        context = c;
    }

    public RecyclerArrayAdapter(@NonNull Context c) {
        context = c;
        objects = new ArrayList<>();
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
        synchronized (lock) {
            objects.add(object);
        }
        notifyItemInserted(getItemCount() - 1);
    }

    public void add(final Collection<? extends T> collection) {
        synchronized (lock) {
            objects.addAll(collection);
        }
        notifyItemInserted(getItemCount() - collection.size());
    }

    public void clear() {
        final int size = getItemCount();
        synchronized (lock) {
            objects.clear();
        }
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Utility method to reload a new dataset. It checks if items are new
     * or modified or deleted to update the views. To define custom criteria,
     * override {@see getDiffUtils} and extend DiffCallback class.
     * @param collection The new dataset
     */
    public void refresh(List<T> collection) {
        final DiffUtil.Callback diffCallback = getDiffUtil(objects, collection);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        synchronized (lock) {
            objects.clear();
            objects.addAll(collection);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * By default it returns a DiffCallback class. Override if needed.
     * @param objects The old list
     * @param collection The new list
     * @return A DiffUtil.Callback object
     */
    protected DiffUtil.Callback getDiffUtil(List<T> objects, List<T> collection) {
        return new DiffCallback(objects, collection);
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    public T getItem(final int position) {
        return objects.get(position);
    }

    public long getItemId(final int position) {
        return position;
    }

    public int getPosition(final T item) {
        return objects.indexOf(item);
    }

    public void insert(final T object, int index) {
        synchronized (lock) {
            objects.add(index, object);
        }
        notifyItemInserted(index);
    }

    public void remove(T object) {
        final int position = getPosition(object);
        synchronized (lock) {
            objects.remove(object);
        }
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, objects.size());
    }

    public void sort(Comparator<? super T> comparator) {
        synchronized (lock) {
            Collections.sort(objects, comparator);
        }
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(objects, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(objects, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        objects.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (dragListener != null)
            dragListener.onStartDrag(viewHolder);
    }

    @Override
    public final void onBindViewHolder(VH holder, int position) {
        holder.bindViewHolder(position);
    }

    @Override
    public final void onBindViewHolder(VH holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        holder.bindViewHolder(position);
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new ArrayFilter();
        }
        return filter;
    }

    /**
     * <p>An array filter constrains the content of the array adapter with
     * a prefix. Each item that does not start with the supplied prefix
     * is removed from the list.</p>
     */
    protected class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            final FilterResults results = new FilterResults();

            if (originalValues == null) {
                synchronized (lock) {
                    originalValues = new ArrayList<>(objects);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                final ArrayList<T> list;
                synchronized (lock) {
                    list = new ArrayList<>(originalValues);
                }
                results.values = list;
                results.count = list.size();
            } else {
                final String prefixString = prefix.toString().toLowerCase();

                final ArrayList<T> values;
                synchronized (lock) {
                    values = new ArrayList<>(originalValues);
                }

                final int count = values.size();
                final ArrayList<T> newValues = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    final T value = values.get(i);
                    final String valueText = value.toString().toLowerCase();
                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(value);
                    } else {
                        final String[] words = valueText.split(" ");
                        for (String word : words) {
                            if (word.startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }
                results.values = newValues;
                results.count = newValues.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            objects = (List<T>) results.values;
            notifyDataSetChanged();
        }
    }

    /**
     * Very basic class to use as diffutil callback. It just uses
     * the method equals to compare objects (both id and contents).
     */
    protected class DiffCallback extends DiffUtil.Callback {

        protected final List<T> mOldList;
        protected final List<T> mNewList;

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
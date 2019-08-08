/*
 * Copyright 2019 Marco Stornelli
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

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.RecyclerView;

@SuppressWarnings("unused")
public class SmartRecycleView extends RecyclerView implements CheckableList {

    private static final int CHECK_POSITION_SEARCH_DISTANCE = 20;
    private SparseBooleanArray checkStates;
    private LongSparseArray<Integer> checkedIdStates;
    private int checkedItemCount = 0;
    @Nullable
    private MultiChoiceModeWrapper multiChoiceModeCallback;
    @Nullable
    private ActionMode choiceActionMode;
    @ChoiceMode
    private int choiceMode;
    private AdapterDataSetObserver adapterDataSetObserver;
    private Set<OnItemClickListener> onItemClickListeners = new HashSet<>();
    private ItemTouchListener onItemTouchListener;

    public interface OnItemClickListener {
        void onItemClick(RecyclerView parent, View clickedView, int position);

        void onItemLongClick(RecyclerView parent, View clickedView, int position);
    }

    public SmartRecycleView(Context context) {
        super(context);
        init();
    }

    public SmartRecycleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmartRecycleView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        choiceMode = NONE;
        onItemTouchListener = new ItemTouchListener();
        super.addOnItemTouchListener(onItemTouchListener);
    }

    @Override
    public void addOnItemTouchListener(@NonNull OnItemTouchListener listener) {
        onItemTouchListener.addOnItemTouchListener(listener);
    }

    @Override
    public void removeOnItemTouchListener(@NonNull OnItemTouchListener listener) {
        onItemTouchListener.removeOnItemTouchListener(listener);
    }

    public void setAdapter(Adapter adapter) {
        Adapter old = getAdapter();
        if (old != null && adapterDataSetObserver != null)
            old.unregisterAdapterDataObserver(adapterDataSetObserver);
        super.setAdapter(adapter);
        adapterDataSetObserver = new AdapterDataSetObserver();
        adapter.registerAdapterDataObserver(adapterDataSetObserver);
        checkStates = new SparseBooleanArray(0);
        if (adapter.hasStableIds()) {
            checkedIdStates = new LongSparseArray<>(0);
        }
    }

    public void setChoiceMode(@ChoiceMode int choiceMode) {
        this.choiceMode = choiceMode;
    }

    public int getChoiceMode() {
        return choiceMode;
    }

    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        if (listener == null) {
            multiChoiceModeCallback = null;
            return;
        }
        if (multiChoiceModeCallback == null) {
            multiChoiceModeCallback = new MultiChoiceModeWrapper();
        }
        multiChoiceModeCallback.setWrapped(listener);
    }

    public int getCheckedItemCount() {
        return checkedItemCount;
    }

    public boolean isItemChecked(int position) {
        return checkStates.get(position);
    }

    public SparseBooleanArray getCheckedItemPositions() {
        return checkStates;
    }

    public long[] getCheckedItemIds() {
        final LongSparseArray<Integer> idStates = checkedIdStates;
        if (idStates == null) {
            return new long[0];
        }

        final int count = idStates.size();
        final long[] ids = new long[count];

        for (int i = 0; i < count; i++) {
            ids[i] = idStates.keyAt(i);
        }

        return ids;
    }

    public void clearChoices() {
        if (checkedItemCount > 0) {
            final int start = checkStates.keyAt(0);
            final int end = checkStates.keyAt(checkStates.size() - 1);
            checkStates.clear();
            if (checkedIdStates != null) {
                checkedIdStates.clear();
            }
            checkedItemCount = 0;

            Adapter adapter = getAdapter();
            if (adapter != null)
                adapter.notifyItemRangeChanged(start, end - start + 1);

            if (choiceActionMode != null) {
                choiceActionMode.finish();
            }
        }
    }

    public void setItemChecked(int position, boolean value, boolean notifyChanged) {
        if (choiceMode == NONE || getAdapter() == null)
            return;

        // Start selection mode if needed. We don't need to if we're unchecking something.
        if (value) {
            startSupportActionModeIfNeeded();
        }

        if (choiceMode == MULTI || choiceMode == MULTI_MODAL) {
            boolean oldValue = checkStates.get(position);
            checkStates.put(position, value);

            if (oldValue != value) {
                final long id = getAdapter().getItemId(position);

                if (checkedIdStates != null) {
                    if (value) {
                        checkedIdStates.put(id, position);
                    } else {
                        checkedIdStates.delete(id);
                    }
                }

                if (value) {
                    checkedItemCount++;
                } else {
                    checkedItemCount--;
                }

                if (notifyChanged) {
                    getAdapter().notifyItemChanged(position);
                }

                if (choiceActionMode != null) {
                    if (multiChoiceModeCallback != null)
                        multiChoiceModeCallback.onItemCheckedStateChanged(choiceActionMode, position, id, value);
                    if (checkedItemCount == 0) {
                        choiceActionMode.finish();
                    }
                }
            }
        } else {
            boolean updateIds = checkedIdStates != null && getAdapter().hasStableIds();
            // Clear all values if we're checking something, or unchecking the currently
            // selected item
            boolean oldValue = isItemChecked(position);
            int oldCount = checkedItemCount;
            if (value || isItemChecked(position)) {
                checkStates.clear();
                if (updateIds) {
                    checkedIdStates.clear();
                }
            }
            // this may end up selecting the value we just cleared but this way
            // we ensure length of mCheckStates is 1, a fact getCheckedItemPosition relies on
            if (value) {
                checkStates.put(position, true);
                if (updateIds) {
                    checkedIdStates.put(getAdapter().getItemId(position), position);
                }
                checkedItemCount = 1;
            } else if (checkStates.size() == 0 || !checkStates.valueAt(0)) {
                checkedItemCount = 0;
            }

            /*
             * Update only one item if: we are deselecting it or we are selecting it
             * but the there wasn't any item checked.
             */
            if ((oldValue && !value) || oldCount == 0)
                getAdapter().notifyItemChanged(position);
            else
                getAdapter().notifyDataSetChanged();
        }
    }

    public void toggleItemChecked(int position, boolean notifyChanged) {
        setItemChecked(position, !isItemChecked(position), notifyChanged);
    }

    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState();
        savedState.checkedItemCount = checkedItemCount;
        savedState.checkStates = checkStates.clone();
        savedState.recycledState = super.onSaveInstanceState();
        if (checkedIdStates != null) {
            savedState.checkedIdStates = checkedIdStates.clone();
        }
        return savedState;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if (state == null)
            return;
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.recycledState);
        if (checkedItemCount == 0) {
            checkedItemCount = savedState.checkedItemCount;
            checkStates = savedState.checkStates;
            checkedIdStates = savedState.checkedIdStates;

            if (checkedItemCount > 0) {
                Adapter adapter = getAdapter();
                // Empty adapter is given a chance to be populated before completeRestoreInstanceState()
                if (adapter != null && adapter.getItemCount() > 0) {
                    confirmCheckedPositions();
                }
                if (getContext() instanceof Activity) {
                    ((Activity) getContext()).getWindow().getDecorView().post(new Runnable() {
                        @Override
                        public void run() {
                            completeRestoreInstanceState();
                        }
                    });
                }
            }
        }
    }

    void completeRestoreInstanceState() {
        if (checkedItemCount > 0 && getAdapter() != null) {
            if (getAdapter().getItemCount() == 0) {
                // Adapter was not populated, clear the selection
                confirmCheckedPositions();
            } else {
                startSupportActionModeIfNeeded();
            }
        }
    }

    private void startSupportActionModeIfNeeded() {
        if (choiceActionMode == null && choiceMode == MULTI_MODAL) {
            if (multiChoiceModeCallback == null) {
                throw new IllegalStateException("No callback set");
            }
            if (getContext() instanceof Activity)
                choiceActionMode = ((Activity) getContext()).startActionMode(multiChoiceModeCallback);
        }
    }

    public static class SavedState implements Parcelable {

        int checkedItemCount;
        SparseBooleanArray checkStates;
        LongSparseArray<Integer> checkedIdStates;
        Parcelable recycledState;

        SavedState() {
        }

        SavedState(Parcel in) {
            checkedItemCount = in.readInt();
            checkStates = in.readSparseBooleanArray();
            final int n = in.readInt();
            if (n >= 0) {
                checkedIdStates = new LongSparseArray<>(n);
                for (int i = 0; i < n; i++) {
                    final long key = in.readLong();
                    final int value = in.readInt();
                    checkedIdStates.append(key, value);
                }
            }
            recycledState = in.readParcelable(RecyclerView.SavedState.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(checkedItemCount);
            out.writeSparseBooleanArray(checkStates);
            final int n = checkedIdStates != null ? checkedIdStates.size() : -1;
            out.writeInt(n);
            for (int i = 0; i < n; i++) {
                out.writeLong(checkedIdStates.keyAt(i));
                out.writeInt(checkedIdStates.valueAt(i));
            }
            out.writeParcelable(recycledState, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    void confirmCheckedPositions() {
        if (checkedItemCount == 0 || getAdapter() == null) {
            return;
        }

        final int itemCount = getAdapter().getItemCount();
        boolean checkedCountChanged = false;

        if (itemCount == 0) {
            // Optimized path for empty adapter: remove all items.
            checkStates.clear();
            if (checkedIdStates != null) {
                checkedIdStates.clear();
            }
            checkedItemCount = 0;
            checkedCountChanged = true;
        } else if (checkedIdStates != null) {
            // Clear out the positional check states, we'll rebuild it below from IDs.
            checkStates.clear();

            for (int checkedIndex = 0; checkedIndex < checkedIdStates.size(); checkedIndex++) {
                final long id = checkedIdStates.keyAt(checkedIndex);
                final int lastPos = checkedIdStates.valueAt(checkedIndex);

                if ((lastPos >= itemCount) || (id != getAdapter().getItemId(lastPos))) {
                    // Look around to see if the ID is nearby. If not, uncheck it.
                    final int start = Math.max(0, lastPos - CHECK_POSITION_SEARCH_DISTANCE);
                    final int end = Math.min(lastPos + CHECK_POSITION_SEARCH_DISTANCE, itemCount);
                    boolean found = false;
                    for (int searchPos = start; searchPos < end; searchPos++) {
                        final long searchId = getAdapter().getItemId(searchPos);
                        if (id == searchId) {
                            found = true;
                            checkStates.put(searchPos, true);
                            checkedIdStates.setValueAt(checkedIndex, searchPos);
                            break;
                        }
                    }

                    if (!found) {
                        checkedIdStates.delete(id);
                        checkedIndex--;
                        checkedItemCount--;
                        checkedCountChanged = true;
                        if (choiceActionMode != null && multiChoiceModeCallback != null) {
                            multiChoiceModeCallback.onItemCheckedStateChanged(choiceActionMode, lastPos, id, false);
                        }
                    }
                } else {
                    checkStates.put(lastPos, true);
                }
            }
        } else {
            // If the total number of items decreased, remove all out-of-range check indexes.
            for (int i = checkStates.size() - 1; (i >= 0) && (checkStates.keyAt(i) >= itemCount); i--) {
                if (checkStates.valueAt(i)) {
                    checkedItemCount--;
                    checkedCountChanged = true;
                }
                checkStates.delete(checkStates.keyAt(i));
            }
        }

        if (checkedCountChanged && choiceActionMode != null) {
            if (checkedItemCount == 0) {
                choiceActionMode.finish();
            } else {
                choiceActionMode.invalidate();
            }
        }
    }

    private class AdapterDataSetObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            confirmCheckedPositions();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            confirmCheckedPositions();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            confirmCheckedPositions();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            confirmCheckedPositions();
        }
    }

    private class MultiChoiceModeWrapper implements MultiChoiceModeListener {

        private MultiChoiceModeListener wrapped;

        public void setWrapped(@NonNull MultiChoiceModeListener wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return wrapped.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return wrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return wrapped.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            wrapped.onDestroyActionMode(mode);
            choiceActionMode = null;
            clearChoices();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            wrapped.onItemCheckedStateChanged(mode, position, id, checked);
        }
    }

    public void addOnItemClickListener(OnItemClickListener listener) {
        if (listener != null)
            onItemClickListeners.add(listener);
    }

    public void removeOnItemClickListener(OnItemClickListener listener) {
        onItemClickListeners.remove(listener);
    }

    private void onItemClick(View view, int position) {
        for (OnItemClickListener l : onItemClickListeners)
            l.onItemClick(this, view, position);
    }

    private void onItemLongClick(View view, int position) {
        for (OnItemClickListener l : onItemClickListeners)
            l.onItemLongClick(this, view, position);
    }

    private class ItemTouchListener extends GestureDetector.SimpleOnGestureListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private boolean disallowedIntercept;
        private List<OnItemTouchListener> wrapped = new ArrayList<>();

        public ItemTouchListener() {
            disallowedIntercept = false;
            this.gestureDetector = new GestureDetector(getContext(), this);
        }

        public void addOnItemTouchListener(OnItemTouchListener l) {
            if (l != null)
                wrapped.add(l);
        }

        public void removeOnItemTouchListener(OnItemTouchListener l) {
            if (l != null)
                wrapped.remove(l);
        }

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
            if (!disallowedIntercept) {
                gestureDetector.onTouchEvent(motionEvent);
            }
            boolean intercept = false;
            for (OnItemTouchListener w : wrapped)
                intercept |= w.onInterceptTouchEvent(recyclerView, motionEvent);
            return intercept;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
            for (OnItemTouchListener w : wrapped)
                w.onTouchEvent(recyclerView, motionEvent);
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            for (OnItemTouchListener w : wrapped)
                w.onRequestDisallowInterceptTouchEvent(disallowedIntercept);
            this.disallowedIntercept = disallowIntercept;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            View view = getChildViewUnder(e);
            if (view != null) {
                view.setPressed(true);
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            View view = getChildViewUnder(e);
            if (view == null)
                return false;

            view.setPressed(false);
            int position = getChildAdapterPosition(view);
            BaseViewHolder baseViewHolder = (BaseViewHolder) getChildViewHolder(view);
            baseViewHolder.onClickListener(view);
            onItemClick(view, position);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            View view = getChildViewUnder(e);
            if (view == null)
                return;
            int position = getChildAdapterPosition(view);
            BaseViewHolder baseViewHolder = (BaseViewHolder) getChildViewHolder(view);
            baseViewHolder.onLongClickListener(view);
            onItemLongClick(view, position);
            view.setPressed(false);
        }

        @Nullable
        private View getChildViewUnder(MotionEvent e) {
            return findChildViewUnder(e.getX(), e.getY());
        }
    }
}

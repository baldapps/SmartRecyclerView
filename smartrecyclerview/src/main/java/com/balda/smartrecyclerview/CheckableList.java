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

import androidx.annotation.IntDef;
import android.util.SparseBooleanArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This interface decouple the adapter from the recycler view
 */
@SuppressWarnings("SameParameterValue, Unused")
public interface CheckableList {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NONE, SINGLE, MULTI, MULTI_MODAL})
    @interface ChoiceMode {
    }
    int NONE = 0;
    int SINGLE = 1;
    int MULTI = 2;
    int MULTI_MODAL = 3;

    void setChoiceMode(@ChoiceMode int choiceMode);

    int getChoiceMode();

    void setMultiChoiceModeListener(MultiChoiceModeListener listener);

    int getCheckedItemCount();

    boolean isItemChecked(int position);

    SparseBooleanArray getCheckedItemPositions();

    long[] getCheckedItemIds();

    void clearChoices();

    void setItemChecked(int position, boolean value, boolean notifyChanged);

    void toggleItemChecked(int position, boolean notifyChanged);
}
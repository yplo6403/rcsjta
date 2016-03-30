/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
 ******************************************************************************/

package com.gsma.rcs.ri.messaging.adapter;

import com.gsma.rcs.ri.R;
import com.gsma.services.rcs.history.HistoryLog;

import android.database.Cursor;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A ViewHolder class keeps references to children views to avoid unnecessary calls to
 * findViewById() or getColumnIndex() on each row.
 */
public class MmsViewHolder extends BasicViewHolder {
    private final LinearLayout mImagesLayout;
    private final TextView mSubjectText;
    private final int mColumnSubjectIdx;
    private final TextView mBodyText;
    private final int mColumnIdIdx;

    /**
     * Constructor
     *
     * @param base view
     * @param cursor cursor
     */
    MmsViewHolder(View base, Cursor cursor) {
        super(base, cursor);
        /* Save column indexes */
        mColumnIdIdx = cursor.getColumnIndexOrThrow(HistoryLog.ID);
        mColumnSubjectIdx = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
        /* Save children views */
        mSubjectText = (TextView) base.findViewById(R.id.subject_text);
        mImagesLayout = (LinearLayout) base.findViewById(R.id.mms_images_layout);
        mBodyText = (TextView) base.findViewById(R.id.body_text);
    }

    public TextView getSubjectText() {
        return mSubjectText;
    }

    public int getColumnSubjectIdx() {
        return mColumnSubjectIdx;
    }

    public TextView getBodyText() {
        return mBodyText;
    }

    public int getColumnIdIdx() {
        return mColumnIdIdx;
    }

    public LinearLayout getImagesLayout() {
        return mImagesLayout;
    }
}

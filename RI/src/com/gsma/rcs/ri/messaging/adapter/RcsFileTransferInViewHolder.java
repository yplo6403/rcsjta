/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010-2016 Orange.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package com.gsma.rcs.ri.messaging.adapter;

import com.gsma.rcs.ri.R;
import com.gsma.services.rcs.history.HistoryLog;

import android.database.Cursor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A ViewHolder class keeps references to children views to avoid unnecessary calls to
 * findViewById() or getColumnIndex() on each row.
 */
public class RcsFileTransferInViewHolder extends BasicViewHolder {
    private final TextView mProgressText;
    private final ImageView mFileImageView;

    private final int mColumnContentIdx;
    private final int mColumnFilenameIdx;
    private final int mColumnFilesizeIdx;
    private final int mColumnTransferredIdx;
    private final int mColumnContactIdx;
    private final int mColumnFileiconIdx;
    private final LinearLayout mButtonLayout;
    private final Button mAcceptButton;
    private final Button mRejectButton;

    /**
     * Constructor
     *
     * @param base view
     * @param cursor cursor
     */
    RcsFileTransferInViewHolder(View base, Cursor cursor) {
        super(base, cursor);
        /* Save column indexes */
        mColumnContentIdx = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
        mColumnFilenameIdx = cursor.getColumnIndexOrThrow(HistoryLog.FILENAME);
        mColumnFilesizeIdx = cursor.getColumnIndexOrThrow(HistoryLog.FILESIZE);
        mColumnTransferredIdx = cursor.getColumnIndexOrThrow(HistoryLog.TRANSFERRED);
        mColumnContactIdx = cursor.getColumnIndexOrThrow(HistoryLog.CONTACT);
        mColumnFileiconIdx = cursor.getColumnIndexOrThrow(HistoryLog.FILEICON);
        /* Save children views */
        mProgressText = (TextView) base.findViewById(R.id.progress_text);
        mFileImageView = (ImageView) base.findViewById(R.id.file_image);
        mButtonLayout = (LinearLayout) base.findViewById(R.id.button);
        mAcceptButton = (Button) base.findViewById(R.id.accept_btn);
        mRejectButton = (Button) base.findViewById(R.id.reject_btn);
    }

    public int getColumnContentIdx() {
        return mColumnContentIdx;
    }

    public TextView getProgressText() {
        return mProgressText;
    }

    public ImageView getFileImageView() {
        return mFileImageView;
    }

    public int getColumnFilenameIdx() {
        return mColumnFilenameIdx;
    }

    public int getColumnFilesizeIdx() {
        return mColumnFilesizeIdx;
    }

    public int getColumnTransferredIdx() {
        return mColumnTransferredIdx;
    }

    public int getColumnContactIdx() {
        return mColumnContactIdx;
    }

    public Button getRejectButton() {
        return mRejectButton;
    }

    public LinearLayout getButtonLayout() {
        return mButtonLayout;
    }

    public Button getAcceptButton() {
        return mAcceptButton;
    }

    public int getColumnFileiconIdx() {
        return mColumnFileiconIdx;
    }
}

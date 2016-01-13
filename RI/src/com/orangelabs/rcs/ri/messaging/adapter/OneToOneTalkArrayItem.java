/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.ri.messaging.adapter;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import com.orangelabs.rcs.ri.R;

import android.view.View;
import android.widget.TextView;

/**
 * Created by yplo6403 on 12/01/2016.
 */
public class OneToOneTalkArrayItem implements Comparable<OneToOneTalkArrayItem> {

    private final int mProviderId;
    private final long mTimestamp;
    private final int mState;
    private final int mReason;
    private final RcsService.Direction mDirection;
    private final ContactId mContact;
    private final String mContent;
    private final String mMimeType;
    private String mFilename;
    private Long mTransferred;
    private Long mFileSize;

    /**
     * Constructor for XMS and RCS chat information
     *
     * @param providerId the provider ID
     * @param contact the contact ID
     * @param timestamp the timestamp
     * @param state the state
     * @param reason the reason
     * @param direction the direction
     * @param content the content
     * @param mimeType the mime type
     */
    public OneToOneTalkArrayItem(int providerId, ContactId contact, long timestamp, int state,
            int reason, RcsService.Direction direction, String content, String mimeType) {
        mProviderId = providerId;
        mContact = contact;
        mTimestamp = timestamp;
        mState = state;
        mReason = reason;
        mDirection = direction;
        mContent = content;
        mMimeType = mimeType;
    }

    /**
     * Constructor for RCS file transfer message
     *
     * @param contact the contact ID
     * @param timestamp the timestamp
     * @param state the state
     * @param reason the reason
     * @param direction the direction
     * @param content the content
     * @param mimeType the mime type
     * @param filename the filename
     * @param fileSize the file size
     * @param transferred the transferred size
     */
    public OneToOneTalkArrayItem(ContactId contact, long timestamp, int state, int reason,
            RcsService.Direction direction, String content, String mimeType, String filename,
            long fileSize, long transferred) {
        this(FileTransferLog.HISTORYLOG_MEMBER_ID, contact, timestamp, state, reason, direction,
                content, mimeType);
        mFilename = filename;
        mFileSize = fileSize;
        mTransferred = transferred;
    }

    public int getProviderId() {
        return mProviderId;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public int getState() {
        return mState;
    }

    public RcsService.Direction getDirection() {
        return mDirection;
    }

    public ContactId getContact() {
        return mContact;
    }

    public String getContent() {
        return mContent;
    }

    public String getFilename() {
        return mFilename;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public Long getTransferred() {
        return mTransferred;
    }

    public Long getFileSize() {
        return mFileSize;
    }

    public int getReason() {
        return mReason;
    }

    @Override
    public int compareTo(OneToOneTalkArrayItem another) {
        if (another == null) {
            throw new NullPointerException("Cannot compare to null");
        }
        return Long.valueOf(mTimestamp).compareTo(another.getTimestamp());
    }

    static public class ViewHolder {

        private final TextView mContactText;
        private final TextView mStatusText;
        private final TextView mTimestampText;
        private final TextView mContentText;

        public ViewHolder(View view) {
            mContactText = (TextView) view.findViewById(R.id.contact_text);
            mStatusText = (TextView) view.findViewById(R.id.status_text);
            mTimestampText = (TextView) view.findViewById(R.id.timestamp_text);
            mContentText = (TextView) view.findViewById(R.id.content_text);
        }

        public TextView getContactText() {
            return mContactText;
        }

        public TextView getStatusText() {
            return mStatusText;
        }

        public TextView getTimestampText() {
            return mTimestampText;
        }

        public TextView getContentText() {
            return mContentText;
        }

    }

}

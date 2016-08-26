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

package com.gsma.rcs.provider.xms.model;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.util.List;

/**
 * A class to model the MMS data object
 */
public class MmsDataObject extends XmsDataObject {

    private final List<MmsPart> mMmsParts;
    private final String mSubject;

    public MmsDataObject(String messageId, ContactId contact, String subject,
            RcsService.Direction dir, ReadStatus readStatus, long timestamp, Long nativeId,
            List<MmsPart> mmsParts) {
        super(messageId, contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, dir, timestamp,
                nativeId);
        mReadStatus = readStatus;
        mMmsParts = mmsParts;
        mSubject = subject;
    }

    public String getSubject() {
        return mSubject;
    }

    public List<MmsPart> getMmsParts() {
        return mMmsParts;
    }

    @Override
    public String toString() {
        return "MmsDataObject{" + super.toString() + ", subject='" + mSubject + "', parts="
                + mMmsParts + '}';
    }

    public static class MmsPart {
        private final String mMessageId;
        private String mMimeType;
        private final String mContentText;
        private final Uri mFile;
        private final byte[] mFileIcon;

        /* Only for outgoing MMS */
        private byte[] mPdu;

        private final String mFileName;
        private final Long mFileSize;

        public MmsPart(String messageId, String fileName, Long fileSize, String mimeType, Uri file,
                byte[] fileIcon) {
            mMessageId = messageId;
            mFileName = fileName;
            mFileSize = fileSize;
            mFile = file;
            mMimeType = mimeType;
            mContentText = null;
            mFileIcon = fileIcon;
        }

        public MmsPart(String messageId, String mimeType, String content) {
            mMessageId = messageId;
            mMimeType = mimeType;
            mContentText = content;
            mFileName = null;
            mFileSize = null;
            mFile = null;
            mPdu = null;
            mFileIcon = null;
        }

        public String getMessageId() {
            return mMessageId;
        }

        public String getMimeType() {
            return mMimeType;
        }

        public String getContentText() {
            return mContentText;
        }

        public Uri getFile() {
            return mFile;
        }

        public byte[] getFileIcon() {
            return mFileIcon;
        }

        public String getFileName() {
            return mFileName;
        }

        public Long getFileSize() {
            return mFileSize;
        }

        public byte[] getPdu() {
            return mPdu;
        }

        public void setPdu(byte[] pdu) {
            mPdu = pdu;
        }

        public void setMimeType(String mimeType) {
            mMimeType = mimeType;
        }

        @Override
        public String toString() {
            return "MmsPart{mimeType=" + mMimeType + ", fileName=" + mFileName + ", fileSize="
                    + mFileSize + '}';
        }
    }
}

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

package com.gsma.rcs.ri.cms.messaging;

import com.gsma.services.rcs.cms.MmsPartLog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.HashSet;
import java.util.Set;

/**
 * MMS Part Data Object
 *
 * @author YPLO6403
 */
public class MmsPartDataObject {

    // @formatter:off
    private static final String[] PROJECTION = {
            MmsPartLog.BASECOLUMN_ID,
            MmsPartLog.MIME_TYPE,
            MmsPartLog.FILENAME,
            MmsPartLog.FILESIZE,
            MmsPartLog.FILEICON,
            MmsPartLog.CONTENT,
    };
    // @formatter:on

    private static final String SELECTION = MmsPartLog.MESSAGE_ID + "=?";
    private static ContentResolver sContentResolver;
    private final long mId;
    private final String mMessageId;
    private final String mMimeType;
    private final String mFilename;
    private final Long mFileSize;
    private final String mBody;
    private final Uri mFile;
    private final byte[] mFileIcon;

    public static final long INVALID_ID = -1;

    @Override
    public String toString() {
        return "MmsPartDataObject{" + "messageId='" + mMessageId + '\'' + ", mimeType='"
                + mMimeType + '\'' + ", filename='" + mFilename + '\'' + ", fileSize=" + mFileSize
                + ", body='" + mBody + '\'' + ", file=" + mFile + '}';
    }

    public MmsPartDataObject(String mimeType, Uri file, String filename, Long fileSize) {
        mId = INVALID_ID;
        mMessageId = null;
        mFilename = filename;
        mMimeType = mimeType;
        mFileSize = fileSize;
        mBody = null;
        mFile = file;
        mFileIcon = null;
    }

    public MmsPartDataObject(long id, String messageId, String mimeType, String filename,
            Long fileSize, String content, byte[] fileIcon) {
        mId = id;
        mMessageId = messageId;
        mMimeType = mimeType;
        mFilename = filename;
        mFileSize = fileSize;
        if (MmsPartLog.MimeType.TEXT_MESSAGE.equals(mimeType)
                || MmsPartLog.MimeType.APPLICATION_SMIL.equals(mimeType)) {
            mBody = content;
            mFile = null;
            mFileIcon = null;
        } else {
            mBody = null;
            mFile = Uri.parse(content);
            mFileIcon = fileIcon;
        }
    }

    /**
     * Gets instance of chat message from MmsPartLog provider
     *
     * @param ctx the context
     * @param messageId the message ID
     * @return instance or null if entry not found
     */
    public static Set<MmsPartDataObject> getParts(Context ctx, String messageId) {
        if (sContentResolver == null) {
            sContentResolver = ctx.getContentResolver();
        }
        Set<MmsPartDataObject> result = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = sContentResolver.query(MmsPartLog.CONTENT_URI, PROJECTION, SELECTION,
                    new String[] {
                        messageId
                    }, null);
            if (cursor == null) {
                throw new IllegalStateException("Cannot query parts for message ID=" + messageId);
            }
            if (!cursor.moveToFirst()) {
                return result;
            }
            int idColumnIdx = cursor.getColumnIndexOrThrow(MmsPartLog.BASECOLUMN_ID);
            int mimeTypeIdx = cursor.getColumnIndexOrThrow(MmsPartLog.MIME_TYPE);
            int filenameIdx = cursor.getColumnIndexOrThrow(MmsPartLog.FILENAME);
            int fileSizeIdx = cursor.getColumnIndexOrThrow(MmsPartLog.FILESIZE);
            int fileIconIdx = cursor.getColumnIndexOrThrow(MmsPartLog.FILEICON);
            int contentIdx = cursor.getColumnIndexOrThrow(MmsPartLog.CONTENT);
            do {
                long id = cursor.getLong(idColumnIdx);
                String filename = cursor.getString(filenameIdx);
                String mimeType = cursor.getString(mimeTypeIdx);
                Long fileSize = cursor.isNull(fileSizeIdx) ? null : cursor.getLong(fileSizeIdx);
                byte[] fileIcon = cursor.getBlob(fileIconIdx);
                String content = cursor.getString(contentIdx);
                result.add(new MmsPartDataObject(id, messageId, mimeType, filename, fileSize,
                        content, fileIcon));
            } while (cursor.moveToNext());
            return result;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long getId() {
        return mId;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public String getFilename() {
        return mFilename;
    }

    public Long getFileSize() {
        return mFileSize;
    }

    public String getBody() {
        return mBody;
    }

    public byte[] getFileIcon() {
        return mFileIcon;
    }

    public Uri getFile() {
        return mFile;
    }

}

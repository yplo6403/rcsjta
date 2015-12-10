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

package com.gsma.rcs.provider.xms.model;

import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MmsDataObject extends XmsDataObject {

    private final List<MmsPart> mMmsPart;
    /**
     * The MMS Identifier created by the native SMS/MMS application.
     */
    private final String mMmsId;
    private final String mSubject;

    public MmsDataObject(String mmsId, String messageId, ContactId contact, String subject,
            RcsService.Direction dir, ReadStatus readStatus, long timestamp,
            long nativeId, long nativeThreadId, List<MmsPart> mmsPart) {
        super(messageId, contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, dir, timestamp,
                nativeId, nativeThreadId);
        mMmsId = mmsId;
        mReadStatus = readStatus;
        mMmsPart = mmsPart;
        mSubject = subject;
    }

    public MmsDataObject(Context ctx, String mmsId, String messageId, ContactId contact,
            String subject, String body, RcsService.Direction dir, long timestamp, List<Uri> files,
            Long nativeId, long maxFileIconSize) throws IOException {
        super(messageId, contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, dir, timestamp,
                nativeId, null);
        mMmsId = mmsId;
        mSubject = subject;
        mMmsPart = new ArrayList<>();
        for (Uri file : files) {
            String filename = FileUtils.getFileName(ctx, file);
            long fileSize = FileUtils.getFileSize(ctx, file);
            String extension = MimeManager.getFileExtension(filename);
            String mimeType = MimeManager.getInstance().getMimeType(extension);
            byte[] fileIcon = null;
            if (MimeManager.isImageType(mimeType)) {
                fileIcon = ImageUtils.tryGetThumbnail(ctx, file, maxFileIconSize);
            }
            mMmsPart.add(new MmsPart(messageId, contact, mimeType, filename, fileSize, file
                    .toString(), fileIcon));
        }
        if (body != null) {
            mMmsPart.add(new MmsPart(messageId, contact, XmsMessageLog.MimeType.TEXT_MESSAGE, null,
                    null, body, null));
        }
    }

    public String getMmsId() {
        return mMmsId;
    }

    public String getSubject() {
        return mSubject;
    }

    public List<MmsPart> getMmsPart() {
        return mMmsPart;
    }

    public static class MmsPart {
        private final String mMessageId;
        private final String mMimeType;
        private final String mBody;
        private final Uri mFile;
        private final byte[] mFileIcon;

        private final String mFileName;
        private final Long mFileSize;
        private final ContactId mContact;

        public MmsPart(String messageId, ContactId contact, String mimeType, String fileName,
                Long fileSize, String data, byte[] fileIcon) {
            mMimeType = mimeType;
            mFileName = fileName;
            mFileSize = fileSize;
            if (MimeManager.isApplicationType(mimeType) || MimeManager.isTextType(mimeType)) {
                mBody = data;
                mFile = null;
            } else {
                mBody = null;
                mFile = Uri.parse(data);
            }
            mFileIcon = fileIcon;
            mMessageId = messageId;
            mContact = contact;
        }

        public String getMessageId() {
            return mMessageId;
        }

        public ContactId getContact() {
            return mContact;
        }

        public String getMimeType() {
            return mMimeType;
        }

        public String getBody() {
            return mBody;
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

    }
}

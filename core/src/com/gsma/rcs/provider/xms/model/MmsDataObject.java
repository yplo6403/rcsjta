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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import com.orange.labs.mms.priv.MmsFileSizeException;
import com.orange.labs.mms.priv.utils.Constants;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MmsDataObject extends XmsDataObject {

    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH = 640;

    private final List<MmsPart> mMmsPart;
    /**
     * The MMS Identifier created by the native SMS/MMS application.
     */
    private final String mMmsId;
    private final String mSubject;

    public MmsDataObject(String mmsId, String messageId, ContactId contact, String subject,
            RcsService.Direction dir, ReadStatus readStatus, long timestamp, Long nativeId,
            Long nativeThreadId, List<MmsPart> mmsPart) {
        super(messageId, contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, dir, timestamp,
                nativeId, nativeThreadId);
        mMmsId = mmsId;
        mReadStatus = readStatus;
        mMmsPart = mmsPart;
        mSubject = subject;
    }

    public MmsDataObject(Context ctx, String mmsId, String messageId, ContactId contact,
            String subject, String body, RcsService.Direction dir, long timestamp, List<Uri> files,
            Long nativeId, long maxFileIconSize) throws MmsFileSizeException, FileAccessException {
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
            mMmsPart.add(new MmsPart(messageId, contact, filename, fileSize, file, fileIcon));
        }
        long availableSize = Constants.MAX_FILE_SIZE;
        /* Remove size of the body text */
        if (body != null) {
            mMmsPart.add(new MmsPart(messageId, contact, XmsMessageLog.MimeType.TEXT_MESSAGE, body));
            availableSize -= body.length();
        }
        /* remove size of the un-compressible parts */
        long imageSize = 0;
        for (MmsPart part : mMmsPart) {
            Long fileSize = part.getFileSize();
            if (fileSize != null) {
                /* The part is a file */
                if (!MimeManager.isImageType(part.getMimeType())) {
                    /* The part cannot be compressed: not an image */
                    availableSize -= fileSize;
                } else {
                    imageSize += fileSize;
                }
            }
        }
        if (availableSize < 0) {
            throw new MmsFileSizeException("Sum of un-compressible MMS parts is too high!");
        }
        if (imageSize > availableSize) {
            /* Image compression is required */
            Map<MmsPart, Long> imagesWithTargetSize = new HashMap<>();
            for (MmsPart part : mMmsPart) {
                if (MimeManager.isImageType(part.getMimeType())) {
                    Long targetSize = (part.getFileSize() * availableSize) / imageSize;
                    imagesWithTargetSize.put(part, targetSize);
                }
            }
            for (Map.Entry<MmsPart, Long> entry : imagesWithTargetSize.entrySet()) {
                MmsPart part = entry.getKey();
                Long maxSize = entry.getValue();
                part.setCompressed(ImageUtils.compressImage(ctx, part.getFile(), maxSize,
                        MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT));
            }
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
        private final String mContentText;
        private final Uri mFile;
        private final byte[] mFileIcon;

        /* By default there is no image compression */
        private byte[] mCompressed;

        private final String mFileName;
        private final Long mFileSize;
        private final ContactId mContact;

        public MmsPart(String messageId, ContactId contact, String fileName, Long fileSize,
                Uri file, byte[] fileIcon) {
            mMessageId = messageId;
            mContact = contact;
            mFileName = fileName;
            mFileSize = fileSize;
            mFile = file;
            String extension = MimeManager.getFileExtension(fileName);
            mMimeType = MimeManager.getInstance().getMimeType(extension);
            mContentText = null;
            mFileIcon = fileIcon;
        }

        public MmsPart(String messageId, ContactId contact, String mimeType, String content) {
            mMessageId = messageId;
            mContact = contact;
            mMimeType = mimeType;
            mContentText = content;
            mFileName = null;
            mFileSize = null;
            mFile = null;
            mCompressed = null;
            mFileIcon = null;
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

        public byte[] getCompressed() {
            return mCompressed;
        }

        public void setCompressed(byte[] compressed) {
            mCompressed = compressed;
        }

        @Override
        public String toString() {
            return "MmsPart{" + "messageId='" + mMessageId + '\'' + ", mimeType='" + mMimeType
                    + '\'' + ", contentText='" + mContentText + '\'' + ", file=" + mFile
                    + ", fileName='" + mFileName + '\'' + ", fileSize=" + mFileSize + ", contact="
                    + mContact + '}';
        }
    }
}

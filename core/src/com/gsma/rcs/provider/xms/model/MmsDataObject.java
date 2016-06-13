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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.xms.mms.MmsFileSizeException;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MmsDataObject extends XmsDataObject {

    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH = 640;

    public static final int MMS_PARTS_MAX_FILE_SIZE = 200000; /* International limit */

    private final List<MmsPart> mMmsParts;

    /**
     * The MMS Identifier created by the native SMS/MMS application.
     */
    private final String mSubject;
    /**
     * The transaction ID to be used by the content observer of the native provider. If an outgoing
     * MMS already exists in local provider having a messageId equals to the transaction ID then
     * message is sent by the CMS and then already persisted.
     */
    private final String mTransId;

    public MmsDataObject(String transId, String messageId, ContactId contact, String subject,
            RcsService.Direction dir, ReadStatus readStatus, long timestamp, Long nativeId,
            Long nativeThreadId, List<MmsPart> mmsParts) {
        super(messageId, contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, dir, timestamp,
                nativeId, nativeThreadId);
        mReadStatus = readStatus;
        mMmsParts = mmsParts;
        mSubject = subject;
        mTransId = transId;
    }

    public MmsDataObject(String messageId, ContactId contact, String subject,
            RcsService.Direction dir, ReadStatus readStatus, long timestamp, Long nativeId,
            Long nativeThreadId, List<MmsPart> mmsParts) {
        super(messageId, contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, dir, timestamp,
                nativeId, nativeThreadId);
        mReadStatus = readStatus;
        mMmsParts = mmsParts;
        mSubject = subject;
        mTransId = null;
    }

    public MmsDataObject(Context ctx, String messageId, ContactId contact, String subject,
            String body, RcsService.Direction dir, long timestamp, List<Uri> files, Long nativeId,
            long maxFileIconSize) throws FileAccessException, MmsFileSizeException {
        super(messageId, contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, dir, timestamp,
                nativeId, null);
        mTransId = null;
        mSubject = subject;
        mMmsParts = new ArrayList<>();
        for (Uri file : files) {
            String filename = FileUtils.getFileName(ctx, file);
            long fileSize = FileUtils.getFileSize(ctx, file);
            String extension = MimeManager.getFileExtension(filename);
            String mimeType = MimeManager.getInstance().getMimeType(extension);
            byte[] fileIcon = null;
            if (MimeManager.isImageType(mimeType)) {
                String imageFilename = FileUtils.getPath(ctx, file);
                fileIcon = ImageUtils.tryGetThumbnail(imageFilename, maxFileIconSize);
            }
            mMmsParts.add(new MmsPart(messageId, filename, fileSize, mimeType, file, fileIcon));
        }
        long availableSize = MMS_PARTS_MAX_FILE_SIZE;
        /* Remove size of the body text */
        if (body != null) {
            mMmsParts.add(new MmsPart(messageId, XmsMessageLog.MimeType.TEXT_MESSAGE, body));
            availableSize -= body.length();
        }
        /* remove size of the un-compressible parts */
        long imageSize = 0;
        for (MmsPart part : mMmsParts) {
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
            for (MmsPart part : mMmsParts) {
                if (MimeManager.isImageType(part.getMimeType())) {
                    Long targetSize = (part.getFileSize() * availableSize) / imageSize;
                    imagesWithTargetSize.put(part, targetSize);
                }
            }
            for (Map.Entry<MmsPart, Long> entry : imagesWithTargetSize.entrySet()) {
                MmsPart part = entry.getKey();
                Long maxSize = entry.getValue();
                String imagePath = FileUtils.getPath(ctx, part.getFile());
                part.setPdu(ImageUtils.compressImage(imagePath, maxSize, MAX_IMAGE_WIDTH,
                        MAX_IMAGE_HEIGHT));
                // images are compressed in jpeg format
                part.setMimeType("image/jpeg");
            }
        }
    }

    public String getSubject() {
        return mSubject;
    }

    public List<MmsPart> getMmsParts() {
        return mMmsParts;
    }

    public String getTransId() {
        return mTransId;
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

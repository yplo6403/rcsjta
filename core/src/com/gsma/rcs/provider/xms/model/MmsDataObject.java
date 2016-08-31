/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.xms.model;

import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * A class to model the MMS data object
 */
public class MmsDataObject extends XmsDataObject {

    private static final Logger sLogger = Logger.getLogger(MmsDataObject.class.getName());

    private final List<MmsPart> mMmsParts;
    private final String mSubject;
    private final String mCorrelator;

    public MmsDataObject(String messageId, ContactId contact, String subject,
            RcsService.Direction dir, ReadStatus readStatus, long timestamp, Long nativeId,
            List<MmsPart> mmsParts) {
        super(messageId, contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, dir, timestamp,
                nativeId);
        mReadStatus = readStatus;
        mMmsParts = mmsParts;
        mSubject = subject;
        String body = null;
        String imageNames = null;
        for (MmsPart part : mmsParts) {
            String mimeType = part.getMimeType();
            if (MimeManager.isImageType(mimeType)) {
                if (imageNames == null) {
                    imageNames = part.getFileName();
                } else {
                    imageNames = imageNames.concat(part.getFileName());
                }
            } else if (XmsMessageLog.MimeType.TEXT_MESSAGE.equals(mimeType)) {
                body = part.getContentText();
            }
        }
        String textToCorrelate = subject;
        if (body != null) {
            if (textToCorrelate == null) {
                textToCorrelate = body;
            } else {
                textToCorrelate = textToCorrelate.concat(body);
            }
        }
        if (imageNames != null) {
            if (textToCorrelate == null) {
                textToCorrelate = imageNames;
            } else {
                textToCorrelate = textToCorrelate.concat(imageNames);
            }
        }
        if (textToCorrelate == null) {
            throw new NullPointerException("Cannot correlate MMS: all arguments are null!");
        }
        mCorrelator = getSha1Hash(textToCorrelate);
    }

    public String getSubject() {
        return mSubject;
    }

    public String getCorrelator() {
        return mCorrelator;
    }

    public List<MmsPart> getMmsParts() {
        return mMmsParts;
    }

    @Override
    public String toString() {
        return "MmsDataObject{" + super.toString() + ", subject='" + mSubject + "', parts="
                + mMmsParts + '}';
    }

    private String getSha1Hash(String input) {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("getSha1Hash for '" + input + "'");
            }
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(input.getBytes("UTF-8"));
            byte[] digest = md.digest();
            return bytesToHex(digest);

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            sLogger.error("Cannot get SHA-1 hash!", e);
            return null;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
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

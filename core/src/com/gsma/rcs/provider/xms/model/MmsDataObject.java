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

import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MmsDataObject extends XmsDataObject {

    private final List<MmsPart> mMmsPart;
    private String mMmsId;

    public MmsDataObject(Context context, ContentResolver contentResolver, String messageId,
            ContactId contact, String body, RcsService.Direction dir, long timestamp,
            List<Uri> files) throws IOException {
        super(messageId, contact, body, XmsMessageLog.MimeType.TEXT_MESSAGE, dir, timestamp);
        mMmsPart = new ArrayList<>();
        for (Uri file : files) {
            String filename = FileUtils.getFileName(context, file);
            String extension = MimeManager.getFileExtension(filename);
            String mimeType = MimeManager.getMimeExtension(extension);
            byte[] content = getBytes(contentResolver.openInputStream(file));
            byte[] fileIcon = MmsUtils.createThumb(contentResolver, file);
            mMmsPart.add(new MmsPart(messageId, mimeType, filename, content.length, content,
                    fileIcon));
        }
        if (body != null) {
            mMmsPart.add(new MmsPart(messageId, XmsMessageLog.MimeType.TEXT_MESSAGE, null, 0, body
                    .getBytes(), null));
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public String getMmsId() {
        return mMmsId;
    }

    public void setMmsId(String mmsId) {
        mMmsId = mmsId;
    }

    public List<MmsPart> getMmsPart() {
        return mMmsPart;
    }

    public class MmsPart {
        private final String mMessageId;
        private final String mMimeType;
        private final byte[] mContent;
        private final byte[] mFileIcon;

        private final String mFileName;
        private final long mFileSize;

        public MmsPart(String messageId, String mimeType, String fileName, long fileSize,
                byte[] content, byte[] fileIcon) {
            mMimeType = mimeType;
            mFileName = fileName;
            mFileSize = fileSize;
            mContent = content;
            mFileIcon = fileIcon;
            mMessageId = messageId;
        }

        public String getMessageId() {
            return mMessageId;
        }

        public String getMimeType() {
            return mMimeType;
        }

        public byte[] getContent() {
            return mContent;
        }

        public byte[] getFileIcon() {
            return mFileIcon;
        }

        public String getFileName() {
            return mFileName;
        }

        public long getFileSize() {
            return mFileSize;
        }
    }
}

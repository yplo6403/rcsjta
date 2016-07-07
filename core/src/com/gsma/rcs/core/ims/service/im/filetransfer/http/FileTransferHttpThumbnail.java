/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.net.Uri;

import java.io.File;

/**
 * File transfer over HTTP thumbnail
 * 
 * @author vfml3370
 */
public class FileTransferHttpThumbnail {

    private int mSize = 0;

    private String mMimeType;

    private Uri mFileIcon;

    private long mExpiration = FileTransferData.UNKNOWN_EXPIRATION;

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings the RCS settings accessor
     */
    public FileTransferHttpThumbnail(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    public FileTransferHttpThumbnail(RcsSettings rcsSettings, Uri fileIcon, String mimeType,
            int size, long expiration) {
        mRcsSettings = rcsSettings;
        mFileIcon = fileIcon;
        mMimeType = mimeType;
        mSize = size;
        mExpiration = expiration;
    }

    /**
     * Gets expiration
     * 
     * @return expiration in milliseconds
     */
    public long getExpiration() {
        return mExpiration;
    }

    /**
     * Sets expiration
     * 
     * @param expiration the expiration
     */
    public void setExpiration(long expiration) {
        mExpiration = expiration;
    }

    /**
     * Gets URI
     * 
     * @return URI
     */
    public Uri getUri() {
        return mFileIcon;
    }

    /**
     * Sets URI
     * 
     * @param fileIcon URI
     */
    public void setUri(Uri fileIcon) {
        mFileIcon = fileIcon;
    }

    /**
     * Gets mime type
     * 
     * @return mime type
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Sets mime type
     * 
     * @param mimetype the mime-type
     */
    public void setMimeType(String mimetype) {
        mMimeType = mimetype;
    }

    /**
     * Gets size
     * 
     * @return size
     */
    public int getSize() {
        return mSize;
    }

    /**
     * Sets size
     * 
     * @param size the size
     */
    public void setSize(int size) {
        mSize = size;
    }

    /**
     * Gets local MmContent
     * 
     * @param fileTransferId the file transfer ID
     * @return local content
     */
    public MmContent getLocalMmContent(String fileTransferId) {
        String iconName = FileTransferUtils.buildFileiconUrl(fileTransferId, mMimeType);
        Uri file = Uri.fromFile(new File(mRcsSettings.getFileIconRootDirectory().concat(iconName)));
        return ContentManager.createMmContent(file, mMimeType, mSize, iconName);
    }
}

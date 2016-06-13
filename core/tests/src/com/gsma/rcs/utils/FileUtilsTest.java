/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.utils;

import com.gsma.rcs.core.content.FileContent;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Philippe LEMORDANT.
 */
public class FileUtilsTest {

    private static String sExternalDir = Environment.getExternalStorageDirectory().toString()
            + File.separator;

    /**
     * Create a file located in the assets directory onto the SDCARD for test purposes
     * 
     * @param ctx the context
     * @param fileName the filename
     * @return the descriptor of the created file
     * @throws IOException
     */
    public static File createFileOnSdCard(Context ctx, String fileName) throws IOException {
        InputStream stream = ctx.getAssets().open(fileName);
        byte[] fileBytes = new byte[stream.available()];
        stream.read(fileBytes);
        stream.close();
        fileName = sExternalDir + fileName;
        File catFile = new File(fileName);
        if (catFile.exists()) {
            catFile.delete();
        }
        FileOutputStream fos = new FileOutputStream(catFile, true);
        fos.write(fileBytes);
        fos.close();
        return catFile;
    }

    public static FileContent getFileContent(Context ctx, Uri file) {
        String filename = FileUtils.getFileName(ctx, file);
        long fileSize = FileUtils.getFileSize(ctx, file);
        return new FileContent(file, fileSize, filename);
    }

    public static boolean doesFileExist(Uri uri) {
        File file = new File(uri.getPath());
        return file.exists();
    }

    public static final class FileTransferHolder {
        public final ContactId mContact;
        public final String mFtId;
        public final long mTimestamp;
        public final long mTimestampSent;
        public final long mFileExpiration;
        public final RcsService.Direction mDir;
        public final FileTransfer.State mState;
        public final FileTransfer.ReasonCode mReason;
        public final Uri mFile;
        public final Uri mFileIcon;

        public FileTransferHolder(ContactId contact, String ftId, long timestamp,
                long timestampSent, RcsService.Direction dir, FileTransfer.State state,
                FileTransfer.ReasonCode reason, Uri file, long fileExpiration, Uri fileIcon) {
            mContact = contact;
            mFtId = ftId;
            mTimestamp = timestamp;
            mTimestampSent = timestampSent;
            mFileExpiration = fileExpiration;
            mDir = dir;
            mState = state;
            mReason = reason;
            mFile = file;
            mFileIcon = fileIcon;
        }
    }
}

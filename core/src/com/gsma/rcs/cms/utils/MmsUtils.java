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
 *
 ******************************************************************************/

package com.gsma.rcs.cms.utils;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.MimeManager;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MmsUtils {

    private static final int MAX_BUF_READ_SIZE = 1024;

    public static byte[] getContent(ContentResolver contentResolver, Uri file)
            throws FileAccessException {
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream is = contentResolver.openInputStream(file);
            if (is == null) {
                throw new FileAccessException("Failed to read Uri=" + file);
            }
            bis = new BufferedInputStream(is);
            byte[] buf = new byte[MAX_BUF_READ_SIZE];
            int n;
            while ((n = bis.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();

        } catch (IOException e) {
            throw new FileAccessException("Failed to read Uri=" + file, e);

        } finally {
            CloseableUtils.tryToClose(bis);
        }
    }

    /**
     * Save MMS to file system
     * 
     * @param rcsSettings the RCS settings accessor
     * @param mimeType the mime type
     * @param mmsId the MMS Id
     * @param data the MMS data
     * @return the file URI
     * @throws FileAccessException
     */
    public static Uri saveContent(RcsSettings rcsSettings, String mimeType, String mmsId,
            byte[] data) throws FileAccessException {
        String fileName = rcsSettings.getMmsRootDirectory() + File.separator + mmsId + "_"
                + DateUtils.getMmsFileDate(System.currentTimeMillis()) + "."
                + MimeManager.getInstance().getExtensionFromMimeType(mimeType);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            File file = new File(fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos, 8 * 1024);
            bos.write(data);
            return Uri.fromFile(file);

        } catch (IOException e) {
            throw new FileAccessException("Failed to save MMS file=" + fileName, e);

        } finally {
            CloseableUtils.tryToClose(bos);
            CloseableUtils.tryToClose(fos);
        }
    }
}

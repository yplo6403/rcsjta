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

package com.gsma.rcs.provider.xms;

import com.gsma.services.rcs.cms.MmsPartLog;

import android.net.Uri;

/**
 * Xms data constants
 */
public class PartData {
    /**
     * Database URIs
     */
    public static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.xms/part");

    /**
     * Primary key unique ID
     */
    public static final String KEY_PART_ID = MmsPartLog.BASECOLUMN_ID;

    /**
     * Unique message identifier.
     */
    public static final String KEY_MESSAGE_ID = MmsPartLog.MESSAGE_ID;

    /**
     * Multipurpose Internet Mail Extensions (MIME) type of message.
     */
    public static final String KEY_MIME_TYPE = MmsPartLog.MIME_TYPE;

    /**
     * File name.
     */
    public static final String KEY_FILENAME = MmsPartLog.FILENAME;

    /**
     * File size.
     */
    public static final String KEY_FILESIZE = MmsPartLog.FILESIZE;

    /**
     * URI of the file or body text depending on the mime type.
     */
    public static final String KEY_CONTENT = MmsPartLog.CONTENT;

    /**
     * Byte array of the file icon.
     */
    public static final String KEY_FILEICON = MmsPartLog.FILEICON;

}

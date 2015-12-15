/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010 France Telecom S.A.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package com.gsma.services.rcs.cms;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Content provider for MMS part
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class MmsPartLog {
    /**
     * Content provider URI for chat messages
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.xms/part");

    /**
     * The name of the column containing the unique id across provider tables.
     * <p/>
     * Type: INTEGER
     * </P>
     */
    public static final String BASECOLUMN_ID = BaseColumns._ID;
    /**
     * The name of the column containing the message ID.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String MESSAGE_ID = "msg_id";
    /**
     * The name of the column containing the MIME-TYPE of the message part.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String MIME_TYPE = "mime_type";
    /**
     * The name of the column containing the filename or null
     * if the mime-type column is "text/plain".
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String FILENAME = "filename";
    /**
     * The name of the column containing the file size or null
     * if the mime-type column is "text/plain".
     * <p/>
     * Type: INTEGER
     * </P>
     */
    public static final String FILESIZE = "filesize";
    /**
     * The name of the column containing the URI of the file or the
     * body text depending on the mime-type column content.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String CONTENT = "content";
    /**
     * The name of the column containing the byte array of the file icon.
     * <p/>
     * Type: BLOB
     * </P>
     */
    public static final String FILEICON = "fileicon";
    /**
     * The name of the column containing the MSISDN of the remote contact.
     * <p/>
     * Type: TEXT
     * </P>
     */
    public static final String CONTACT = "contact";

    /**
     * Message MIME-types
     */
    public static class MimeType {

        /**
         * MIME-type of body part
         */
        public static final String TEXT_MESSAGE = "text/plain";

        /**
         * MIME-type of SMIL part
         */
        public static final String APPLICATION_SMIL = "application/smil";

    }
}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.cms.provider.xms;

import android.net.Uri;

/**
 * Xms data constants
 * 
 */
public class PartData {
    /**
     * Database URIs
     */
    public static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.cms.xms/part");
    
    /**
     * Database filename
     */    
    public static final String DATABASE_NAME = "rcs_cms_xms.db";

    /**
     * Database table
     */    

    public static final String TABLE_PART = "part";

    /**
     * Unique ID
     */
    public static final String KEY_BASECOLUMN_ID = "_id";

    /**
     * Native Part Id
     */
    public static final String KEY_NATIVE_ID= "native_id";

    /**
     * MessageId
     */
    public static final String KEY_MESSAGE_ID = "message_id";
    
    /**
     * Content type
     */
    public static final String KEY_CONTENT_TYPE = "content_type";

    /**
     * File name
     */
    public static final String KEY_CONTENT_ID = "content_id";

    /**
     * Data
     */
    public static final String KEY_DATA = "_data";

    /**
     * Thumb
     */
    public static final String KEY_THUMB = "thumb";
    /**
     * Text content
     */
    public static final String KEY_TEXT = "text";
}

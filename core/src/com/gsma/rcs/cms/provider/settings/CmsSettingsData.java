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

package com.gsma.rcs.cms.provider.settings;

import android.net.Uri;

/**
 * @author VGZL8743
 *
 */
public class CmsSettingsData {
    /**
     * Content provider URI
     */
    /* package private */static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.cms.settings/setting");

    /**
     * Key of the Rcs configuration parameter
     */
    /* package private */static final String KEY_KEY = "key";

    /**
     * Value of the Rcs configuration parameter
     */
    /* package private */static final String KEY_VALUE = "value";

    public static final String CMS_IMAP_SERVER_ADDRESS = "cms_imap_server_address";
    /* package private */static final String DEFAULT_CMS_IMAP_SERVER_ADDRESS = "imap://";

    public static final String CMS_IMAP_USER_LOGIN = "cms_imap_user_login";
    /* package private */static final String DEFAULT_CMS_IMAP_USER_LOGIN = "";

    public static final String CMS_IMAP_USER_PWD = "cms_imap_user_pwd";
    /* package private */static final String DEFAULT_CMS_IMAP_USER_PWD = "";

    public static final String CMS_RCS_MESSAGE_FOLDER = "cms_rcs_message_folder";
    /* package private */static final String DEFAULT_CMS_RCS_MESSAGE_FOLDER = "RCSMessageStore";
    
    public static final String CMS_MY_NUMBER = "cms_rcs_my_number";
    /* package private */static final String DEFAULT_CMS_MY_NUMBER = "+33";
}

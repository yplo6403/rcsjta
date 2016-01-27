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

package com.gsma.rcs.cms.integration;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

public class RcsSettingsMock {

    private static final String mServerAddress = "imap://172.20.65.102";
    private static final String mUserLogin = "test_integration";
    private static final String mUserPwd = "test_integration";
    private static final String mDefaultDirectory = "Default";
    private static final String mDirectorySeparator = "/";
    private static final String mContact = "+33601020304";

    private static String mOriServerAddress;
    private static String mOriUserLogin;
    private static String mOriUserPwd;
    private static String mOriDefaultDirectory;
    private static String mOriDirectorySeparator;
    private static ContactId mOriContact;

    public static RcsSettings getMockSettings(Context context) {
        RcsSettings settings = RcsSettings.createInstance(new LocalContentResolver(context));

        mOriServerAddress = settings.getMessageStoreUrl();
        mOriUserLogin = settings.getMessageStoreUser();
        mOriUserPwd = settings.getMessageStorePwd();
        mOriDefaultDirectory = settings.getMessageStoreDefaultDirectoryName();
        mOriDirectorySeparator = settings.getMessageStoreDirectorySeparator();
        mOriContact = settings.getUserProfileImsUserName();

        settings.setMessageStoreUrl(mServerAddress);
        settings.setMessageStoreUser(mUserLogin);
        settings.setMessageStorePwd(mUserPwd);
        settings.setMessageStoreDefaultDirectoryName(mDefaultDirectory);
        settings.setMessageStoreDirectorySeparator(mDirectorySeparator);
        settings.setUserProfileImsUserName(ContactUtil
                .createContactIdFromTrustedData("+33601020304"));
        return settings;
    }

    public static void restoreSettings() {
        RcsSettings settings = RcsSettings.createInstance(null);
        settings.setMessageStoreUrl(mOriServerAddress);
        settings.setMessageStoreUser(mOriUserLogin);
        settings.setMessageStorePwd(mOriUserPwd);
        settings.setMessageStoreDefaultDirectoryName(mOriDefaultDirectory);
        settings.setMessageStoreDirectorySeparator(mOriDirectorySeparator);
        settings.setUserProfileImsUserName(mOriContact);
    }

}

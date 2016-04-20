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

package com.gsma.rcs.core.cms.utils;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.services.rcs.contact.ContactId;

public class CmsUtils {

    public static String contactToCmsFolder(ContactId contactId) {
        return Constants.CMS_ROOT_DIRECTORY + Constants.CMS_DIRECTORY_SEPARATOR
                + Constants.TEL_PREFIX
                + contactId.toString();
    }

    public static String contactToHeader(ContactId contactId) {
        return Constants.TEL_PREFIX + contactId.toString();
    }

    public static ContactId cmsFolderToContact(String cmsFolder) {
        String contact = StringUtils.removeQuotes(cmsFolder);
        String prefix = Constants.CMS_ROOT_DIRECTORY + Constants.CMS_DIRECTORY_SEPARATOR + (Constants.TEL_PREFIX);
        if (cmsFolder.startsWith(prefix)) {
            contact = cmsFolder.substring(prefix.length());
        }
        PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(contact);
        if (phoneNumber != null) {
            return ContactUtil.createContactIdFromValidatedData(phoneNumber);
        }
        return null;
    }

    public static String groupChatToCmsFolder(String conversationId,
            String contributionId) {
        return Constants.CMS_ROOT_DIRECTORY + Constants.CMS_DIRECTORY_SEPARATOR + conversationId
                + Constants.CMS_DIRECTORY_SEPARATOR + contributionId;
    }

    public static String cmsFolderToChatId(String cmsFolder) {
        String folder = StringUtils.removeQuotes(cmsFolder);
        String prefix =Constants.CMS_ROOT_DIRECTORY + Constants.CMS_DIRECTORY_SEPARATOR;
        if (cmsFolder.startsWith(prefix)) {
            folder = folder.substring(prefix.length());
        }
        String[] val = folder.split(Constants.CMS_DIRECTORY_SEPARATOR, 2);
        if (val.length != 2) {
            return null;
        }
        return val[0];
    }
}

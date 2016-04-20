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

package com.gsma.rcs.core.cms.utils;

import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class CmsUtilsTest extends AndroidTestCase {


    public void test() throws RcsPermissionDeniedException {
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        ContactId contact = contactUtils.formatContact("+33600112233");
        String cmsFolder = "Default/tel:"+contact;
        Assert.assertEquals(cmsFolder, CmsUtils.contactToCmsFolder(contact));
        Assert.assertEquals("tel:"+contact, CmsUtils.contactToHeader(contact));
        Assert.assertEquals(contact, CmsUtils.cmsFolderToContact(cmsFolder));
    }

}

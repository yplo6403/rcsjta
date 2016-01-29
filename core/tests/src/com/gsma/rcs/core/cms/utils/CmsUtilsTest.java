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

import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class CmsUtilsTest extends AndroidTestCase {

    private RcsSettings mRcsSettings;

    protected void setUp() throws Exception {
        super.setUp();
        com.gsma.services.rcs.contact.ContactUtil.getInstance(getContext());
        mRcsSettings = RcsSettingsMock.getMockSettings(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        RcsSettingsMock.restoreSettings();
    }

    public void test() {

        ContactId contactId = ContactUtil.createContactIdFromTrustedData("+33600112233");
        String header = "tel:+33600112233";
        String cmsFolder = "Default/tel:+33600112233";

        Assert.assertEquals("Default/tel:+33600112233",
                CmsUtils.contactToCmsFolder(mRcsSettings, contactId));
        Assert.assertEquals("tel:+33600112233", CmsUtils.contactToHeader(contactId));
        Assert.assertEquals("+33600112233", CmsUtils.headerToContact(header).toString());
        Assert.assertEquals("+33600112233", CmsUtils.cmsFolderToContact(mRcsSettings, cmsFolder)
                .toString());
    }

}

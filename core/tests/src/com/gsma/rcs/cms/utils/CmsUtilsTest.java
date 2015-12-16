package com.gsma.rcs.cms.utils;

import android.test.AndroidTestCase;

import com.gsma.rcs.cms.integration.RcsSettingsMock;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import junit.framework.Assert;

public class CmsUtilsTest extends AndroidTestCase{

    protected void setUp() throws Exception {
        super.setUp();
        com.gsma.services.rcs.contact.ContactUtil.getInstance(getContext());
    }
    public void test(){

        RcsSettings rcsSettings = RcsSettingsMock.getRcsSettings(getContext());
        ContactId contactId = ContactUtil.createContactIdFromTrustedData("+33600112233");
        String header = "tel:+33600112233";
        String cmsFolder = "Default/tel:+33600112233";

        Assert.assertEquals("Default/tel:+33600112233",CmsUtils.contactToCmsFolder(rcsSettings, contactId));
        Assert.assertEquals("tel:+33600112233", CmsUtils.contactToHeader(contactId));
        Assert.assertEquals("+33600112233", CmsUtils.headerToContact(header).toString());
        Assert.assertEquals("+33600112233", CmsUtils.cmsFolderToContact(rcsSettings,cmsFolder).toString());
    }

}

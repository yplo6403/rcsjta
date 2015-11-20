package com.gsma.rcs.cms.utils;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class SipUtilsTest extends AndroidTestCase{

    public void test(){

        String contact1 = "+33600112233";
        String contact2 = "tel:+33600112233";

        Assert.assertEquals("<tel:+33600112233>", SipUtils.asSipContact(contact1));
        Assert.assertEquals("<tel:+33600112233>", SipUtils.asSipContact(contact2));

        Assert.assertEquals("+33600112233", SipUtils.asContact("<tel:+33600112233>"));
        Assert.assertEquals("+33600112233", SipUtils.asContact("tel:+33600112233"));
    }

}

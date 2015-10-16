package com.gsma.rcs.cms.utils;

import com.gsma.rcs.utils.Base64;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class HeaderCorrelatorUtilsTest extends AndroidTestCase{

    public void test(){
        
        String str = "Bonjour!";        
        Assert.assertEquals(str,HeaderCorrelatorUtils.buildHeader(str));
        
        str = "Bienvenu à l'été indien";
        String base64 = new String(Base64.encodeBase64(str.getBytes()));
        String expected = new StringBuilder(HeaderCorrelatorUtils.PREFIX).append(base64).append(HeaderCorrelatorUtils.SUFFIX).toString();
        System.out.print(expected);
        Assert.assertEquals(expected, HeaderCorrelatorUtils.buildHeader(str));

        str = "На здоровье, мой друг";
        expected = new StringBuilder("=?utf-8?b?0J3QsCDQt9C00L7RgNC+0LLRjNC1LCDQvNC+0Lkg0LTRgNGD0LM=?=").toString();
        System.out.print(expected);
        Assert.assertEquals(expected, HeaderCorrelatorUtils.buildHeader(str));

    }       
}

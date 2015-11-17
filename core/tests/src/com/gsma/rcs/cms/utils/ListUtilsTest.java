package com.gsma.rcs.cms.utils;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Arrays;
import java.util.List;

public class ListUtilsTest extends AndroidTestCase{

    public void test(){

        List<Integer> list;

        list = Arrays.asList(new Integer[]{1,2,3,4,5});
        String expected ="1,2,3,4,5";
        Assert.assertEquals(expected, ListUtils.join(list,","));

        list = Arrays.asList(new Integer[]{1});
        expected ="1";
        Assert.assertEquals(expected, ListUtils.join(list,","));

    }


}

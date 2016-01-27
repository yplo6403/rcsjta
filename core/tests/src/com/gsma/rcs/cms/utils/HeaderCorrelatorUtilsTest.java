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

package com.gsma.rcs.cms.utils;

import com.gsma.rcs.utils.Base64;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class HeaderCorrelatorUtilsTest extends AndroidTestCase {

    public void test() {

        String str = "Bonjour!";
        Assert.assertEquals(str, HeaderCorrelatorUtils.buildHeader(str));

        str = "Bienvenu à l'été indien";
        String base64 = new String(Base64.encodeBase64(str.getBytes()));
        String expected = new StringBuilder(HeaderCorrelatorUtils.PREFIX).append(base64)
                .append(HeaderCorrelatorUtils.SUFFIX).toString();
        System.out.print(expected);
        Assert.assertEquals(expected, HeaderCorrelatorUtils.buildHeader(str));

        str = "На здоровье, мой друг";
        expected = new StringBuilder(
                "=?utf-8?b?0J3QsCDQt9C00L7RgNC+0LLRjNC1LCDQvNC+0Lkg0LTRgNGD0LM=?=").toString();
        System.out.print(expected);
        Assert.assertEquals(expected, HeaderCorrelatorUtils.buildHeader(str));

    }
}

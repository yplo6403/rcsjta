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

package com.gsma.rcs.core.cms.protocol.message.cpim;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.protocol.message.HeaderPart;
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class CpimMessageTest extends AndroidTestCase {

    private String expectedPayload = new StringBuilder().append("header1: value1")
            .append(Constants.CRLF).append("header2: value2").append(Constants.CRLF)
            .append(Constants.CRLF).append("Content-Type: myContentType").append(Constants.CRLF)
            .append(Constants.CRLF).append("myContent").toString();

    public void testFromPayload() throws CmsSyncHeaderFormatException {
        CpimMessage cpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
        cpimMessage.parsePayload(expectedPayload);
        Assert.assertEquals("value1", cpimMessage.getHeader("header1"));
        Assert.assertEquals("value2", cpimMessage.getHeader("header2"));
        Assert.assertEquals("myContentType", cpimMessage.getBody().getContentType());
        Assert.assertEquals("myContent", ((TextCpimBody) cpimMessage.getBody()).getContent());
    }

    public void test() {

        HeaderPart headers = new HeaderPart();
        headers.addHeader("header1", "value1");
        headers.addHeader("header2", "value2");
        CpimBody cpimBody = new TextCpimBody("myContentType", "myContent");

        CpimMessage cpimMessage = new CpimMessage(headers, cpimBody);
        Assert.assertEquals(expectedPayload, cpimMessage.getPayload());
    }

}

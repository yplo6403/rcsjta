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

package com.gsma.rcs.cms.imap.message.cpim.text;

import com.gsma.rcs.cms.Constants;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class TextCpimBodyTest extends AndroidTestCase {

    public void testFromPayload() {

        StringBuilder payload = new StringBuilder();
        payload.append("Content-Type: text/plain").append(Constants.CRLFCRLF);
        payload.append("myContent");

        TextCpimBody textCpimBody = new TextCpimBody();
        textCpimBody.parseBody(payload.toString());
        Assert.assertEquals("text/plain", textCpimBody.getContentType());
        Assert.assertEquals("myContent", textCpimBody.getContent());
    }

    public void test() {

        StringBuilder expected = new StringBuilder();
        expected.append("Content-Type: myContentType").append(Constants.CRLFCRLF);
        expected.append("myContent");

        TextCpimBody textCpimBody = new TextCpimBody("myContentType", "myContent");
        String payload = textCpimBody.toPayload();
        Assert.assertEquals(expected.toString(), payload);
    }

}

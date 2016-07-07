/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.utils;

import android.test.AndroidTestCase;

public class Base64Test extends AndroidTestCase {

    public final void testBase64() {
        String ss = "2 + 2 = quatre, non 5?";
        assertEquals(Base64.encodeBase64ToString(ss.getBytes()), "MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==");
        assertEquals(new String(Base64.encodeBase64(ss.getBytes())),
                "MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==");
        assertEquals(Base64.encodeBase64ToString(Base64
                .decodeBase64(("MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==").getBytes())),
                "MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==");
        assertEquals(
                new String(Base64.decodeBase64(("MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==").getBytes())),
                ss);
    }

}

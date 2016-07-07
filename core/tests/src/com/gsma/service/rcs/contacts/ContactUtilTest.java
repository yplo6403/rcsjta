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

package com.gsma.service.rcs.contacts;

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;

public class ContactUtilTest extends AndroidTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        RcsSettings settings = RcsSettingsMock.getMockSettings(context);
        AndroidFactory.setApplicationContext(context, settings);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        RcsSettingsMock.restoreSettings();
    }

    public void testGetValidPhoneNumberFromUri() {
        String telUri = "<tel:+33640519308?Accept-Contact=%2Bsip.instance%3D%22%3Curn%3Agsma%3Aimei%3A35824005-944763-1%3E%22>";
        ContactUtil.PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(telUri);
        assertNotNull(number);
        assertEquals("+33640519308", number.getNumber());
    }

}

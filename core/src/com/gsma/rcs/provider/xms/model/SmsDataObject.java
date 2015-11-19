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

package com.gsma.rcs.provider.xms.model;

import com.gsma.rcs.cms.utils.HeaderCorrelatorUtils;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

public class SmsDataObject extends XmsDataObject {

    private final String mCorrelator;

    public SmsDataObject(String messageId, ContactId contact, String body,
            RcsService.Direction dir, long timestamp, long nativeId) {
        super(messageId, contact, body, XmsMessageLog.MimeType.TEXT_MESSAGE, dir, timestamp, nativeId);
        mCorrelator = HeaderCorrelatorUtils.buildHeader(mBody);
    }

    @Override
    public String getCorrelator() {
        return mCorrelator;
    }
}

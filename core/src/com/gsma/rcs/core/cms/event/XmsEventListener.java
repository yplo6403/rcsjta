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
 *
 ******************************************************************************/

package com.gsma.rcs.core.cms.event;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;

/**
 * Interface for events from SMS/MMS native provider.
 */
public interface XmsEventListener {

    void onIncomingSms(SmsDataObject message);

    void onOutgoingSms(SmsDataObject message);

    void onIncomingMms(MmsDataObject message);

    void onOutgoingMms(MmsDataObject message) throws FileAccessException;

    void onSmsMessageStateChanged(SmsDataObject sms);

    void onMmsMessageStateChanged(MmsDataObject mms);
}

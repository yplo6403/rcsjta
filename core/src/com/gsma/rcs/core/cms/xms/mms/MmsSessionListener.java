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

package com.gsma.rcs.core.cms.xms.mms;

import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Created by yplo6403 on 08/12/2015.
 */
public interface MmsSessionListener {

    /**
     * MMS transfer error
     *
     * @param reason The reason code
     * @param contact The remote contact
     * @param mmsId The messageId
     */
    void onMmsTransferError(XmsMessage.ReasonCode reason, ContactId contact, String mmsId);

    /**
     * MMS transferred
     * 
     * @param contact The remote contact
     */
    void onMmsTransferred(ContactId contact, String mmsId);

    /**
     * MMS transfer is started
     *
     * @param contact The remote contact
     */
    void onMmsTransferStarted(ContactId contact, String mmsId);
}

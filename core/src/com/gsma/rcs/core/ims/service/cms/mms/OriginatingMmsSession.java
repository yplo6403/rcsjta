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

package com.gsma.rcs.core.ims.service.cms.mms;

import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by yplo6403 on 07/12/2015.
 */
public class OriginatingMmsSession implements Runnable {

    private static final Logger sLogger = Logger.getLogger(OriginatingMmsSession.class
            .getSimpleName());

    private final String mMmsId;
    private final ContactId mContact;
    private final String mSubject;
    private final XmsLog mXmsLog;
    private final Set<MmsDataObject.MmsPart> mParts;
    private final List<MmsSessionListener> mListeners;

    /**
     * @param xmsLog The XMS log accessor
     * @param mmsId The message ID
     * @param contact The remote contact
     * @param subject The subject
     * @param parts The MMS attachement parts
     */
    public OriginatingMmsSession(XmsLog xmsLog, String mmsId, ContactId contact, String subject,
            Set<MmsDataObject.MmsPart> parts) {
        mXmsLog = xmsLog;
        mMmsId = mmsId;
        mContact = contact;
        mSubject = subject;
        mParts = parts;
        mListeners = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            boolean logActivated = sLogger.isActivated();
            if (logActivated) {
                sLogger.debug("Send MMS ID " + mMmsId + " to contact " + mContact + " subject='"
                        + mSubject + "'");
            }
            // TODO send MMS
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to send MMS!", e);
        }

    }

    public void addListener(MmsSessionListener listener) {
        mListeners.add(listener);
    }
}

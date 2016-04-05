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

import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.cms.xms.mms.MmsSessionListener;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;

public class MmsSessionHandler implements MmsSessionListener {

    private final CmsLog mCmsLog;
    private final XmsLog mXmsLog;
    private final RcsSettings mSettings;
    private final CmsSyncScheduler mCmsSyncScheduler;

    /**
     * Default constructor
     *
     * @param cmsLog the IMAP log accessor
     * @param settings the RCS settings accessor
     * @param cmsSyncScheduler the CMS synchronization scheduler
     */
    public MmsSessionHandler(CmsLog cmsLog, XmsLog xmsLog, RcsSettings settings,
                             CmsSyncScheduler cmsSyncScheduler) {
        mCmsLog = cmsLog;
        mXmsLog = xmsLog;
        mSettings = settings;
        mCmsSyncScheduler = cmsSyncScheduler;
    }

    @Override
    public void onMmsTransferError(ReasonCode reason, ContactId contact, String mmsId) {
    }

    @Override
    public void onMmsTransferred(ContactId contact, String mmsId) {
        mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(contact),
                ReadStatus.READ, CmsObject.DeleteStatus.NOT_DELETED, mSettings
                        .getMessageStorePushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.MMS, mmsId, null));
        if (mCmsSyncScheduler != null) {
            MmsDataObject mms = (MmsDataObject) mXmsLog.getXmsDataObject(mmsId);
            mCmsSyncScheduler.schedulePushMessages(mms.getContact());
        }
    }

    @Override
    public void onMmsTransferStarted(ContactId contact, String mmsId) {
    }
}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms.event;

import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.service.cms.mms.MmsSessionListener;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;

public class MmsSessionHandler implements MmsSessionListener {

    private final ImapLog mImapLog;
    private final XmsLog mXmsLog;
    private final RcsSettings mSettings;
    private final ImapEventFrameworkHandler mImapEventFrameworkHandler;

    /**
     * Default constructor
     *
     * @param imapLog the IMAP log accessor
     * @param settings the RCS settings accessor
     */
    public MmsSessionHandler(ImapLog imapLog, XmsLog xmsLog, RcsSettings settings,
            ImapEventFrameworkHandler imapEventFrameworkHandler) {
        mImapLog = imapLog;
        mXmsLog = xmsLog;
        mSettings = settings;
        mImapEventFrameworkHandler = imapEventFrameworkHandler;
    }

    @Override
    public void onMmsTransferError(ReasonCode reason, ContactId contact, String mmsId) {
    }

    @Override
    public void onMmsTransferred(ContactId contact, String mmsId) {
        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings, contact),
                ReadStatus.READ, MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getMessageStorePushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.MMS, mmsId, null));

        MmsDataObject mms = (MmsDataObject) mXmsLog.getXmsDataObject(mmsId);
        if (Direction.INCOMING == mms.getDirection()) {
            mImapEventFrameworkHandler.onIncomingMms(mms);
        } else {
            mImapEventFrameworkHandler.onOutgoingMms(mms);
        }
    }

    @Override
    public void onMmsTransferStarted(ContactId contact, String mmsId) {
    }
}

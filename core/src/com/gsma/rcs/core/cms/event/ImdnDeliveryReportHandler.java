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

import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

public class ImdnDeliveryReportHandler implements ImdnDeliveryReportListener {

    private static final Logger sLogger = Logger.getLogger(ImdnDeliveryReportHandler.class
            .getSimpleName());
    protected final CmsLog mCmsLog;

    /**
     * Default constructor
     *
     * @param cmsLog the IMAP log accessor
     */
    public ImdnDeliveryReportHandler(CmsLog cmsLog) {
        mCmsLog = cmsLog;
    }

    @Override
    public void onDeliveryReport(String chatId, ContactId remote, String messageId, String imdnMessageId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeliveryReport: " + chatId + ", " + remote.toString() + ", "
                    + imdnMessageId);
        }
        if (chatId.equals(remote.toString())) { // OneToOne
            onDeliveryReport(remote, messageId, imdnMessageId);
        } else { // GC
            onDeliveryReport(chatId, messageId, imdnMessageId);
        }
    }

    @Override
    public void onDeliveryReport(String chatId, String messageId, String imdnMessageId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeliveryReport: " + chatId + ", " + imdnMessageId);
        }
        if(mCmsLog.getChatData(messageId) == null){ // do not persist IMDN if chat message is not present
            return;
        }
        mCmsLog.addMessage(new CmsObject(CmsUtils.groupChatToCmsFolder(chatId, chatId),
                ReadStatus.READ, CmsObject.DeleteStatus.NOT_DELETED, PushStatus.PUSHED,
                MessageType.IMDN, imdnMessageId, null));
    }

    @Override
    public void onDeliveryReport(ContactId contact, String messageId, String imdnMessageId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeliveryReport: " + contact + ", " + imdnMessageId);
        }
        if(mCmsLog.getChatData(messageId) == null){// Do not persist IMDN if chat message is not present
            return;
        }
        mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(contact), ReadStatus.READ,
                CmsObject.DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.IMDN,
                imdnMessageId, null));
    }

}

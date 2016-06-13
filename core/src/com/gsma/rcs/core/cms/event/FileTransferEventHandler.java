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

import com.gsma.rcs.core.cms.event.framework.EventFrameworkManager;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

public class FileTransferEventHandler implements FileTransferListener {

    private static final Logger sLogger = Logger.getLogger(FileTransferEventHandler.class
            .getSimpleName());
    protected final MessagingLog mMessagingLog;
    protected final CmsLog mCmsLog;
    protected final RcsSettings mSettings;
    protected final EventFrameworkManager mEventFrameworkManager;
    protected final ImdnDeliveryReportListener mImdnDeliveryReportListener;

    /**
     * Default constructor
     *
     * @param eventFrameworkManager the event framework handler
     * @param cmsLog the IMAP log accessor
     * @param messagingLog the messaging log accessor
     * @param settings the RCS settings accessor
     * @param imdnDeliveryReportListener the listener for delivery report event
     */
    public FileTransferEventHandler(EventFrameworkManager eventFrameworkManager, CmsLog cmsLog,
            MessagingLog messagingLog, RcsSettings settings,
            ImdnDeliveryReportListener imdnDeliveryReportListener) {
        mEventFrameworkManager = eventFrameworkManager;
        mMessagingLog = messagingLog;
        mCmsLog = cmsLog;
        mSettings = settings;
        mImdnDeliveryReportListener = imdnDeliveryReportListener;
    }

    @Override
    public void onNewFileTransfer(ContactId contact, Direction direction, String transferId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onNewFileTransfer contact : " + contact.toString() + ", transferId : "
                    + transferId);
        }
        mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(contact),
                direction == Direction.INCOMING ? ReadStatus.UNREAD : ReadStatus.READ,
                CmsObject.DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.FILE_TRANSFER,
                transferId, null));
    }

    @Override
    public void onNewGroupFileTransfer(String chatId, Direction direction, String transferId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onNewFileTransfer chatId : " + chatId + ", transferId : " + transferId);
        }
        mCmsLog.addMessage(new CmsObject(CmsUtils.groupChatToCmsFolder(chatId, chatId),
                direction == Direction.INCOMING ? ReadStatus.UNREAD : ReadStatus.READ,
                CmsObject.DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.FILE_TRANSFER,
                transferId, null));
    }

    @Override
    public void onReadFileTransfer(String transferId) {
        if (sLogger.isActivated()) {
            sLogger.debug("onReadFileTransfer transferId : " + transferId);
        }
        mCmsLog.updateRcsReadStatus(MessageType.FILE_TRANSFER, transferId,
                ReadStatus.READ_REPORT_REQUESTED, null);
        if (mEventFrameworkManager != null) {
            if (mMessagingLog.isGroupFileTransfer(transferId)) {
                mEventFrameworkManager.updateFlagsForGroupChat(mMessagingLog
                        .getFileTransferChatId(transferId));
            } else {
                mEventFrameworkManager.updateFlagsForChat(mMessagingLog
                        .getFileTransferContact(transferId));
            }
        }
    }

    @Override
    public void onDeleteFileTransfer(ContactId contact, Set<String> transferIds) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteFileTransfer for contact : " + contact);
        }
        for (String transferId : transferIds) {
            mCmsLog.updateRcsDeleteStatus(MessageType.FILE_TRANSFER, transferId,
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
        }
        if (mEventFrameworkManager != null) {
            mEventFrameworkManager.updateFlagsForChat(contact);
        }
    }

    @Override
    public void onDeleteGroupFileTransfer(String chatId, Set<String> transferIds) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteGroupFileTransfer for chatId : " + chatId);
        }
        for (String transferId : transferIds) {
            mCmsLog.updateRcsDeleteStatus(MessageType.FILE_TRANSFER, transferId,
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
        }
        if (mEventFrameworkManager != null) {
            mEventFrameworkManager.updateFlagsForGroupChat(chatId);
        }
    }
}

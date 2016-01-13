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

import android.content.Context;

import com.gsma.rcs.cms.fordemo.ImapCommandController;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.cms.mms.MmsSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.broadcaster.IXmsMessageEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;

public class MmsSessionHandler implements MmsSessionListener {

    private static final Logger sLogger = Logger.getLogger(MmsSessionHandler.class.getSimpleName());
    private final Context mContext;
    private final ImapLog mImapLog;
    private final XmsLog mXmsLog;
    private final RcsSettings mSettings;
    private final ImapCommandController mImapCommandController;

    /**
     * Default constructor
     *
     * @param context
     * @param imapLog
     * @param settings
     */
    public MmsSessionHandler(Context context, ImapLog imapLog, XmsLog xmsLog, RcsSettings settings, ImapCommandController imapCommandController) {
        mContext = context;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
        mSettings = settings;
        mImapCommandController = imapCommandController;
    }

    @Override
    public void onMmsTransferError(ReasonCode reason, ContactId contact, String mmsId) {
    }

    @Override
    public void onMmsTransferred(ContactId contact, String mmsId) {

        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings, contact),
                ReadStatus.READ, MessageData.DeleteStatus.NOT_DELETED,
                mSettings.getCmsPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.MMS, mmsId, null));

        if (mImapCommandController != null) {
            MmsDataObject mms = (MmsDataObject) mXmsLog.getXmsDataObject(mmsId);
            if (Direction.INCOMING == mms.getDirection()) {
                mImapCommandController.onIncomingMms(mms);
            } else {
                mImapCommandController.onOutgoingMms(mms);
            }
        }
    }

    @Override
    public void onMmsTransferStarted(ContactId contact, String mmsId) {
    }
}

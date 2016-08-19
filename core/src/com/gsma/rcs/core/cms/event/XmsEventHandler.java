/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.event;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

public class XmsEventHandler implements XmsEventListener {

    private static final Logger sLogger = Logger.getLogger(XmsEventHandler.class.getName());

    private final XmsLog mXmsLog;
    private final CmsLog mCmsLog;
    private final CmsServiceImpl mCmsService;
    private final PushStatus mPushStatusSms;
    private final PushStatus mPushStatusMms;
    private CmsSyncScheduler mCmsSyncScheduler;

    /**
     * Default constructor
     *
     * @param cmsLog the IMAP log accessor
     * @param xmsLog the XMS log accessor
     * @param settings the RCS settings accessor
     * @param cmsService the CMS service impl
     */
    public XmsEventHandler(CmsLog cmsLog, XmsLog xmsLog, RcsSettings settings,
            CmsServiceImpl cmsService) {
        mXmsLog = xmsLog;
        mCmsLog = cmsLog;
        mCmsService = cmsService;
        mPushStatusSms = settings.shouldPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED;
        mPushStatusMms = settings.shouldPushMms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED;
    }

    public void setCmsSyncScheduler(CmsSyncScheduler cmsSyncScheduler) {
        mCmsSyncScheduler = cmsSyncScheduler;
    }

    private void insertIntoCmsThenScheduleSync(XmsDataObject message, MessageType msgType,
            PushStatus push) {
        ContactId contact = message.getContact();
        String folder = CmsUtils.contactToCmsFolder(contact);
        CmsXmsObject cms = new CmsXmsObject(msgType, folder, message.getMessageId(), push,
                ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, message.getNativeProviderId());
        mCmsLog.addXmsMessage(cms);
        if (mCmsSyncScheduler != null) {
            mCmsSyncScheduler.schedulePushMessages(contact);
        }
    }

    @Override
    public void onIncomingSms(SmsDataObject sms) {
        if (sLogger.isActivated()) {
            sLogger.debug("onIncomingSms: ".concat(sms.toString()));
        }
        mXmsLog.addSms(sms);
        insertIntoCmsThenScheduleSync(sms, MessageType.SMS, mPushStatusSms);
        mCmsService.broadcastNewMessage(sms.getMimeType(), sms.getMessageId());
    }

    @Override
    public void onOutgoingSms(SmsDataObject sms) {
        if (sLogger.isActivated()) {
            sLogger.debug("onOutgoingSms ".concat(sms.toString()));
        }
        mXmsLog.addSms(sms);
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        String msgId = message.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug("onIncomingMms ID=" + msgId + " contact=" + message.getContact());
        }
        mXmsLog.addIncomingMms(message);
        insertIntoCmsThenScheduleSync(message, MessageType.MMS, mPushStatusMms);
        mCmsService.broadcastNewMessage(message.getMimeType(), msgId);
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) throws FileAccessException {
        String msgId = mXmsLog.addOutgoingMms(message);
        if (sLogger.isActivated()) {
            sLogger.debug("onOutgoingMms ID=" + msgId + " contact=" + message.getContact());
        }
    }

    @Override
    public void onSmsMessageStateChanged(SmsDataObject sms) {
        if (sLogger.isActivated()) {
            sLogger.debug("onSmsMessageStateChanged SMS=" + sms);
        }
        ContactId contact = sms.getContact();
        String msgId = sms.getMessageId();
        XmsMessage.State state = sms.getState();
        XmsMessage.ReasonCode reason = sms.getReasonCode();
        boolean updated;
        switch (state) {
            case DELIVERED:
                long timestampDelivered = NtpTrustedTime.currentTimeMillis();
                updated = mXmsLog.setMessageDelivered(contact, msgId, timestampDelivered);
                break;

            case SENT:
                updated = mXmsLog.setMessageSent(contact, msgId, sms.getTimestampSent());
                break;

            default:
                updated = mXmsLog.setStateAndReasonCode(contact, msgId, state, reason);
        }
        if (updated) {
            if (XmsMessage.State.SENT == state) {
                insertIntoCmsThenScheduleSync(sms, MessageType.SMS, mPushStatusSms);
            }
            if (sLogger.isActivated()) {
                sLogger.debug("onMessageStateChanged msgId=" + msgId + ", state=" + state
                        + ", reason=" + reason);
            }
            mCmsService.broadcastMessageStateChanged(contact, sms.getMimeType(), msgId, state,
                    reason);
        }
    }

    @Override
    public void onMmsMessageStateChanged(MmsDataObject mms) {
        if (sLogger.isActivated()) {
            sLogger.debug("onMmsMessageStateChanged MMS=" + mms);
        }
        ContactId contact = mms.getContact();
        long nativeId = mms.getNativeProviderId();
        String msgId = mms.getMessageId();
        XmsMessage.State state = mms.getState();
        XmsMessage.ReasonCode reason = mms.getReasonCode();
        boolean updated = false;
        switch (state) {
            case DELIVERED:
                long timestampDelivered = NtpTrustedTime.currentTimeMillis();
                updated = mXmsLog.setMessageDelivered(contact, msgId, timestampDelivered);
                break;

            case SENT:
                if (mXmsLog.updateMmsMessageId(contact, nativeId, msgId)) {
                    updated = mXmsLog.setMessageSent(contact, msgId, mms.getTimestampSent());
                }
                break;

            default:
                if (mXmsLog.updateMmsMessageId(contact, nativeId, msgId)) {
                    updated = mXmsLog.setStateAndReasonCode(contact, msgId, state, reason);
                }
        }
        if (updated) {
            if (XmsMessage.State.SENT == state) {
                insertIntoCmsThenScheduleSync(mms, MessageType.MMS, mPushStatusMms);
            }
            if (sLogger.isActivated()) {
                sLogger.debug("onMessageStateChanged msgId=" + msgId + ", state=" + state
                        + ", reason=" + reason);
            }
            mCmsService.broadcastMessageStateChanged(contact, mms.getMimeType(), msgId, state,
                    reason);
        }
    }

}

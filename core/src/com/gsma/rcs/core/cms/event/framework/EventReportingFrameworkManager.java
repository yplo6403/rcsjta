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

package com.gsma.rcs.core.cms.event.framework;

import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventReportingFrameworkConfig;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

/**
 * This class is the entry point for the event framework.<br>
 * It allows:
 * <ul>
 * <li>- to push XMS messages on the message store with IMAP commands</li>
 * <li>- to update flags of messages on the message store with SIP or IMAP commands The protocol
 * used for updating flags depends on provisioning parameters.</li>
 * </ul>
 */
public class EventReportingFrameworkManager implements EventReportingFramework {

    private static final Logger sLogger = Logger.getLogger(EventReportingFrameworkManager.class
            .getSimpleName());

    private final RcsSettings mSettings;
    private final ImapEventReportingFrameworkManager mImapEventReportingFrameworkManager;
    private final SipEventReportingFrameworkManager mSipEventReportingFrameworkManager;
    private final InstantMessagingService mInstantMessagingService;

    /**
     * Constructor
     * 
     * @param context the context
     * @param scheduler the scheduler
     * @param instantMessagingService the IMS module
     * @param settings the RCS settings accessor
     */
    public EventReportingFrameworkManager(Context context, CmsSyncScheduler scheduler,
            InstantMessagingService instantMessagingService, CmsLog cmsLog, RcsSettings settings) {
        mInstantMessagingService = instantMessagingService;
        mImapEventReportingFrameworkManager = new ImapEventReportingFrameworkManager(scheduler);
        mSipEventReportingFrameworkManager = new SipEventReportingFrameworkManager(settings, cmsLog);
        mSipEventReportingFrameworkManager.start();
        mSettings = settings;
    }

    @Override
    public void pushSmsMessage(ContactId contact) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("pushSmsMessage");
        }
        if (mSettings.getMessageStorePushSms()) {
            mImapEventReportingFrameworkManager.pushXmsMessage(contact);
        } else {
            if (logActivated) {
                sLogger.info("Sms push is not allowed from settings");
            }
        }
    }

    @Override
    public void pushMmsMessage(ContactId contact) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("pushMmsMessage");
        }
        if (mSettings.getMessageStorePushMms()) {
            mImapEventReportingFrameworkManager.pushXmsMessage(contact);
        } else {
            if (logActivated) {
                sLogger.info("Mms push is not allowed from settings");
            }
        }
    }

    @Override
    public void stop() {
        mSipEventReportingFrameworkManager.terminate();
        mSipEventReportingFrameworkManager.interrupt();
    }

    @Override
    public void updateFlagsForXms(ContactId contact) {
        updateFlags(contact, null);
    }

    @Override
    public void updateFlagsForChat(ContactId contact) {
        updateFlags(contact, null);
    }

    @Override
    public void updateFlagsForGroupChat(String chatId) {
        updateFlags(null, chatId);
    }

    private void updateFlags(ContactId contactId, String chatId) {

        EventReportingFrameworkConfig cfg = mSettings.getEventReportingFrameworkConfig();
        if (EventReportingFrameworkConfig.DISABLED == cfg) {
            if (sLogger.isActivated()) {
                sLogger.debug("Event reporting framework is not enabled");
            }
            return;
        }

        boolean isOneToOne = contactId != null;

        // When enabled, try to report events with an established MSRP session first and with an
        // IMAP session otherwise
        if (EventReportingFrameworkConfig.ENABLED == cfg) {
            ChatSession chatSession;
            String cmsFolder;
            if (isOneToOne) {
                chatSession = mInstantMessagingService.getOneToOneChatSession(contactId);
                cmsFolder = CmsUtils.contactToCmsFolder(contactId);
            } else {
                chatSession = mInstantMessagingService.getGroupChatSession(chatId);
                cmsFolder = CmsUtils.groupChatToCmsFolder(chatId, chatId);
            }
            if (chatSession != null && chatSession.isMediaEstablished()) {
                mSipEventReportingFrameworkManager.tryToReportEvents(cmsFolder, chatSession);
            } else { // no MSRP session available, use IMAP session instead
                if (isOneToOne) {
                    mImapEventReportingFrameworkManager.updateFlags(contactId);
                } else {
                    mImapEventReportingFrameworkManager.updateFlags(chatId);
                }
            }
            return;
        }

        if (EventReportingFrameworkConfig.IMAP_ONLY == cfg) {
            if (isOneToOne) {
                mImapEventReportingFrameworkManager.updateFlags(contactId);
            } else {
                mImapEventReportingFrameworkManager.updateFlags(chatId);
            }
            return;
        }
    }

}

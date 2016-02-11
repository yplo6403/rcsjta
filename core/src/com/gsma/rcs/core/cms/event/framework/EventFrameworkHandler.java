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
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventFrameworkMode;
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
public class EventFrameworkHandler implements EventFramework {

    private static final Logger sLogger = Logger.getLogger(EventFrameworkHandler.class
            .getSimpleName());

    private final RcsSettings mSettings;
    private final ImapEventFrameworkHandler mImapEventFrameworkHandler;
    private final SipEventFrameworkHandler mSipEventFrameworkHandler;

    /**
     * Constructor
     * 
     * @param context the context
     * @param scheduler the scheduler
     * @param settings the RCS settings accessor
     */
    public EventFrameworkHandler(Context context, CmsSyncScheduler scheduler, RcsSettings settings) {
        mImapEventFrameworkHandler = new ImapEventFrameworkHandler(scheduler);
        mSipEventFrameworkHandler = new SipEventFrameworkHandler(context, settings);
        mSettings = settings;
    }

    @Override
    public void pushSmsMessage(ContactId contact) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("pushSmsMessage");
        }
        if (mSettings.getMessageStorePushSms()) {
            mImapEventFrameworkHandler.pushXmsMessage(contact);
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
            mImapEventFrameworkHandler.pushXmsMessage(contact);
        } else {
            if (logActivated) {
                sLogger.info("Mms push is not allowed from settings");
            }
        }
    }

    @Override
    public void updateFlagsForXms() {
        EventFrameworkMode xmsMode = mSettings.getEventFrameworkForXms();
        if (EventFrameworkMode.DISABLED == xmsMode) {
            if (sLogger.isActivated()) {
                sLogger.debug("Event framework is not enabled for Xms messages");
            }
            return;
        }
        if (EventFrameworkMode.IMAP == xmsMode) {
            mImapEventFrameworkHandler.updateFlags(xmsMode, mSettings.getEventFrameworkForChat());
        } else if (EventFrameworkMode.SIP == xmsMode) {
            mSipEventFrameworkHandler.updateFlags(xmsMode, mSettings.getEventFrameworkForChat());
        }
    }

    @Override
    public void updateFlagsForChat() {
        EventFrameworkMode chatMode = mSettings.getEventFrameworkForChat();
        if (EventFrameworkMode.DISABLED == chatMode) {
            if (sLogger.isActivated()) {
                sLogger.debug("Event framework is not enabled for Chat messages");
            }
            return;
        }
        if (EventFrameworkMode.IMAP == chatMode) {
            mImapEventFrameworkHandler.updateFlags(mSettings.getEventFrameworkForXms(), chatMode);
        } else if (EventFrameworkMode.SIP == chatMode) {
            mSipEventFrameworkHandler.updateFlags(mSettings.getEventFrameworkForXms(), chatMode);
        }
    }

}

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
import com.gsma.rcs.provider.settings.RcsSettingsData.EventFrameworkMode;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

/**
 * This class is in charge of updating flags on the message store using IMAP commands
 */
public class ImapEventFrameworkHandler {

    private static final Logger sLogger = Logger.getLogger(ImapEventFrameworkHandler.class
            .getSimpleName());

    private final CmsSyncScheduler mScheduler;

    /* package private */ImapEventFrameworkHandler(CmsSyncScheduler scheduler) {
        mScheduler = scheduler;
    }

    /* package private */void pushXmsMessage(ContactId contact) {
        if (!mScheduler.schedulePushMessages(contact)) {
            if (sLogger.isActivated()) {
                sLogger.info("--> can not schedule push message operation");
            }
        }
    }

    /* package private */void updateFlags(EventFrameworkMode xmsMode, EventFrameworkMode chatMode) {
        if (!mScheduler.scheduleUpdateFlags(xmsMode, chatMode)) {
            if (sLogger.isActivated()) {
                sLogger.info("--> can not schedule update flag operation");
            }
        }
    }
}

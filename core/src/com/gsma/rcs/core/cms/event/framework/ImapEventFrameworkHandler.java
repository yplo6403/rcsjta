
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

    /* package private */void pushMessages(ContactId contact) {
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

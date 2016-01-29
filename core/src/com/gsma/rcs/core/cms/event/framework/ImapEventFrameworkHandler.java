
package com.gsma.rcs.core.cms.event.framework;

import com.gsma.rcs.core.cms.sync.scheduler.Scheduler;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventFrameworkMode;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

/**
 * This class is in charge of updating flags on the message store using IMAP commands
 */
public class ImapEventFrameworkHandler {

    private static final Logger sLogger = Logger.getLogger(ImapEventFrameworkHandler.class
            .getSimpleName());

    private final Context mContext;
    private final RcsSettings mSettings;
    private final Scheduler mScheduler;

    /* package private */ImapEventFrameworkHandler(Context context, Scheduler scheduler,
            RcsSettings settings) {
        mContext = context;
        mScheduler = scheduler;
        mSettings = settings;
    }

    /* package private */void onNewXmsMessage(ContactId contact) {
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

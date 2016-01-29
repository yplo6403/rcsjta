
package com.gsma.rcs.core.cms.event.framework;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventFrameworkMode;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;

/**
 * This class is in charge of updating flags on the message store using SIP commands
 */
public class SipEventFrameworkHandler {

    private static final Logger sLogger = Logger.getLogger(SipEventFrameworkHandler.class
            .getSimpleName());

    private final Context mContext;
    private final RcsSettings mSettings;

    /* package private */SipEventFrameworkHandler(Context context, RcsSettings settings) {
        mContext = context;
        mSettings = settings;
    }

    /* package private */void updateFlags(EventFrameworkMode xmsMode, EventFrameworkMode chatMode) {
        sLogger.warn("TODO");
        sLogger.warn("--> SIP event framework not implemented");
    }
}

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

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2016 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.service;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Device boot event receiver: automatically starts the RCS service
 * 
 * @author jexa7410
 */
public class DeviceBoot extends BroadcastReceiver {
    private static Logger logger = Logger.getLogger(DeviceBoot.class.getSimpleName());

    @Override
    public void onReceive(Context context, Intent intent) {
        if (logger.isActivated())
            logger.debug("Start RCS service after boot");
        LocalContentResolver localContentResolver = new LocalContentResolver(context);
        RcsSettings rcsSettings = RcsSettings.getInstance(localContentResolver);
        LauncherUtils.launchRcsService(context, true, false, rcsSettings);
    }
}

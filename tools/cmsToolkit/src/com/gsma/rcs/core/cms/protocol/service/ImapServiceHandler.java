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

package com.gsma.rcs.core.cms.protocol.service;

import com.orangelabs.rcs.cms.toolkit.Preferences;
import com.sonymobile.rcs.imap.IoService;
import com.sonymobile.rcs.imap.SocketIoService;

import android.content.Context;

import java.util.logging.Logger;

/**
 * Class used to open and close an IMAP service. The synchronization process relies on this service
 * for executing command with the message store
 */
public class ImapServiceHandler {

    private static final Logger sLogger = Logger
            .getLogger(ImapServiceHandler.class.getSimpleName());

    private static final int SOCKET_TIMEOUT_IN_MS = 30000;

    private BasicImapService mBasicImapService;

    /**
     * Constructor
     * 
     * @param context application context
     */
    public ImapServiceHandler(Context context) {
        IoService io = new SocketIoService(Preferences.getMessageStoreUrl(context),
                SOCKET_TIMEOUT_IN_MS);
        mBasicImapService = new BasicImapService(io);
        // TODO FGI : Handle SSL authentication with message store
        // TODO FGI : See MESSAGE_STORE_AUTH parameter in provisioning
        mBasicImapService.setAuthenticationDetails(Preferences.getMessageStoreUser(context),
                Preferences.getMessageStorePwd(context), null, null, false);
    }

    public synchronized BasicImapService openService() {
        sLogger.fine("--> open IMAP Service");
        try {
            mBasicImapService.init();
            return mBasicImapService;
        } catch (Exception e) {
            e.printStackTrace();
            sLogger.severe(e.getMessage());
            return null;
        }
    }

    /**
     * Close the current service (connection) with the CMS server
     */
    public synchronized void closeService() {
        sLogger.fine("<-- close IMAP Service");
        if (mBasicImapService.isAvailable()) {
            try {
                mBasicImapService.logout();
                mBasicImapService.close();
            } catch (Exception e) {
                e.printStackTrace();
                sLogger.severe(e.getMessage());
            }
        }
    }
}

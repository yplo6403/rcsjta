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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.imaplib.imap.IoService;
import com.gsma.rcs.imaplib.imap.SocketIoService;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;
import java.net.URI;

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
     * @param rcsSettings the RCS settings accessor
     */
    public ImapServiceHandler(RcsSettings rcsSettings) {
        IoService io = new SocketIoService(URI.create(rcsSettings.getMessageStoreUri().toString()),
                SOCKET_TIMEOUT_IN_MS);
        mBasicImapService = new BasicImapService(io);
        // TODO FGI : Handle SSL authentication with message store
        // TODO FGI : See MESSAGE_STORE_AUTH parameter in provisioning
        mBasicImapService.setAuthenticationDetails(rcsSettings.getMessageStoreUser(),
                rcsSettings.getMessageStorePwd(), null, null, false);
    }

    public synchronized BasicImapService openService() throws NetworkException, PayloadException {
        if (sLogger.isActivated()) {
            sLogger.debug("--> open IMAP Service");
        }
        try {
            mBasicImapService.init();
            return mBasicImapService;
        } catch (IOException e) {
            throw new NetworkException("Failed to initialize CMS service!", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to initialize CMS service!", e);
        }
    }

    /**
     * Close the current service (connection) with the CMS server
     */
    public synchronized void closeService() throws NetworkException, PayloadException {
        if (sLogger.isActivated()) {
            sLogger.debug("<-- close IMAP Service");
        }
        if (mBasicImapService.isAvailable()) {
            try {
                mBasicImapService.logout();
                mBasicImapService.close();
            } catch (IOException e) {
                throw new NetworkException("Failed to close connection with CMS server!", e);

            } catch (ImapException e) {
                throw new PayloadException("Failed to close connection with CMS server!", e);
            }
        }
    }
}

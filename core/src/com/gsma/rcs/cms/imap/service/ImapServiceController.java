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

package com.gsma.rcs.cms.imap.service;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.IoService;
import com.sonymobile.rcs.imap.SocketIoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to restrict the connection with the CMS server. It allows only one connection with the
 * CMS server at a given time. Started and stopped from the Core.
 */
public class ImapServiceController {

    private static final Logger sLogger = Logger.getLogger(ImapServiceController.class
            .getSimpleName());

    private final List<ImapServiceListener> mListeners = new ArrayList<>();
    private final RcsSettings mRcsSettings;
    private boolean mStarted;
    private boolean mAvailable;
    private BasicImapService mBasicImapService;

    /**
     * Constructor
     * 
     * @param rcsSettings the RCS settings accessor
     */
    public ImapServiceController(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
        mStarted = false;
        mAvailable = true;
    }

    public synchronized void start() {
        if (mStarted) {
            return;
        }
        mStarted = true;
    }

    public synchronized void stop() throws NetworkException, PayloadException {
        if (!mStarted) {
            return;
        }
        if (mBasicImapService != null) {
            if (sLogger.isActivated()) {
                sLogger.debug("Force to close current IMAP service");
            }
            closeService();
        }
        mStarted = false;
    }

    protected boolean isStarted() {
        return mStarted;
    }

    /**
     * Create an imap service (connection) with the CMS server Parameters for the service are
     * retrieved from the settings
     * 
     * @return IMAP service
     * @throws ImapServiceNotAvailableException
     */
    public synchronized BasicImapService createService() throws ImapServiceNotAvailableException {

        if (!mAvailable) {
            if (sLogger.isActivated()) {
                sLogger.debug("IMAP sync with CMS server not available");
                sLogger.debug("A previous one is already in progress ... ");
            }
            throw new ImapServiceNotAvailableException(
                    "IMAP sync already in progress. Can not start a new one yet.");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Start of a new IMAP synchronization.");
        }
        IoService io = new SocketIoService(mRcsSettings.getCmsServerAddress());
        mBasicImapService = new BasicImapService(io);
        mBasicImapService.setAuthenticationDetails(mRcsSettings.getCmsUserLogin(),
                mRcsSettings.getCmsUserPwd(), null, null, false);
        mAvailable = false;
        return mBasicImapService;
    }

    /**
     * Retrieve the current imap service
     * 
     * @return IMAP service
     * @throws ImapServiceNotAvailableException
     */
    public BasicImapService getService() throws ImapServiceNotAvailableException {
        if (mBasicImapService == null) {
            throw new ImapServiceNotAvailableException("BasicImapService not open");
        }
        return mBasicImapService;
    }

    public synchronized void initService() throws ImapServiceNotAvailableException,
            NetworkException, PayloadException {
        if (mBasicImapService == null) {
            throw new ImapServiceNotAvailableException("BasicImapService not open");
        }
        try {
            mBasicImapService.init();

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
            sLogger.debug("End of IMAP synchronization.");
        }
        if (mBasicImapService == null) {
            return;
        }

        try {
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
        } finally {
            mAvailable = true;
            for (ImapServiceListener listener : mListeners) {
                listener.onImapServiceAvailable();
            }
            mListeners.clear();
        }
    }

    /**
     * Check if there is no sync in progress
     * 
     * @return True if there is no sync in progress
     */
    public boolean isSyncAvailable() {
        return mAvailable;
    }

    /**
     * Register a listener to be notified of the end of the sync
     * 
     * @param listener the IMAP listener
     */
    public void registerListener(ImapServiceListener listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    /**
     * Unregister a listener
     * 
     * @param listener the IMAP listener
     */
    public void unregisterListener(ImapServiceListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    /**
     * Interface used to notify end of sync
     */
    public interface ImapServiceListener {
        void onImapServiceAvailable();
    }
}

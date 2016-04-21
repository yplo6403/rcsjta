/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core;

import com.gsma.rcs.addressbook.AddressBookManager;
import com.gsma.rcs.addressbook.LocaleManager;
import com.gsma.rcs.core.cms.event.XmsEventHandler;
import com.gsma.rcs.core.cms.service.CmsService;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.cms.xms.observer.XmsObserver;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.platform.ntp.NtpManager;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.history.HistoryLog;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.utils.DeviceUtils;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;
import java.security.KeyStoreException;

/**
 * Core (singleton pattern)
 * 
 * @author JM. Auffret
 */
public class Core {

    private static final String BACKGROUND_THREAD_NAME = Core.class.getSimpleName();

    private static volatile Core sInstance;

    private final XmsManager mXmsManager;

    private final XmsObserver mXmsObserver;

    private CoreListener mListener;

    private boolean mStarted = false;

    private ImsModule mImsModule;

    private AddressBookManager mAddressBookManager;

    private NtpManager mNtpManager;

    private static final Logger sLogger = Logger.getLogger(BACKGROUND_THREAD_NAME);

    /**
     * Handler to process messages & runnable associated with background thread.
     */
    private Handler mBackgroundHandler;

    /**
     * Boolean to check is the Core is stopping
     */
    private boolean mStopping = false;

    private final LocaleManager mLocaleManager;

    /**
     * Returns the singleton instance
     * 
     * @return Core instance
     */
    public static Core getInstance() {
        return sInstance;
    }

    /**
     * Instantiate the core
     * 
     * @param ctx The application context
     * @param listener Listener
     * @param rcsSettings RcsSettings instance
     * @param contentResolver The content resolver
     * @param localContentResolver The local content resolver
     * @param contactManager The contact manager
     * @param messagingLog The messaging log accessor
     * @param historyLog The history log accessor
     * @param richCallHistory The richcall log accessor
     * @param xmsLog The XMS log accessor
     * @return Core instance
     * @throws IOException
     * @throws KeyStoreException
     */
    public static Core createCore(Context ctx, CoreListener listener, RcsSettings rcsSettings,
            ContentResolver contentResolver, LocalContentResolver localContentResolver,
            ContactManager contactManager, MessagingLog messagingLog, HistoryLog historyLog,
            RichCallHistory richCallHistory, XmsLog xmsLog, CmsLog cmsLog) throws IOException,
            KeyStoreException {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (Core.class) {
            if (sInstance == null) {
                KeyStoreManager.loadKeyStore(rcsSettings);
                sInstance = new Core(ctx, listener, contentResolver, localContentResolver,
                        rcsSettings, contactManager, messagingLog, historyLog, richCallHistory,
                        xmsLog, cmsLog);
            }
        }
        return sInstance;
    }

    /**
     * Terminate the core
     * 
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    public synchronized static void terminateCore() throws PayloadException, NetworkException,
            ContactManagerException {
        if (sInstance == null) {
            return;
        }
        sInstance.stopCore();
        sInstance = null;
    }

    /**
     * Constructor
     * 
     * @param ctx The application context
     * @param listener Listener
     * @param contentResolver The content resolver
     * @param localContentResolver The local content resolver
     * @param rcsSettings The RCS settings accessor
     * @param contactManager The contact manager
     * @param messagingLog The messaging log accessor
     * @param historyLog The history log accessor
     * @param richCallHistory The richcall log accessor
     * @param xmsLog The XMS log accessor
     */
    private Core(Context ctx, CoreListener listener, ContentResolver contentResolver,
            LocalContentResolver localContentResolver, RcsSettings rcsSettings,
            ContactManager contactManager, MessagingLog messagingLog, HistoryLog historyLog,
            RichCallHistory richCallHistory, XmsLog xmsLog, CmsLog cmsLog) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Terminal core initialization");
        }
        mListener = listener;
        if (logActivated) {
            sLogger.info("My device UUID is ".concat(String.valueOf(DeviceUtils.getDeviceUUID(ctx))));
        }

        // Initialize the phone utils
        PhoneUtils.initialize(rcsSettings);

        // Create the address book manager
        mAddressBookManager = new AddressBookManager(contentResolver, contactManager);
        mLocaleManager = new LocaleManager(ctx, this, rcsSettings, contactManager);
        mXmsManager = new XmsManager(ctx, contentResolver);

        // instantiate Xms Observer on native SMS/MMS content provider
        mXmsObserver = new XmsObserver(ctx);

        /* Create the IMS module */
        mImsModule = new ImsModule(this, ctx, localContentResolver, rcsSettings, contactManager,
                messagingLog, historyLog, richCallHistory, mAddressBookManager, xmsLog, cmsLog);

        /* Create the NTP manager */
        mNtpManager = new NtpManager(ctx, rcsSettings);

        if (logActivated) {
            sLogger.info("Terminal core is created with success");
        }
    }

    /**
     * Initializes Core
     * 
     * @param xmsEventHandler the XMS event handler
     */
    public void initialize(XmsEventHandler xmsEventHandler) {
        final HandlerThread backgroundThread = new HandlerThread(BACKGROUND_THREAD_NAME);
        backgroundThread.start();
        mBackgroundHandler = new Handler(backgroundThread.getLooper());

        mImsModule.initialize(xmsEventHandler);
        mXmsObserver.registerListener(xmsEventHandler);
    }

    /**
     * Returns the event listener
     * 
     * @return Listener
     */
    public CoreListener getListener() {
        return mListener;
    }

    /**
     * Returns the IMS module
     * 
     * @return IMS module
     */
    public ImsModule getImsModule() {
        return mImsModule;
    }

    /**
     * Schedule a background task on Handler for execution
     */
    public void scheduleCoreOperation(Runnable task) {
        mBackgroundHandler.post(task);
    }

    /**
     * Is core started
     * 
     * @return Boolean
     */
    public boolean isStarted() {
        return mStarted;
    }

    /**
     * Start the terminal core
     */
    public synchronized void startCore() {
        if (mStarted) {
            return;
        }
        mImsModule.start();
        mAddressBookManager.start();
        mXmsManager.start();
        mLocaleManager.start();
        mNtpManager.start();
        mXmsObserver.start();
        mListener.onCoreLayerStarted();

        mStarted = true;
        if (sLogger.isActivated()) {
            sLogger.info("RCS core service has been started with success");
        }
    }

    /**
     * Stop the terminal core
     * 
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    private void stopCore() throws PayloadException, NetworkException, ContactManagerException {
        if (!mStarted) {
            return;
        }
        mStopping = true;
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Stop the RCS core service");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBackgroundHandler.getLooper().quitSafely();
        } else {
            mBackgroundHandler.getLooper().quit();
        }

        mLocaleManager.stop();
        mAddressBookManager.stop();
        mXmsManager.stop();
        mImsModule.stop();
        mNtpManager.stop();
        mXmsObserver.stop();

        mStopping = false;
        mStarted = false;
        if (logActivated) {
            sLogger.info("RCS core service has been stopped with success");
        }
        mListener.onCoreLayerStopped();
    }

    /**
     * Returns the capability service
     * 
     * @return Capability service
     */
    public CapabilityService getCapabilityService() {
        return getImsModule().getCapabilityService();
    }

    /**
     * Returns the richcall service
     * 
     * @return Rich call service
     */
    public RichcallService getRichcallService() {
        return getImsModule().getRichcallService();
    }

    /**
     * Returns the CMS service
     *
     * @return CMS service
     */
    public CmsService getCmsService() {
        return getImsModule().getCmsService();
    }

    /**
     * Returns the IM service
     * 
     * @return IM service
     */
    public InstantMessagingService getImService() {
        return getImsModule().getInstantMessagingService();
    }

    /**
     * Returns the SIP service
     * 
     * @return SIP service
     */
    public SipService getSipService() {
        return getImsModule().getSipService();
    }

    /**
     * Returns True if Core is stopping
     * 
     * @return True if Core is stopping
     */
    public boolean isStopping() {
        return mStopping;
    }

    /**
     * Sets the listener
     * 
     * @param listener The Core listener
     */
    public void setListener(CoreListener listener) {
        mListener = listener;
    }

    /**
     * Gets XMS manager
     *
     * @return XmsManager
     */
    public XmsManager getXmsManager() {
        return mXmsManager;
    }
}

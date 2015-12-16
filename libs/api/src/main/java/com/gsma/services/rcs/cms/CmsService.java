/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.services.rcs.cms;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This class offers the main entry point to Common Message Store service.<br>
 * Several applications may connect/disconnect to the API.
 *
 * @author Philippe LEMORDANT
 */
public final class CmsService extends RcsService {

    private static boolean sApiCompatible = false;

    private final Map<CmsSynchronizationListener, WeakReference<ICmsSynchronizationListener>> mCmsSyncListeners = new WeakHashMap<>();

    private final Map<XmsMessageListener, WeakReference<IXmsMessageListener>> mXmsMessageListeners = new WeakHashMap<>();

    /**
     * API
     */
    private ICmsService mApi;
    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(ICmsService.Stub.asInterface(service));
            if (mListener != null) {
                mListener.onServiceConnected();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            setApi(null);
            if (mListener == null) {
                return;
            }
            ReasonCode reasonCode = ReasonCode.CONNECTION_LOST;
            try {
                if (!mRcsServiceControl.isActivated()) {
                    reasonCode = ReasonCode.SERVICE_DISABLED;
                }
            } catch (RcsServiceException e) {
                // Do nothing
            }
            mListener.onServiceDisconnected(reasonCode);
        }
    };

    /**
     * Constructor
     *
     * @param ctx      Application context
     * @param listener Service listener
     */
    public CmsService(Context ctx, RcsServiceListener listener) {
        super(ctx, listener);
    }

    /**
     * Connects to the API
     *
     * @throws RcsPermissionDeniedException
     */
    public final void connect() throws RcsPermissionDeniedException {
        if (!sApiCompatible) {
            try {
                sApiCompatible = mRcsServiceControl.isCompatible(this);
                if (!sApiCompatible) {
                    throw new RcsPermissionDeniedException(
                            "The TAPI client version of the CMS service is not compatible with the TAPI service implementation version on this device!");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException(
                        "The compatibility of TAPI client version with the TAPI service implementation version of this device cannot be checked for the CMS service!",
                        e);
            }
        }
        Intent serviceIntent = new Intent(ICmsService.class.getName());
        serviceIntent.setPackage(RcsServiceControl.RCS_STACK_PACKAGENAME);
        mCtx.bindService(serviceIntent, apiConnection, 0);
    }

    /**
     * Disconnects from the API
     */
    public void disconnect() {
        try {
            mCtx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Set API interface
     *
     * @param api API interface
     */
    protected void setApi(IInterface api) {
        super.setApi(api);
        mApi = (ICmsService) api;
    }

    /**
     * Adds a listener on CMS synchronization events
     *
     * @param listener The CMS synchronization listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(CmsSynchronizationListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            ICmsSynchronizationListener rcsListener = new CmsSynchronizationListenerImpl(listener);
            mCmsSyncListeners.put(listener, new WeakReference<>(rcsListener));
            mApi.addEventListener(rcsListener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Removes a listener on CMS synchronization events
     *
     * @param listener The CMS synchronization listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(CmsSynchronizationListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<ICmsSynchronizationListener> weakRef = mCmsSyncListeners
                    .remove(listener);
            if (weakRef == null) {
                return;
            }
            ICmsSynchronizationListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener(rcsListener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Synchronizes all local CMS repository with network CMS repository.
     *
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void syncAll() throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.syncAll();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Synchronizes local CMS messages with network CMS repository for a given contact.
     *
	 * @param contact The remote contact
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void syncOneToOneConversation(ContactId contact) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.syncOneToOneConversation(contact);
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Synchronizes local CMS messages with network CMS repository for a given chat ID.
     *
     * @param chatId The chat ID
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void syncGroupConversation(String chatId)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.syncGroupConversation(chatId);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets current XMS message from its unique ID
     *
     * @param messageId The message ID
     * @return XMS message or null if not found
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public XmsMessage getXmsMessage(String messageId) throws RcsGenericException, RcsServiceNotAvailableException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new XmsMessage(mApi.getXmsMessage(messageId));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

	/**
     * Sends a text based SMS message
     *
     * @param contact The remote contact
     * @param message The text message
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void sendTextMessage(ContactId contact, String message) throws RcsGenericException, RcsServiceNotAvailableException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.sendTextMessage(contact, message);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it's possible and allowed to send MMS messages right now, else returns false.
     *
     * @return boolean
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public boolean isAllowedToSendMultimediaMessage() throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.isAllowedToSendMultimediaMessage();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

	/**
     * Sends a MMS message
     *
     * @param contact The remote contact
	 * @param files The list of file URIs (image or video)
     * @param subject The subject
	 * @param body The body text message
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public XmsMessage sendMultimediaMessage(ContactId contact, List<Uri> files, String subject, String body) throws RcsGenericException, RcsServiceNotAvailableException, RcsPersistentStorageException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            for (Uri file : files) {
                tryToGrantUriPermissionToStackServices(file);
            }
            IXmsMessage xms = mApi.sendMultimediaMessage(contact, files, subject, body);
            return new XmsMessage(xms);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Marks a received message as read (ie. displayed in the UI)
     *
     * @param msgId The message ID.
     * @throws RcsServiceNotAvailableException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void markMessageAsRead(String msgId) throws RcsServiceNotAvailableException,
            RcsPersistentStorageException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.markXmsMessageAsRead(msgId);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

	/**
     * Adds a XMS message event listener.
     *
     * @param listener The XMS message event listener.
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(XmsMessageListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IXmsMessageListener rcsListener = new XmsMessageListenerImpl(listener);
            mXmsMessageListeners.put(listener, new WeakReference<>(rcsListener));
            mApi.addEventListener2(rcsListener);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

	/**
     * Removes a XMS message event listener.
     *
     * @param listener The XMS message event listener.
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(XmsMessageListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IXmsMessageListener> weakRef = mXmsMessageListeners
                    .remove(listener);
            if (weakRef == null) {
                return;
            }
            IXmsMessageListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener2(rcsListener);
            }

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

	/**
     * Deletes all XMS messages from history.
     *
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteXmsMessages() throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteXmsMessages();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

	/**
     * Deletes all XMS messages for a given contact from history.
     *
     * @param contact The remote contact.
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteXmsMessages(ContactId contact) throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteXmsMessages2(contact);
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

	/**
     * Deletes a XMS message specified by its unique ID from history.
     *
     * @param msgId The message ID.
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteXmsMessage(String msgId) throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteXmsMessage(msgId);
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Granting temporary read Uri permission from client to stack service if it is a content URI
     *
     * @param file Uri of file to grant permission
     */
    private void tryToGrantUriPermissionToStackServices(Uri file) {
        if (!ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
            return;
        }
        Intent cmsServiceIntent = new Intent(ICmsService.class.getName());
        List<ResolveInfo> stackServices = mCtx.getPackageManager().queryIntentServices(
                cmsServiceIntent, 0);
        for (ResolveInfo stackService : stackServices) {
            mCtx.grantUriPermission(stackService.serviceInfo.packageName, file,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }
}

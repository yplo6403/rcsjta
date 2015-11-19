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
 ******************************************************************************/

package com.gsma.rcs.service.api;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import com.gsma.rcs.core.ims.service.cms.CmsService;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.XmsPersistedStorageAccessor;
import com.gsma.rcs.service.broadcaster.CmsEventBroadcaster;
import com.gsma.rcs.service.broadcaster.XmsMessageEventBroadcaster;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.ICmsService;
import com.gsma.services.rcs.cms.ICmsSynchronizationListener;
import com.gsma.services.rcs.cms.IXmsMessage;
import com.gsma.services.rcs.cms.IXmsMessageListener;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common Message Store service implementation
 *
 * @author Philippe LEMORDANT
 */
public class CmsServiceImpl extends ICmsService.Stub {

    private static final Logger sLogger = Logger.getLogger(CmsServiceImpl.class.getSimpleName());
    private final CmsEventBroadcaster mCmsBroadcaster = new CmsEventBroadcaster();

    private final XmsMessageEventBroadcaster mXmsMessageBroadcaster = new XmsMessageEventBroadcaster();

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();
    private final CmsService mCmsService;

    private final Map<String, IXmsMessage> mXmsMessageCache = new HashMap<>();
    private final XmsLog mXmsLog;
    private final ContentResolver mContentResolver;
    private final Context mContext;

    /**
     * Constructor
     */
    public CmsServiceImpl(Context context, CmsService cmsService, XmsLog xmsLog,
                          ContentResolver contentResolver) {
        if (sLogger.isActivated()) {
            sLogger.info("CMS service API is loaded");
        }
        mContext = context;
        mCmsService = cmsService;
        mCmsService.register(this);
        mXmsLog = xmsLog;
        mContentResolver = contentResolver;
    }

    /**
     * Close API
     */
    public void close() {
        mXmsMessageCache.clear();
        if (sLogger.isActivated()) {
            sLogger.info("CMS service API is closed");
        }
    }

    @Override
    public void addEventListener(ICmsSynchronizationListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a CMS sync event listener");
        }
        try {
            synchronized (lock) {
                mCmsBroadcaster.addEventListener(listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void removeEventListener(ICmsSynchronizationListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a CMS sync event listener");
        }
        try {
            synchronized (lock) {
                mCmsBroadcaster.removeEventListener(listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public int getServiceVersion() {
        return RcsService.Build.API_VERSION;
    }

    @Override
    public void syncOneToOneConversation(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Sync One-to-One conversation for contact " + contact);
        }
        mCmsService.syncOneToOneConversation(contact);
    }

    @Override
    public void syncGroupConversation(String chatId) throws RemoteException {
        if (chatId == null) {
            throw new ServerApiIllegalArgumentException("chat ID must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Sync group conversation for chat ID " + chatId);
        }
        mCmsService.syncGroupConversation(chatId);
    }

    @Override
    public void syncAll() throws RemoteException {
        mCmsService.syncAll();
    }

    @Override
    public IXmsMessage getXmsMessage(String messageId) throws RemoteException {
        if (TextUtils.isEmpty(messageId)) {
            throw new ServerApiIllegalArgumentException("messageId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get XMS message ".concat(messageId));
        }
        try {
            IXmsMessage xmsMessage = mXmsMessageCache.get(messageId);
            if (xmsMessage != null) {
                return xmsMessage;
            }
            XmsPersistedStorageAccessor accessor = new XmsPersistedStorageAccessor(mXmsLog,
                    messageId);
            IXmsMessage result = new XmsMessageImpl(messageId, accessor);
            mXmsMessageCache.put(messageId, result);
            return result;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void sendTextMessage(final ContactId contact, final String text)
            throws RemoteException {
        if (TextUtils.isEmpty(text)) {
            throw new ServerApiIllegalArgumentException("message must not be null or empty!");
        }
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        mCmsService.scheduleOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mCmsService.sendSms(contact, text);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to send SMS!", e);
                }
            }
        });
    }

    private ArrayList<Uri> getValidatedUris(List<Uri> files) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (Uri file : files) {
            if (!FileUtils.isReadFromUriPossible(mContext, file)) {
                throw new ServerApiIllegalArgumentException("file '" + file.toString()
                        + "' must refer to a file that exists and that is readable by stack!");
            }
            String mimeType = mContentResolver.getType(file);
            if (!MimeManager.isImageType(mimeType)) {
                if (!MimeManager.isVideoType(mimeType)) {
                    throw new ServerApiIllegalArgumentException("file '" + file.toString()
                            + "' has invalid mime-type: '" + mimeType + "'!");
                }
            }
            uris.add(file);
        }
        return uris;
    }

    @Override
    public void sendMultimediaMessage(final ContactId contact, List<Uri> files,
                                      final String text) throws RemoteException {
        if (sLogger.isActivated()) {
            sLogger.debug("sendMultimediaMessage contact=" + contact + " text=" + text);
        }
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (files == null || files.isEmpty()) {
            throw new ServerApiIllegalArgumentException("files must not be null or empty!");
        }
        final ArrayList<Uri> uris = getValidatedUris(files);
        mCmsService.scheduleOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mCmsService.sendMms(contact, text, uris);

                } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                    sLogger.error("Failed to send MMS!", e);
                }
            }
        });
    }

    @Override
    public void markXmsMessageAsRead(final String messageId) throws RemoteException {
        if (TextUtils.isEmpty(messageId)) {
            throw new ServerApiIllegalArgumentException("msgId must not be null or empty!");
        }
        mCmsService.scheduleOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mXmsLog.markMessageAsRead(messageId);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to mark message as read!", e);
                }
            }
        });
    }

    @Override
    public void addEventListener2(IXmsMessageListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a XMS message event listener");
        }
        try {
            synchronized (lock) {
                mXmsMessageBroadcaster.addEventListener(listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void removeEventListener2(IXmsMessageListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a XMS message event listener");
        }
        try {
            synchronized (lock) {
                mXmsMessageBroadcaster.removeEventListener(listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void deleteXmsMessages() throws RemoteException {

    }

    @Override
    public void deleteXmsMessages2(ContactId contact) throws RemoteException {

    }

    @Override
    public void deleteXmsMessage(String messageId) throws RemoteException {

    }

    /**
     * Broadcasts all synchronized
     */
    public void broadcastAllSynchronized() {
        mCmsBroadcaster.broadcastAllSynchronized();
    }

    /**
     * Broadcasts One-to-One conversation synchronized
     */
    public void broadcastOneToOneConversationSynchronized(ContactId contact) {
        mCmsBroadcaster.broadcastOneToOneConversationSynchronized(contact);
    }

    /**
     * Broadcasts Group conversation synchronized
     */
    public void broadcastGroupConversationSynchronized(String chatId) {
        mCmsBroadcaster.broadcastGroupConversationSynchronized(chatId);
    }

    public void broadcastMessageStateChanged(ContactId contact, String mimeType, String msgId,
                                             XmsMessage.State state, XmsMessage.ReasonCode reasonCode) {
        mXmsMessageBroadcaster.broadcastMessageStateChanged(contact, mimeType, msgId, state,
                reasonCode);
    }

    public void broadcastNewMessage(String mimeType, String msgId) {
        mXmsMessageBroadcaster.broadcastNewMessage(mimeType, msgId);
    }

    public void broadcastMessageDeleted(ContactId contact, Set<String> messageIds) {
        mXmsMessageBroadcaster.broadcastMessageDeleted(contact, messageIds);
    }

}

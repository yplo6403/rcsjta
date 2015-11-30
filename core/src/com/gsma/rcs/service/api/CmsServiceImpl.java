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

import com.gsma.rcs.cms.CmsManager;
import com.gsma.rcs.core.ims.service.cms.CmsService;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.XmsPersistedStorageAccessor;
import com.gsma.rcs.service.broadcaster.CmsEventBroadcaster;
import com.gsma.rcs.service.broadcaster.XmsMessageEventBroadcaster;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.rcs.xms.XmsManager;
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
    private final XmsManager mXmsManager;
    private final CmsManager mCmsManager;

    /**
     * Constructor
     */
    public CmsServiceImpl(Context context, CmsService cmsService, XmsLog xmsLog,
                          ContentResolver contentResolver, XmsManager xmsManager, CmsManager cmsManager) {
        if (sLogger.isActivated()) {
            sLogger.info("CMS service API is loaded");
        }
        mContext = context;
        mCmsService = cmsService;
        mCmsService.register(this);
        mXmsLog = xmsLog;
        mContentResolver = contentResolver;
        mXmsManager = xmsManager;
        mCmsManager = cmsManager;
        mCmsManager.registerXmsMessageEventBroadcaster(mXmsMessageBroadcaster);
    }

    /**
     * Close API
     */
    public void close() {
        mXmsMessageCache.clear();
        if (sLogger.isActivated()) {
            sLogger.info("CMS service API is closed");
        }
        mCmsManager.unregisterXmsMessageEventBroadcaster(mXmsMessageBroadcaster);
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
    public void syncOneToOneConversation(final ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (!mCmsService.isServiceStarted()) {
            throw new ServerApiServiceNotAvailableException("CMS service is not available!");
        }
        mCmsService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.info("Sync One-to-One conversation for contact " + contact);
                }
                try {
                    mCmsService.syncOneToOneConversation(contact);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to synchronize One-to-One conversation for contact " + contact, e);
                }
            }
        });
    }

    @Override
    public void syncGroupConversation(final String chatId) throws RemoteException {
        if (chatId == null) {
            throw new ServerApiIllegalArgumentException("chat ID must not be null!");
        }
        if (!mCmsService.isServiceStarted()) {
            throw new ServerApiServiceNotAvailableException("CMS service is not available!");
        }
        mCmsService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.info("Sync group conversation for chat ID " + chatId);
                }
                try {
                    mCmsService.syncGroupConversation(chatId);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to synchronize group conversation for chat ID " + chatId, e);
                }
            }
        });
    }

    @Override
    public void syncAll() throws RemoteException {
        if (!mCmsService.isServiceStarted()) {
            throw new ServerApiServiceNotAvailableException("CMS service is not available!");
        }
        mCmsService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Synchronize CMS");
                }
                try {
                    mCmsService.syncAll();

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to synchronize CMS!", e);
                }
            }
        });
    }

    @Override
    public IXmsMessage getXmsMessage(String messageId) throws RemoteException {
        if (TextUtils.isEmpty(messageId)) {
            throw new ServerApiIllegalArgumentException("message ID must not be null or empty!");
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
    public void sendTextMessage(final ContactId contact, final String text) throws RemoteException {
        if (TextUtils.isEmpty(text)) {
            throw new ServerApiIllegalArgumentException("Text must not be null or empty!");
        }
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        try {
            mXmsManager.sendSms(contact, text);

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

    @Override
    public void sendMultimediaMessage(final ContactId contact, List<Uri> files, final String text)
            throws RemoteException {
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
        try {
            mXmsManager.sendMms(contact, text, uris);

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
    public void markXmsMessageAsRead(final String messageId) throws RemoteException {
        if (TextUtils.isEmpty(messageId)) {
            throw new ServerApiIllegalArgumentException("message ID must not be null or empty!");
        }
        mCmsService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    RcsService.ReadStatus readStatus = mXmsLog.getReadStatus(messageId);
                    if (readStatus != null) {
                        if (RcsService.ReadStatus.UNREAD == readStatus) {
                            mCmsManager.onReadRcsMessage(messageId);
                            if (!mCmsService.isServiceStarted()) {
                                // TODO synchronize CMS
                            }
                        }
                    } else {
                        sLogger.warn("Cannot mark as read: message ID'" + messageId + "' not found!");
                    }

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
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
        mCmsService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mCmsManager.onDeleteAll();
                    if (mCmsService.isServiceStarted()) {
                        // TODO synchronize CMS
                    }
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error("Failed to delete messages", e);
                }
            }
        });
    }

    @Override
    public void deleteXmsMessages2(final ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("Contact must not be null!");
        }
        mCmsService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mCmsManager.onDeleteRcsConversation(contact);
                    if (mCmsService.isServiceStarted()) {
                        // TODO synchronize CMS
                    }
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error("Failed to delete messages for contact " + contact, e);
                }
            }
        });
    }

    @Override
    public void deleteXmsMessage(final String messageId) throws RemoteException {
        if (TextUtils.isEmpty(messageId)) {
            throw new ServerApiIllegalArgumentException("message ID must not be null or empty!");
        }
        mCmsService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mCmsManager.onDeleteRcsMessage(messageId);
                    if (mCmsService.isServiceStarted()) {
                        // TODO synchronize CMS
                    }
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error("Failed to delete message ID " + messageId, e);
                }
            }
        });
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

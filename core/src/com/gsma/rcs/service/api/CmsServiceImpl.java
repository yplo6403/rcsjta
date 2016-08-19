/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler.SyncType;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncSchedulerListener;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncSchedulerTaskType;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.cms.xms.mms.MmsFileSizeException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.smsmms.SmsMmsLog;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsDeleteTask;
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
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import com.klinker.android.send_message.Utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common Message Store service implementation
 *
 * @author Philippe LEMORDANT
 */
public class CmsServiceImpl extends ICmsService.Stub implements CmsSyncSchedulerListener {

    private static final Logger sLogger = Logger.getLogger(CmsServiceImpl.class.getName());

    private final CmsEventBroadcaster mCmsBroadcaster = new CmsEventBroadcaster();
    private final XmsMessageEventBroadcaster mXmsMessageBroadcaster = new XmsMessageEventBroadcaster();
    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();
    private final CmsSessionController mCmsSessionCtrl;

    private final Map<String, XmsMessageImpl> mXmsMessageCache = new HashMap<>();
    private final XmsLog mXmsLog;
    private final Context mCtx;
    private final XmsManager mXmsManager;
    private final LocalContentResolver mLocalContentResolver;
    private final SmsMmsLog mSmsMmsLog;

    /**
     * Constructor
     */
    public CmsServiceImpl(Context ctx, CmsSessionController cmsSessionCtrl,
            ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, XmsLog xmsLog, XmsManager xmsManager,
            LocalContentResolver localContentResolver, SmsMmsLog smsMmsLog) {
        if (sLogger.isActivated()) {
            sLogger.info("CMS service API is loaded");
        }
        mCtx = ctx;
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsSessionCtrl.register(this, chatService, fileTransferService, imService);
        mXmsLog = xmsLog;
        mXmsManager = xmsManager;
        mLocalContentResolver = localContentResolver;
        mSmsMmsLog = smsMmsLog;
    }

    @Override
    public void addEventListener(ICmsSynchronizationListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
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
    public int getServiceVersion() {
        return RcsService.Build.API_VERSION;
    }

    @Override
    public void syncOneToOneConversation(final ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (!mCmsSessionCtrl.isServiceStarted()) {
            throw new ServerApiServiceNotAvailableException("CMS service is not available!");
        }
        mCmsSessionCtrl.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.info("Sync One-to-One conversation for contact ".concat(contact
                            .toString()));
                }
                try {
                    mCmsSessionCtrl.syncOneToOneConversation(contact);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to synchronize One-to-One conversation for contact "
                            .concat(contact.toString()), e);
                }
            }
        });
    }

    @Override
    public void syncGroupConversation(final String chatId) throws RemoteException {
        if (chatId == null) {
            throw new ServerApiIllegalArgumentException("chat ID must not be null!");
        }
        if (!mCmsSessionCtrl.isServiceStarted()) {
            throw new ServerApiServiceNotAvailableException("CMS service is not available!");
        }
        mCmsSessionCtrl.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.info("Sync group conversation for chat ID " + chatId);
                }
                try {
                    mCmsSessionCtrl.syncGroupConversation(chatId);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to synchronize group conversation for chat ID " + chatId,
                            e);
                }
            }
        });
    }

    @Override
    public void syncAll() throws RemoteException {
        if (!mCmsSessionCtrl.isServiceStarted()) {
            throw new ServerApiServiceNotAvailableException("CMS service is not available!");
        }
        mCmsSessionCtrl.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.debug("Synchronize CMS");
                }
                try {
                    mCmsSessionCtrl.syncAll();

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
    public IXmsMessage getXmsMessage(ContactId remote, String messageId) throws RemoteException {
        if (TextUtils.isEmpty(messageId)) {
            throw new ServerApiIllegalArgumentException("message ID must not be null or empty!");
        }
        try {
            return getOrCreateXmsMessage(remote, messageId);

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

    public XmsMessageImpl getOrCreateXmsMessage(ContactId remote, String messageId) {
        String key = remote.toString() + messageId;
        XmsMessageImpl xmsMessage = mXmsMessageCache.get(key);
        if (xmsMessage != null) {
            return xmsMessage;
        }
        XmsPersistedStorageAccessor accessor = new XmsPersistedStorageAccessor(mXmsLog, remote,
                messageId);
        XmsMessageImpl result = new XmsMessageImpl(messageId, accessor);
        mXmsMessageCache.put(key, result);
        return result;
    }

    @Override
    public void sendTextMessage(final ContactId contact, final String text) throws RemoteException {
        if (TextUtils.isEmpty(text)) {
            throw new ServerApiIllegalArgumentException("Text must not be null or empty!");
        }
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (!isAllowedToSendXms()) {
            throw new ServerApiPermissionDeniedException(
                    "\'com.gsma.rcs\' is not the default messaging app!");
        }
        mCmsSessionCtrl.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mXmsManager.sendTextMessage(contact, text);

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

    @Override
    public boolean isAllowedToSendXms() throws RemoteException {
        return Utils.isDefaultSmsApp(mCtx);
    }

    @Override
    public void sendMultimediaMessage(final ContactId contact, final List<Uri> files,
            final String subject, final String body) throws RemoteException {
        if (sLogger.isActivated()) {
            sLogger.debug("send MMS to " + contact + " subject='" + subject + "' body='" + body
                    + "'");
        }
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (files == null || files.isEmpty()) {
            throw new ServerApiIllegalArgumentException("files must not be null or empty!");
        }
        if (!isAllowedToSendXms()) {
            throw new ServerApiPermissionDeniedException(
                    "\'com.gsma.rcs\' is not the default messaging app!");
        }
        checkUris(files);
        mCmsSessionCtrl.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mXmsManager.sendMultimediaMessage(contact, files, subject, body);

                } catch (RuntimeException | MmsFileSizeException | FileAccessException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to send MMS to contact ".concat(contact.toString()), e);
                }
            }
        });
    }

    private void checkUris(List<Uri> files) {
        for (Uri file : files) {
            if (!FileUtils.isReadFromUriPossible(mCtx, file)) {
                throw new ServerApiIllegalArgumentException("file '" + file.toString()
                        + "' must refer to a file that exists and that is readable by stack!");
            }
            String fileName = FileUtils.getFileName(mCtx, file);
            if (fileName == null) {
                throw new ServerApiIllegalArgumentException("Invalid Uri '" + file + "'!");
            }
            String extension = MimeManager.getFileExtension(fileName);
            String mimeType = MimeManager.getInstance().getMimeType(extension);
            if (mimeType == null) {
                throw new ServerApiIllegalArgumentException("Invalid mime type for Uri '" + file
                        + "'!");
            }
            if (!MimeManager.isImageType(mimeType)) {
                if (!MimeManager.isVideoType(mimeType)) {
                    if (!MimeManager.isVCardType(mimeType)) {
                        throw new ServerApiIllegalArgumentException("file '" + file.toString()
                                + "' has invalid mime-type: '" + mimeType + "'!");
                    }
                }
            }
        }
    }

    public void markXmsMessageAsRead_(final ContactId contact, final String messageId) {
        if (mXmsLog.markMessageAsRead(contact, messageId)) {
            mCmsSessionCtrl.scheduleImOperation(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCmsSessionCtrl.onReadXmsMessage(contact, messageId);

                    } catch (RuntimeException e) {
                        /*
                         * Intentionally catch runtime exceptions as else it will abruptly end the
                         * thread and eventually bring the whole system down, which is not intended.
                         */
                        sLogger.error("Failed to mark message as read!", e);
                    }
                }
            });
            broadcastMessageRead(contact, messageId);

        } else {
            /* no reporting towards the network if message is already marked as read */
            if (sLogger.isActivated()) {
                sLogger.info("Message with ID " + messageId + " is already marked as read!");
            }
        }
    }

    @Override
    public void markXmsMessageAsRead(final ContactId contact, final String messageId)
            throws RemoteException {
        if (TextUtils.isEmpty(messageId)) {
            throw new ServerApiIllegalArgumentException("message ID must not be null or empty!");
        }
        markXmsMessageAsRead_(contact, messageId);
    }

    @Override
    public void addEventListener2(IXmsMessageListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
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
        mCmsSessionCtrl.scheduleImOperation(new XmsDeleteTask(this, mLocalContentResolver,
                mCmsSessionCtrl));
    }

    @Override
    public void deleteXmsMessages2(final ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("Contact must not be null!");
        }
        mCmsSessionCtrl.scheduleImOperation(new XmsDeleteTask(this, mLocalContentResolver, contact,
                mCmsSessionCtrl));
    }

    public void deleteXmsMessageByIdAndContact(ContactId contact, String messageId) {
        mCmsSessionCtrl.scheduleImOperation(new XmsDeleteTask(this, mLocalContentResolver, contact,
                messageId, mCmsSessionCtrl));
    }

    @Override
    public void deleteXmsMessage(ContactId contact, String messageId) throws RemoteException {
        if (TextUtils.isEmpty(messageId)) {
            throw new ServerApiIllegalArgumentException("message ID must not be null or empty!");
        }
        deleteXmsMessageByIdAndContact(contact, messageId);
    }

    /*
     * This method is only for debug purpose.
     */
    @Override
    public void deleteImapData() throws RemoteException {
        mCmsSessionCtrl.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.info("Delete IMAP data");
                }
                try {
                    mCmsSessionCtrl.deleteCmsData();

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error("Failed to delete cms data", e);
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

    public void broadcastNewMessage(String mimeType, String msgId) {
        mXmsMessageBroadcaster.broadcastNewMessage(mimeType, msgId);
    }

    public void broadcastMessageDeleted(ContactId contact, Set<String> messageIds) {
        mXmsMessageBroadcaster.broadcastMessageDeleted(contact, messageIds);
    }

    public void broadcastMessageRead(ContactId contact, String messageId) {
        mXmsMessageBroadcaster.broadcastMessageRead(contact, messageId);
    }

    public void broadcastMessageStateChanged(ContactId contact, String mimeType, String msgId,
            XmsMessage.State state, XmsMessage.ReasonCode reasonCode) {
        mXmsMessageBroadcaster.broadcastMessageStateChanged(contact, mimeType, msgId, state,
                reasonCode);
    }

    @Override
    public void onCmsOperationExecuted(CmsSyncSchedulerTaskType operation, SyncType syncType,
            boolean result, Object param) {
        switch (syncType) {
            case ALL:
                if (sLogger.isActivated()) {
                    sLogger.debug("CMS sync finished for all (result=" + result + ")");
                }
                broadcastAllSynchronized();
                break;
            case GROUP:
                if (sLogger.isActivated()) {
                    sLogger.debug("CMS sync finished for group=" + param + " (result=" + result
                            + ")");
                }
                broadcastGroupConversationSynchronized((String) param);
                break;
            case ONE_TO_ONE:
                if (sLogger.isActivated()) {
                    ContactId contact = (ContactId) param;
                    sLogger.debug("CMS sync finished for contact=" + contact + " (result=" + result
                            + ")");
                }
                broadcastOneToOneConversationSynchronized((ContactId) param);
                break;
        }
    }

    /**
     * Deletes MMS parts
     *
     * @param contact the remote contact
     * @param messageId the message ID
     */
    public void deleteMmsParts(ContactId contact, String messageId) {
        // Check if MMS message
        Cursor cursor = null;
        RcsService.Direction dir = null;
        boolean deleteParts = false;
        try {
            cursor = mXmsLog.getXmsMessage(contact, messageId);
            if (!cursor.moveToNext()) {
                return;
            }
            String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_MIME_TYPE));
            if (XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                dir = RcsService.Direction.valueOf(cursor.getInt(cursor
                        .getColumnIndexOrThrow(XmsData.KEY_DIRECTION)));
                if (RcsService.Direction.INCOMING == dir) {
                    // For incoming MMS, messageId is unique: delete parts
                    deleteParts = true;
                } else {
                    /*
                     * For outgoing MMS, only remove parts if there is only one message left
                     * referencing this part.
                     */
                    if (mXmsLog.getCountXms(messageId) == 1) {
                        deleteParts = true;
                    }
                }
            }
        } finally {
            CursorUtil.close(cursor);
        }
        if (deleteParts) {
            mXmsLog.deleteMmsParts(messageId, dir);
        }
    }

    /**
     * Removes a XMS message from cache
     *
     * @param contact the remote contact
     * @param messageId the message ID
     */
    public void removeXmsMessage(ContactId contact, String messageId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a XMS message from cache (size=" + mXmsMessageCache.size() + ")");
        }
        String key = contact.toString() + messageId;
        mXmsMessageCache.remove(key);
    }

    public void deleteNativeXms(ContactId contact, String messageId) {
        Cursor cursor = null;
        boolean delete = false;
        Long nativeId;
        String mimeType;
        try {
            cursor = mXmsLog.getXmsMessage(contact, messageId);
            if (!cursor.moveToNext()) {
                return;
            }
            int nativeIdColumnIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_ID);
            if (cursor.isNull(nativeIdColumnIdx)) {
                return;
            }
            nativeId = cursor.getLong(nativeIdColumnIdx);
            mimeType = cursor.getString(cursor.getColumnIndexOrThrow(XmsData.KEY_MIME_TYPE));
            if (XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                RcsService.Direction dir = RcsService.Direction.valueOf(cursor.getInt(cursor
                        .getColumnIndexOrThrow(XmsData.KEY_DIRECTION)));
                if (RcsService.Direction.INCOMING == dir) {
                    // For incoming MMS, messageId is unique: delete
                    delete = true;
                } else {
                    /*
                     * For outgoing MMS, only remove if there is only one message left referencing
                     * this part.
                     */
                    if (mXmsLog.getCountXms(messageId) == 1) {
                        delete = true;
                    }
                }
            } else {
                delete = true;
            }
        } finally {
            CursorUtil.close(cursor);
        }
        if (delete) {
            if (XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE.equals(mimeType)) {
                if (mSmsMmsLog.deleteMms(nativeId) == 0) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Failed to delete MMS from native provider for ID=" + nativeId);
                    }
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Delete MMS from native provider for ID=" + nativeId);
                    }
                }
            } else {
                if (mSmsMmsLog.deleteSms(nativeId) == 0) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Failed to delete SMS from native provider for ID=" + nativeId);
                    }
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Delete SMS from native provider for ID=" + nativeId);
                    }
                }
            }
        }
    }
}

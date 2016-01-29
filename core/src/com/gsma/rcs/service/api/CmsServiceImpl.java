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
 ******************************************************************************/

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.cms.sync.scheduler.SchedulerTaskType;
import com.gsma.rcs.core.cms.sync.scheduler.SchedulerListener;
import com.gsma.rcs.core.cms.sync.scheduler.Scheduler.SyncType;
import com.gsma.rcs.core.cms.service.CmsService;
import com.gsma.rcs.core.cms.xms.mms.MmsSessionListener;
import com.gsma.rcs.core.cms.xms.mms.OriginatingMmsSession;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.XmsPersistedStorageAccessor;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.service.broadcaster.CmsEventBroadcaster;
import com.gsma.rcs.service.broadcaster.XmsMessageEventBroadcaster;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.ICmsService;
import com.gsma.services.rcs.cms.ICmsSynchronizationListener;
import com.gsma.services.rcs.cms.IXmsMessage;
import com.gsma.services.rcs.cms.IXmsMessageListener;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common Message Store service implementation
 *
 * @author Philippe LEMORDANT
 */
public class CmsServiceImpl extends ICmsService.Stub implements MmsSessionListener, SchedulerListener {

    private static final Logger sLogger = Logger.getLogger(CmsServiceImpl.class.getSimpleName());
    private final CmsEventBroadcaster mCmsBroadcaster = new CmsEventBroadcaster();
    private final XmsMessageEventBroadcaster mXmsMessageBroadcaster = new XmsMessageEventBroadcaster();

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();
    private final CmsService mCmsService;

    private final Map<String, XmsMessageImpl> mXmsMessageCache = new HashMap<>();
    private final XmsLog mXmsLog;
    private final Context mContext;
    private final XmsManager mXmsManager;
    private final RcsSettings mRcsSettings;

    public static final String[] BLACK_LISTED_MODELS = new String[] {
        "LG-H955"
    };
    public static final Set<String> sBlackListedModel = new HashSet<>(
            Arrays.asList(BLACK_LISTED_MODELS));

    /**
     * Constructor
     */
    public CmsServiceImpl(Context context, CmsService cmsService, ChatServiceImpl chatService,
            XmsLog xmsLog, RcsSettings rcsSettings, XmsManager xmsManager) {
        if (sLogger.isActivated()) {
            sLogger.info("CMS service API is loaded");
        }
        mContext = context;
        mCmsService = cmsService;
        mCmsService.register(this);
        mCmsService.register(chatService);
        mXmsLog = xmsLog;
        mRcsSettings = rcsSettings;
        mXmsManager = xmsManager;
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
                    sLogger.info("Sync One-to-One conversation for contact ".concat(contact
                            .toString()));
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
                    sLogger.error("Failed to synchronize group conversation for chat ID " + chatId,
                            e);
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
            return getOrCreateXmsMessage(messageId);

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

    public XmsMessageImpl getOrCreateXmsMessage(String messageId) {
        XmsMessageImpl xmsMessage = mXmsMessageCache.get(messageId);
        if (xmsMessage != null) {
            return xmsMessage;
        }
        XmsPersistedStorageAccessor accessor = new XmsPersistedStorageAccessor(mXmsLog, messageId);
        XmsMessageImpl result = new XmsMessageImpl(messageId, accessor);
        mXmsMessageCache.put(messageId, result);
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
        try {
            mXmsManager.sendSms(contact, text);

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to send SMS!", e);
        }
    }

    @Override
    public boolean isAllowedToSendMultimediaMessage() throws RemoteException {
        try {
            return !sBlackListedModel.contains(Build.MODEL)
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        } catch (RuntimeException e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public IXmsMessage sendMultimediaMessage(final ContactId contact, List<Uri> files,
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
        if (sBlackListedModel.contains(Build.MODEL)
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new ServerApiPermissionDeniedException("MMS cannot be sent from API!");
        }
        try {
            checkUris(files);
            String mMessageId = IdGenerator.generateMessageID();
            long timestamp = System.currentTimeMillis();
            long maxFileIconSize = mRcsSettings.getMaxFileIconSize();
            MmsDataObject mmsDataObject = new MmsDataObject(mContext, null, mMessageId, contact,
                    subject, body, RcsService.Direction.OUTGOING, timestamp, files, null,
                    maxFileIconSize);
            mmsDataObject.setReadStatus(ReadStatus.READ);
            mXmsLog.addOutgoingMms(mmsDataObject);
            XmsMessageImpl mms = getOrCreateXmsMessage(mMessageId);
            mCmsService.tryToDequeueMmsMessages();
            return mms;

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

    private void checkUris(List<Uri> files) {
        for (Uri file : files) {
            if (!FileUtils.isReadFromUriPossible(mContext, file)) {
                throw new ServerApiIllegalArgumentException("file '" + file.toString()
                        + "' must refer to a file that exists and that is readable by stack!");
            }
            String fileName = FileUtils.getFileName(mContext, file);
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
                            if (mCmsService.isServiceStarted()) {
                                mCmsService.markXmsMessageAsRead(messageId);
                            }
                        }
                    } else {
                        sLogger.warn("Cannot mark as read: message ID'" + messageId
                                + "' not found!");
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
                    if (mCmsService.isServiceStarted()) {
                        mCmsService.deleteXmsMessages();
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
                    if (mCmsService.isServiceStarted()) {
                        mCmsService.deleteXmsMessages2(contact);
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
                    if (mCmsService.isServiceStarted()) {
                        mCmsService.deleteXmsMessage(messageId);
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

    public void broadcastNewMessage(String mimeType, String msgId) {
        mXmsMessageBroadcaster.broadcastNewMessage(mimeType, msgId);
    }

    public void broadcastMessageDeleted(ContactId contact, Set<String> messageIds) {
        mXmsMessageBroadcaster.broadcastMessageDeleted(contact, messageIds);
    }

    /**
     * Dequeue MMS
     *
     * @param mmsId The message ID
     * @param contact The remote contact
     * @param subject The subject
     * @param parts The MMS attachment parts
     */
    public void dequeueMmsMessage(String mmsId, ContactId contact, String subject,
            List<MmsDataObject.MmsPart> parts) {
        if (sLogger.isActivated()) {
            sLogger.debug("Dequeue MMS ID=".concat(mmsId));
        }
        /* For outgoing MMS transfer, timestampSent = timestamp */
        OriginatingMmsSession session = new OriginatingMmsSession(mContext, mmsId, contact,
                subject, parts, mRcsSettings, mXmsManager);
        session.addListener(this);
        session.addListener(mCmsService.getCmsManager().getMmsSessionHandler());
        mCmsService.scheduleImOperation(session);
    }

    public void setXmsStateAndReasonCode(String messageId, String mimeType, ContactId contact,
            XmsMessage.State state, XmsMessage.ReasonCode reasonCode) {
        if (mXmsLog.setStateAndReasonCode(messageId, state, reasonCode)) {
            mXmsMessageBroadcaster.broadcastMessageStateChanged(contact, mimeType, messageId,
                    state, reasonCode);
        }
    }

    public void setXmsStateAndTimestamp(String mmsId, ContactId contact, XmsMessage.State state,
            long timestamp, long timestampSent) {
        if (mXmsLog.setStateAndTimestamp(mmsId, state, XmsMessage.ReasonCode.UNSPECIFIED,
                timestamp, timestampSent)) {
            broadcastMessageStateChanged(contact, XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, mmsId,
                    state, XmsMessage.ReasonCode.UNSPECIFIED);
        }
    }

    public void broadcastMessageStateChanged(ContactId contact, String mimeType, String msgId,
            XmsMessage.State state, XmsMessage.ReasonCode reasonCode) {
        mXmsMessageBroadcaster.broadcastMessageStateChanged(contact, mimeType, msgId, state,
                reasonCode);
    }

    @Override
    public void onMmsTransferError(ReasonCode reason, ContactId contact, String mmsId) {
        setXmsStateAndReasonCode(mmsId, MimeType.MULTIMEDIA_MESSAGE, contact, State.FAILED, reason);
    }

    @Override
    public void onMmsTransferred(ContactId contact, String mmsId) {
        setXmsStateAndReasonCode(mmsId, MimeType.MULTIMEDIA_MESSAGE, contact, State.SENT,
                ReasonCode.UNSPECIFIED);
    }

    @Override
    public void onMmsTransferStarted(ContactId contact, String mmsId) {
        long timestamp = System.currentTimeMillis();
        setXmsStateAndTimestamp(mmsId, contact, XmsMessage.State.SENDING, timestamp, timestamp);
    }

    public XmsMessageEventBroadcaster getXmsMessageBroadcaster() {
        return mXmsMessageBroadcaster;
    }


    @Override
    public void onCmsOperationExecuted(SchedulerTaskType operation, SyncType syncType, boolean result, Object param) {

        if(syncType == SyncType.ONE_TO_ONE){
            broadcastOneToOneConversationSynchronized((ContactId)param);
        }
        else if(syncType == SyncType.GROUP){
            broadcastGroupConversationSynchronized((String)param);
        }
        else if(syncType == SyncType.ALL){
            broadcastAllSynchronized();
        }
    }
}

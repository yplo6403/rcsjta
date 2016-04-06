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

package com.gsma.rcs.core.cms.sync.scheduler;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler.SyncParams.ExtraParameter;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncBasicTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncPushRequestTask;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is used to schedule operation with the message store
 */
public class CmsSyncScheduler {

    private static final Logger sLogger = Logger.getLogger(CmsSyncScheduler.class.getSimpleName());
    private static final String MESSAGE_STORE_SYNC_OPERATIONS = "MessageStoreSyncOperations";

    /**
     * When entering in a conversation, the method markMessageAsRead (at the API level) is invoked
     * for each unread message. We must delayed the Update Flag Task, otherwise this task will be
     * executed before that all messages has been marked as read from the API.
     */
    private static final long UPDATE_FLAG_TASK_DELAY_IN_MS = 1000L;

    /* package private */static long sEndOfLastSync = 0L;

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final LocalStorage mLocalStorage;
    private final CmsLog mCmsLog;
    private final XmsLog mXmsLog;

    ImapServiceHandler mImapServiceHandler;

    /* package private */CmsSyncSchedulerTaskType mCurrentOperation;

    /* package private */SyncRequestHandler mSyncRequestHandler;
    private boolean mStarted;

    public enum SyncType {
        ONE_TO_ONE, GROUP, ALL, UNSPECIFIED
    }

    private Map<CmsSyncSchedulerTaskType, Set<CmsSyncSchedulerListener>> mListeners;

    /**
     * Constructor
     *
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param localStorage the local storage
     * @param cmsLog the CMS log accessor
     * @param xmsLog the XMS log accessor
     */
    public CmsSyncScheduler(Context context, RcsSettings rcsSettings, LocalStorage localStorage,
            CmsLog cmsLog, XmsLog xmsLog) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mLocalStorage = localStorage;
        mCmsLog = cmsLog;
        mXmsLog = xmsLog;
        mStarted = false;
        mListeners = new HashMap<>();
        mImapServiceHandler = new ImapServiceHandler(mRcsSettings);
    }

    private SyncRequestHandler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new SyncRequestHandler(thread.getLooper());
    }

    public synchronized void start() {
        if (mStarted) {
            return;
        }
        mStarted = true;
        mSyncRequestHandler = allocateBgHandler(MESSAGE_STORE_SYNC_OPERATIONS);
        init();
    }

    public synchronized void stop() {
        if (!mStarted) {
            return;
        }
        mSyncRequestHandler.getLooper().quit();
        mSyncRequestHandler.getLooper().getThread().interrupt();
        mStarted = false;
    }

    public synchronized void registerListener(CmsSyncSchedulerTaskType operation,
            CmsSyncSchedulerListener listener) {
        Set<CmsSyncSchedulerListener> listeners = mListeners.get(operation);
        if (listeners == null) {
            listeners = new HashSet<>();
            mListeners.put(operation, listeners);
        }
        listeners.add(listener);
    }

    public synchronized void unregisterListener(CmsSyncSchedulerTaskType operation,
            CmsSyncSchedulerListener listener) {
        Set<CmsSyncSchedulerListener> listeners = mListeners.get(operation);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public boolean scheduleSyncForOneToOneConversation(ContactId contactId) {
        SyncParams parameters = new SyncParams(SyncType.ONE_TO_ONE);
        parameters.addExtraParameter(ExtraParameter.CONTACT_ID, contactId);
        return mStarted && schedule(CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY, parameters);
    }

    public boolean scheduleSyncForGroupConversation(String chatId) {
        SyncParams parameters = new SyncParams(SyncType.GROUP);
        parameters.addExtraParameter(ExtraParameter.CHAT_ID, chatId);
        return mStarted && schedule(CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY, parameters);
    }

    public boolean scheduleSync() {
        SyncParams parameters = new SyncParams(SyncType.ALL);
        return mStarted && schedule(CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY, parameters);
    }

    public boolean schedulePushMessages(ContactId contactId) {
        SyncParams parameters = new SyncParams(SyncType.UNSPECIFIED);
        parameters.addExtraParameter(ExtraParameter.CONTACT_ID, contactId);
        parameters.addExtraParameter(ExtraParameter.DELAY, UPDATE_FLAG_TASK_DELAY_IN_MS);
        return mStarted && schedule(CmsSyncSchedulerTaskType.PUSH_MESSAGES, parameters);
    }

    public boolean scheduleUpdateFlags(ContactId contact) {
        SyncParams parameters = new SyncParams(SyncType.UNSPECIFIED);
        parameters.addExtraParameter(ExtraParameter.CONTACT_ID, contact);
        parameters.addExtraParameter(ExtraParameter.DELAY, UPDATE_FLAG_TASK_DELAY_IN_MS);
        return mStarted && schedule(CmsSyncSchedulerTaskType.UPDATE_FLAGS, parameters);
    }

    public boolean scheduleUpdateFlags(String chatId) {
        SyncParams parameters = new SyncParams(SyncType.UNSPECIFIED);
        parameters.addExtraParameter(ExtraParameter.CHAT_ID, chatId);
        parameters.addExtraParameter(ExtraParameter.DELAY, UPDATE_FLAG_TASK_DELAY_IN_MS);
        return mStarted && schedule(CmsSyncSchedulerTaskType.UPDATE_FLAGS, parameters);
    }

    public void init() {
        SyncParams parameters = new SyncParams(SyncType.ALL);
        boolean scheduled = schedule(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION, parameters);
        if (!scheduled) {
            long syncTimerInterval = mRcsSettings.getMessageStoreSyncTimer();
            if (syncTimerInterval <= 0) { // Periodic sync is disabled
                return;
            }
            // start new periodic sync
            long delta = NtpTrustedTime.currentTimeMillis() - sEndOfLastSync;
            if (delta < syncTimerInterval) {
                parameters.addExtraParameter(ExtraParameter.DELAY, syncTimerInterval - delta);
            }
            schedule(CmsSyncSchedulerTaskType.SYNC_PERIODIC, parameters);
        }
    }

    private boolean canScheduleNewOperation(CmsSyncSchedulerTaskType newOperation) {
        if (mSyncRequestHandler.hasMessages(newOperation.toInt())) {
            /*
             * Cannot schedule new operation if there is one pending of same type
             */
            return false;
        }
        switch (newOperation) {
            case SYNC_PERIODIC:
            case SYNC_FOR_DATA_CONNECTION:
            case SYNC_FOR_USER_ACTIVITY:
                if (mCurrentOperation == CmsSyncSchedulerTaskType.SYNC_PERIODIC
                        || mCurrentOperation == CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION
                        || mCurrentOperation == CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY) {
                    return false;
                }
                long now = NtpTrustedTime.currentTimeMillis();
                long delta = now - sEndOfLastSync;
                long dataConnectionInterval = mRcsSettings.getDataConnectionSyncTimer();
                if (CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION == newOperation
                        && (delta < dataConnectionInterval)) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("DataConnection event discarded (interval="
                                + dataConnectionInterval + ") (Last=" + sEndOfLastSync + ") (Now="
                                + now + ")");
                    }
                    return false;
                }
                break;

            default:
                if (mCurrentOperation == newOperation) {
                    return false;
                }
        }
        return true;
    }

    private synchronized boolean schedule(CmsSyncSchedulerTaskType newOperation,
            SyncParams parameters) {
        if (!canScheduleNewOperation(newOperation)) {
            if (sLogger.isActivated()) {
                traceScheduler();
            }
            return false;
        }
        if (sLogger.isActivated()) {
            sLogger.info("Schedule new operation with message store:");
            sLogger.info("--> " + newOperation.toString());
            sLogger.info("--> " + parameters.toString());
        }
        Message message;
        switch (newOperation) {
            case SYNC_PERIODIC:
                message = mSyncRequestHandler.obtainMessage(
                        CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt(), parameters);
                /* Schedule next periodic sync */
                mSyncRequestHandler.sendMessageDelayed(message,
                        mRcsSettings.getMessageStoreSyncTimer());
                break;

            case SYNC_FOR_DATA_CONNECTION:
            case SYNC_FOR_USER_ACTIVITY:
                /* Remove periodic sync if there is one scheduled */
                mSyncRequestHandler.removeMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt());
                message = mSyncRequestHandler.obtainMessage(newOperation.toInt(), parameters);
                mSyncRequestHandler.sendMessage(message);
                break;

            case PUSH_MESSAGES:
            case UPDATE_FLAGS:
                message = mSyncRequestHandler.obtainMessage(newOperation.toInt(), parameters);
                mSyncRequestHandler.sendMessageDelayed(message,
                        (long) parameters.getExtraParameter(ExtraParameter.DELAY));
                break;
        }
        return true;
    }

    /**
     * Handler used for sync request
     */
    /* package private */class SyncRequestHandler extends Handler {

        SyncRequestHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mCurrentOperation = CmsSyncSchedulerTaskType.valueOf(msg.what);
            SyncParams syncParams = null;
            boolean result = false;
            switch (mCurrentOperation) {
                case PUSH_MESSAGES:
                case UPDATE_FLAGS:
                    result = executePushRequest();
                    break;

                case SYNC_FOR_DATA_CONNECTION:
                case SYNC_FOR_USER_ACTIVITY:
                case SYNC_PERIODIC:
                    syncParams = (SyncParams) msg.obj;
                    result = executeSync(syncParams);
                    break;
            }
            // notify listeners
            Set<CmsSyncSchedulerListener> listeners = mListeners.get(mCurrentOperation);
            if (listeners != null) {
                for (CmsSyncSchedulerListener listener : listeners) {
                    SyncType syncType = SyncType.UNSPECIFIED;
                    Object callbackParam = null;
                    if (syncParams != null) {
                        syncType = syncParams.mSyncType;
                        if (SyncType.ONE_TO_ONE == syncType) {
                            callbackParam = syncParams.getExtraParameter(ExtraParameter.CONTACT_ID);
                        } else if (SyncType.GROUP == syncType) {
                            callbackParam = syncParams.getExtraParameter(ExtraParameter.CHAT_ID);
                        }
                    }
                    listener.onCmsOperationExecuted(mCurrentOperation, syncType, result,
                            callbackParam);
                }
            }
            mCurrentOperation = null;
            sEndOfLastSync = NtpTrustedTime.currentTimeMillis();
            // schedule new periodic sync on all conversations
            long syncTimerInterval = mRcsSettings.getMessageStoreSyncTimer();
            if (syncTimerInterval > 0) {
                syncParams = new SyncParams(SyncType.ALL);
                syncParams.addExtraParameter(ExtraParameter.DELAY, syncTimerInterval);
                schedule(CmsSyncSchedulerTaskType.SYNC_PERIODIC, syncParams);
            }
        }
    }

    private boolean executeSync(SyncParams syncParams) {
        String remoteFolder = null;
        if (syncParams.mSyncType == SyncType.ONE_TO_ONE) {
            ContactId contact = (ContactId) syncParams.getExtraParameter(ExtraParameter.CONTACT_ID);
            remoteFolder = CmsUtils.contactToCmsFolder(contact);

        } else if (syncParams.mSyncType == SyncType.GROUP) {
            String chatId = (String) syncParams.getExtraParameter(ExtraParameter.CHAT_ID);
            remoteFolder = CmsUtils.groupChatToCmsFolder(chatId, chatId);
        }

        CmsSyncBasicTask task = null;
        switch (syncParams.mSyncType) {
            case ONE_TO_ONE:
            case GROUP:
                task = new CmsSyncBasicTask(mContext, mRcsSettings, mLocalStorage, remoteFolder,
                        mXmsLog, mCmsLog);
                break;
            case ALL:
                task = new CmsSyncBasicTask(mContext, mRcsSettings, mLocalStorage, mXmsLog, mCmsLog);
                break;
        }
        boolean res = executeTask(task);
        if (res) {
            if (sLogger.isActivated()) {
                String msg = "Sync successfully executed";
                if (remoteFolder != null) {
                    msg += " for folder : " + remoteFolder;
                }
                sLogger.info(msg);
            }
        }
        return res;
    }

    private boolean executePushRequest() {
        CmsSyncPushRequestTask.ICmsSyncPushRequestTask callback = new CmsSyncPushRequestTask.ICmsSyncPushRequestTask() {
            @Override
            public void onMessagesPushed(Map<String, Integer> uids) {
                for (Entry<String, Integer> entry : uids.entrySet()) {
                    String baseId = entry.getKey();
                    Integer uid = entry.getValue();
                    mCmsLog.updateXmsPushStatus(uid, baseId, PushStatus.PUSHED);
                }
            }

            @Override
            public void onReadRequestsReported(String folder, Set<Integer> uids) {
                for (Integer uid : uids) {
                    mCmsLog.updateReadStatus(folder, uid, ReadStatus.READ);
                }
            }

            @Override
            public void onDeleteRequestsReported(String folder, Set<Integer> uids) {
                for (Integer uid : uids) {
                    mCmsLog.updateDeleteStatus(folder, uid, DeleteStatus.DELETED);
                }
            }
        };
        CmsSyncPushRequestTask task = new CmsSyncPushRequestTask(mContext, mRcsSettings, mXmsLog,
                mCmsLog, callback);
        boolean res = executeTask(task);
        if (res) { // update local storage when no error occurred during task execution
            if (sLogger.isActivated()) {
                sLogger.info("PushRequest task successfully executed");
            }
        }
        return res;
    }

    private boolean executeTask(CmsSyncSchedulerTask task) {
        boolean success = false;
        try {
            BasicImapService basicImapService = mImapServiceHandler.openService();
            task.execute(basicImapService);
            success = true;
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Failed to sync : " + e.getMessage());
            }
        } catch (FileAccessException | PayloadException | RuntimeException e) {
            sLogger.error("Failed to sync : ", e);

        } finally {
            try {
                mImapServiceHandler.closeService();
            } catch (NetworkException | PayloadException | RuntimeException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Failed to sync : " + e);
                }
            }
        }
        return success;
    }

    /* package private */static class SyncParams {

        enum ExtraParameter {
            DELAY, // delay before executing task in ms
            CONTACT_ID, CHAT_ID
        }

        final SyncType mSyncType;
        final Map<ExtraParameter, Object> mExtraParameter;

        SyncParams(SyncType syncType) {
            mSyncType = syncType;
            mExtraParameter = new HashMap<>();
            mExtraParameter.put(ExtraParameter.DELAY, 0L);
        }

        void addExtraParameter(ExtraParameter extra, Object value) {
            mExtraParameter.put(extra, value);
        }

        Object getExtraParameter(ExtraParameter extra) {
            return mExtraParameter.get(extra);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SyncParams{");
            sb.append("mSyncType=").append(mSyncType);
            for (Entry<ExtraParameter, Object> entry : mExtraParameter.entrySet()) {
                sb.append(", ").append(entry.getKey()).append("=").append(entry.getValue());
            }
            sb.append('}');
            return sb.toString();
        }
    }

    private void traceScheduler() {
        sLogger.debug(" >>> Scheduler state : ");
        sLogger.debug("     --> CurrentOperation : "
                + (mCurrentOperation == null ? "null" : mCurrentOperation));
        if (mSyncRequestHandler.hasMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt())) {
            sLogger.debug("     --> has messages of type " + CmsSyncSchedulerTaskType.SYNC_PERIODIC);
        }
        if (mSyncRequestHandler.hasMessages(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION
                .toInt())) {
            sLogger.debug("     --> has messages of type "
                    + CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION);
        }
        if (mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY.toInt())) {
            sLogger.debug("     --> has messages of type "
                    + CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY);
        }
        if (mSyncRequestHandler.hasMessages(CmsSyncSchedulerTaskType.PUSH_MESSAGES.toInt())) {
            sLogger.debug("     --> has messages of type " + CmsSyncSchedulerTaskType.PUSH_MESSAGES);
        }
        if (mSyncRequestHandler.hasMessages(CmsSyncSchedulerTaskType.UPDATE_FLAGS.toInt())) {
            sLogger.debug("     --> has messages of type " + CmsSyncSchedulerTaskType.UPDATE_FLAGS);
        }
        sLogger.debug(" <<< Scheduler state");
    }
}

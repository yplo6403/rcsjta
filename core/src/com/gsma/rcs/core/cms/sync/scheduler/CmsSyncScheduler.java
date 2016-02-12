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

package com.gsma.rcs.core.cms.sync.scheduler;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.sync.process.FlagChange;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.sync.process.Synchronizer;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler.SyncParams.ExtraParameter;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncPushMessageTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncPushMessageTask.PushMessageTaskListener;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncUpdateFlagTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncUpdateFlagTask.UpdateFlagTaskListener;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventFrameworkMode;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is used to schedule operation with the message store
 */
public class CmsSyncScheduler implements PushMessageTaskListener, UpdateFlagTaskListener {

    private static final Logger sLogger = Logger.getLogger(CmsSyncScheduler.class.getSimpleName());
    private static final String MESSAGE_STORE_SYNC_OPERATIONS = "MessageStoreSyncOperations";

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
        return mStarted && schedule(CmsSyncSchedulerTaskType.PUSH_MESSAGES, parameters);
    }

    public boolean scheduleUpdateFlags(EventFrameworkMode xmsMode, EventFrameworkMode chatMode) {
        SyncParams parameters = new SyncParams(SyncType.UNSPECIFIED);
        parameters.addExtraParameter(ExtraParameter.UPDATE_XMS_FLAGS, xmsMode);
        parameters.addExtraParameter(ExtraParameter.UPDATE_CHAT_FLAGS, chatMode);
        return mStarted && schedule(CmsSyncSchedulerTaskType.UPDATE_FLAGS, parameters);
    }

    public void init() {
        SyncParams parameters = new SyncParams(SyncType.ALL);
        boolean scheduled = schedule(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION, parameters);
        if (!scheduled) {
            // start new periodic sync
            long delta = System.currentTimeMillis() - sEndOfLastSync;
            long syncTimerInterval = mRcsSettings.getMessageStoreSyncTimer();
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
                long now = System.currentTimeMillis();
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
                mSyncRequestHandler.sendMessage(message);
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

            try {
                BasicImapService basicImapService = mImapServiceHandler.openService();
                switch (mCurrentOperation) {
                    case PUSH_MESSAGES:
                        syncParams = (SyncParams) msg.obj;
                        executePush(basicImapService,
                                (ContactId) syncParams.getExtraParameter(ExtraParameter.CONTACT_ID));
                        result = true;
                        break;

                    case UPDATE_FLAGS:
                        syncParams = (SyncParams) msg.obj;
                        EventFrameworkMode xmsMode = (EventFrameworkMode) syncParams
                                .getExtraParameter(ExtraParameter.UPDATE_XMS_FLAGS);
                        EventFrameworkMode chatMode = (EventFrameworkMode) syncParams
                                .getExtraParameter(ExtraParameter.UPDATE_CHAT_FLAGS);
                        executeUpdate(basicImapService, xmsMode, chatMode);
                        result = true;
                        break;

                    case SYNC_FOR_DATA_CONNECTION:
                    case SYNC_FOR_USER_ACTIVITY:
                    case SYNC_PERIODIC:
                        syncParams = (SyncParams) msg.obj;
                        executeSync(basicImapService, syncParams);
                        result = true;
                }
            } catch (NetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Failed to sync : " + e.getMessage());
                }
            } catch (PayloadException | FileAccessException | RuntimeException e) {
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
            sEndOfLastSync = System.currentTimeMillis();
            // schedule new periodic sync on all conversations
            syncParams = new SyncParams(SyncType.ALL);
            syncParams.addExtraParameter(ExtraParameter.DELAY,
                    mRcsSettings.getMessageStoreSyncTimer());
            schedule(CmsSyncSchedulerTaskType.SYNC_PERIODIC, syncParams);
        }
    }

    private void executeSync(BasicImapService basicImapService, SyncParams syncParams)
            throws PayloadException, NetworkException, FileAccessException {
        String remoteFolder = null;
        if (syncParams.mSyncType == SyncType.ONE_TO_ONE) {
            ContactId contact = (ContactId) syncParams.getExtraParameter(ExtraParameter.CONTACT_ID);
            remoteFolder = CmsUtils.contactToCmsFolder(mRcsSettings, contact);

        } else if (syncParams.mSyncType == SyncType.GROUP) {
            String chatId = (String) syncParams.getExtraParameter(ExtraParameter.CHAT_ID);
            remoteFolder = CmsUtils.groupChatToCmsFolder(mRcsSettings, chatId, chatId);
        }

        Synchronizer synchronizer = new Synchronizer(mContext, mRcsSettings, mLocalStorage,
                basicImapService);
        switch (syncParams.mSyncType) {
            case ONE_TO_ONE:
            case GROUP:
                synchronizer.syncFolder(remoteFolder);
                break;
            case ALL:
                synchronizer.syncAll();
                break;
        }
    }

    private void executePush(BasicImapService basicImapService, ContactId contact) {
        CmsSyncPushMessageTask task = new CmsSyncPushMessageTask(mContext, mRcsSettings, mXmsLog,
                mCmsLog, contact, this);
        task.setBasicImapService(basicImapService);
        task.run();
    }

    private void executeUpdate(BasicImapService basicImapService, EventFrameworkMode xmsMode,
            EventFrameworkMode chatMode) {
        CmsSyncUpdateFlagTask task = new CmsSyncUpdateFlagTask(mCmsLog, xmsMode, chatMode, this);
        task.setBasicImapService(basicImapService);
        task.run();
    }

    /* package private */static class SyncParams {

        enum ExtraParameter {
            DELAY, // delay before executing task in ms
            CONTACT_ID, CHAT_ID, UPDATE_XMS_FLAGS, UPDATE_CHAT_FLAGS
        }

        final SyncType mSyncType;
        final Map<ExtraParameter, Object> mExtraParameter;

        SyncParams(SyncType syncType) {
            mSyncType = syncType;
            mExtraParameter = new HashMap<>();
            mExtraParameter.put(ExtraParameter.DELAY, 0);
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

    @Override
    public void onPushMessageTaskCallbackExecuted(Map<String, Integer> uidsMap) {
        if (sLogger.isActivated()) {
            sLogger.info("onPushMessageTaskCallbackExecuted");
        }
        for (Entry<String, Integer> entry : uidsMap.entrySet()) {
            String baseId = entry.getKey();
            Integer uid = entry.getValue();
            mCmsLog.updateXmsPushStatus(uid, baseId, PushStatus.PUSHED);
        }
    }

    @Override
    public void onUpdateFlagTaskExecuted(List<FlagChange> flags) {
        if (sLogger.isActivated()) {
            sLogger.info("onUpdateFlagTaskExecuted");
        }
        for (FlagChange fg : flags) {
            boolean deleted = fg.isDeleted();
            boolean seen = fg.isSeen();
            String folderName = fg.getFolder();
            for (Integer uid : fg.getUids()) {
                if (deleted) {
                    mCmsLog.updateDeleteStatus(folderName, uid, DeleteStatus.DELETED);
                } else if (seen) {
                    mCmsLog.updateReadStatus(folderName, uid, ReadStatus.READ);
                }
            }
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

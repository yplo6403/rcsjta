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

package com.gsma.rcs.cms.scheduler;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceHandler;
import com.gsma.rcs.cms.imap.task.CmsTask;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.imap.task.PushMessageTask.PushMessageTaskListener;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask.UpdateFlagTaskListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.Synchronizer;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
    This class is used to schedule operation with the message store
 */
public class CmsScheduler implements PushMessageTaskListener, UpdateFlagTaskListener {

    private static final Logger sLogger = Logger.getLogger(CmsScheduler.class.getSimpleName());
    private static final String MESSAGE_STORE_SYNC_OPERATIONS = "MessageStoreSyncOperations";

    private static long sEndOfLastSync = 0l;

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final LocalStorage mLocalStorage;
    private final ImapLog mImapLog;
    private final XmsLog mXmsLog;
    private final int mDataConnectionInterval;
    private final int mSyncTimerInterval;

    ImapServiceHandler mImapServiceHandler;

    /* package private */ CmsOperation mCurrentOperation;

    /* package private */ SyncRequestHandler mSyncRequestHandler;
    private boolean mStarted;

    public enum SyncType{
        ONE_TO_ONE, GROUP, ALL, UNSPECIFIED
    }

    private Map<CmsOperation, Set<CmsOperationListener>> mListeners;


    /**
     * Constructor
     *
     * @param rcsSettings the RCS settings accessor
     */
    public CmsScheduler(Context context, RcsSettings rcsSettings, LocalStorage localStorage, ImapLog imapLog, XmsLog xmsLog) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mLocalStorage = localStorage;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
        mDataConnectionInterval = rcsSettings.getDataConnectionSyncTimer();
        mSyncTimerInterval = rcsSettings.getMessageStoreSyncTimer();
        mStarted = false;
        mListeners = new HashMap();
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

    public synchronized void registerListener(CmsOperation operation, CmsOperationListener listener){
        Set<CmsOperationListener> listeners = mListeners.get(operation);
        if(listeners == null){
            listeners = new HashSet<>();
            mListeners.put(operation, listeners);
        }
        listeners.add(listener);
    }

    public synchronized void unregisterListener(CmsOperation operation, CmsOperationListener listener){
        Set<CmsOperationListener> listeners = mListeners.get(operation);
        if(listeners != null){
            listeners.remove(listener);
        }
    }

    public boolean scheduleSyncForOneToOneConversation(ContactId contactId){
        SyncParams parameters = new SyncParams<>(SyncType.ONE_TO_ONE, contactId, 0);
        return mStarted && schedule(CmsOperation.SYNC_FOR_USER_ACTIVITY, parameters);
    }

    public boolean scheduleSyncForGroupConversation(String chatId){
        SyncParams parameters = new SyncParams<>(SyncType.GROUP, chatId, 0);
        return mStarted && schedule(CmsOperation.SYNC_FOR_USER_ACTIVITY, parameters);
    }

    public boolean scheduleSync(){
        SyncParams parameters = new SyncParams<>(SyncType.ALL, null, 0);
        return mStarted && schedule(CmsOperation.SYNC_FOR_USER_ACTIVITY, parameters);
    }

    public boolean schedulePushMessages(ContactId contact){
        SyncParams parameters = new SyncParams<>(SyncType.UNSPECIFIED, contact, 0);
        return mStarted && schedule(CmsOperation.PUSH_MESSAGES, parameters);
    }

    public boolean scheduleUpdateFlags(){
        SyncParams parameters = new SyncParams<>(SyncType.UNSPECIFIED, null, 0);
        return mStarted && schedule(CmsOperation.UPDATE_FLAGS, parameters);
    }

    //TODO FGI : to be removed, defined for Cms Toolkit
    public boolean scheduleToolkitTask(CmsTask cmsTask){
        Message message = mSyncRequestHandler.obtainMessage(CmsOperation.TOOLKIT_TASK.toInt(),cmsTask);
        mSyncRequestHandler.sendMessage(message);
        return true;
    }

    public void init(){

        SyncParams parameters = new SyncParams<>(SyncType.ALL, null, 0);
        boolean scheduled = schedule(CmsOperation.SYNC_FOR_DATA_CONNECTION, parameters);
        if(!scheduled){
            // start new periodic sync
            long delta = System.currentTimeMillis() - sEndOfLastSync;
            if(delta > mSyncTimerInterval){
                parameters.mDelay = 0;
            } else{
              parameters.mDelay = mSyncTimerInterval-delta;
            }
            schedule(CmsOperation.SYNC_PERIODIC, parameters);
        }
    }

    private boolean canScheduleNewOperation(CmsOperation newOperation){

        if(mSyncRequestHandler.hasMessages(newOperation.toInt())){
            return false;
        }

        if(newOperation == CmsOperation.SYNC_PERIODIC ||
           newOperation == CmsOperation.SYNC_FOR_DATA_CONNECTION ||
           newOperation == CmsOperation.SYNC_FOR_USER_ACTIVITY){

            if(mCurrentOperation == CmsOperation.SYNC_PERIODIC ||
               mCurrentOperation == CmsOperation.SYNC_FOR_DATA_CONNECTION ||
               mCurrentOperation == CmsOperation.SYNC_FOR_USER_ACTIVITY){
                return false;
            }

            long now = System.currentTimeMillis();
            long delta = now - sEndOfLastSync;
            if(newOperation == CmsOperation.SYNC_FOR_DATA_CONNECTION &&
                    (delta < mDataConnectionInterval)){
                if(sLogger.isActivated()){
                    sLogger.debug("DataConnection event not taken into account");
                    sLogger.debug("Data connection interval : " + mDataConnectionInterval);
                    sLogger.debug("Last sync : " + sEndOfLastSync);
                    sLogger.debug("Now : " + now);
                }
                return false;
            }
        }else if (mCurrentOperation == newOperation){
            return false;
        }
        return true;
    }

    private synchronized boolean schedule(CmsOperation newOperation, SyncParams parameters){

        if(!canScheduleNewOperation(newOperation)){
            if(sLogger.isActivated()){
                traceScheduler();
            }
            return false;
        }

        if(sLogger.isActivated()){
            sLogger.info("Schedule new operation with message store:");
            sLogger.info("--> " + newOperation.toString());
            sLogger.info("--> " + parameters.toString());
        }

        Message message;
        switch (newOperation) {
            case SYNC_PERIODIC:
                message = mSyncRequestHandler.obtainMessage(CmsOperation.SYNC_PERIODIC.toInt(),parameters);
                mSyncRequestHandler.sendMessageDelayed(message, mSyncTimerInterval );
                break;
            case SYNC_FOR_DATA_CONNECTION:
                    mSyncRequestHandler.removeMessages(CmsOperation.SYNC_PERIODIC.toInt());
                    message = mSyncRequestHandler.obtainMessage(CmsOperation.SYNC_FOR_DATA_CONNECTION.toInt(), parameters);
                    mSyncRequestHandler.sendMessage(message);
                break;
            case SYNC_FOR_USER_ACTIVITY:
                mSyncRequestHandler.removeMessages(CmsOperation.SYNC_PERIODIC.toInt());
                message = mSyncRequestHandler.obtainMessage(CmsOperation.SYNC_FOR_USER_ACTIVITY.toInt(), parameters);
                mSyncRequestHandler.sendMessage(message);
                break;
            case PUSH_MESSAGES:
                message = mSyncRequestHandler.obtainMessage(CmsOperation.PUSH_MESSAGES.toInt(), parameters);
                mSyncRequestHandler.sendMessage(message);
                break;
            case UPDATE_FLAGS:
                message = mSyncRequestHandler.obtainMessage(CmsOperation.UPDATE_FLAGS.toInt(), parameters);
                mSyncRequestHandler.sendMessage(message);
                break;
        }
        return true;
    }


    /**
     * Handler used for sync request
     */
    /* package private */ class SyncRequestHandler extends Handler {

        SyncRequestHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mCurrentOperation = CmsOperation.valueOf(msg.what);
            SyncParams syncParams = null;
            boolean result = false;

            try {
                BasicImapService basicImapService = mImapServiceHandler.openService();
                if(mCurrentOperation == CmsOperation.SYNC_PERIODIC ||
                        mCurrentOperation == CmsOperation.SYNC_FOR_DATA_CONNECTION ||
                        mCurrentOperation == CmsOperation.SYNC_FOR_USER_ACTIVITY){
                    syncParams = (SyncParams)msg.obj;
                    executeSync(basicImapService, syncParams);
                    result = true;
                }

                else if (mCurrentOperation == CmsOperation.PUSH_MESSAGES){
                    syncParams = (SyncParams)msg.obj;
                    executePush(basicImapService, (ContactId)syncParams.mParam);
                    result = true;
                }

                else if (mCurrentOperation == CmsOperation.UPDATE_FLAGS){
                    executeUpdate(basicImapService);
                    result = true;
                }

                else if (mCurrentOperation == CmsOperation.TOOLKIT_TASK){ //TODO FGI : to be removed, used by Cms Toolkit only
                    executeCmsTask(basicImapService, (CmsTask) msg.obj);
                    result = true;
                }

            } catch (NetworkException | PayloadException e) {
                if(sLogger.isActivated()){
                    sLogger.debug("Failed to sync : " + e);
                }
            }
            finally {
                try {
                    mImapServiceHandler.closeService();
                } catch (NetworkException |PayloadException e) {
                    if(sLogger.isActivated()){
                        sLogger.debug("Failed to sync : " + e);
                    }
                }
            }

            // notify listeners
            Set<CmsOperationListener> listeners = mListeners.get(mCurrentOperation);
            if(listeners != null){
                for(CmsOperationListener listener : listeners){
                    SyncType syncType = (syncParams == null) ? SyncType.UNSPECIFIED : syncParams.mSyncType;
                    Object param = (syncParams == null) ? null : syncParams.mParam;
                    listener.onCmsOperationExecuted(mCurrentOperation,syncType, result, param);
                }
            }

            mCurrentOperation = null;
            if(msg.what == 5){ // TODO FGI : to be removed, used only for CMS task
                return;
            }

            sEndOfLastSync = System.currentTimeMillis();
            // schedule new periodic sync on all conversations
            schedule(CmsOperation.SYNC_PERIODIC, new SyncParams<>(SyncType.ALL, null, mSyncTimerInterval));
        }
    }

    void executeSync(BasicImapService basicImapService, SyncParams syncParams){

        String remoteFolder = null;
        if(syncParams.mSyncType == SyncType.ONE_TO_ONE){
            remoteFolder = CmsUtils.contactToCmsFolder(mRcsSettings, (ContactId)syncParams.mParam);
        }
        else if (syncParams.mSyncType == SyncType.GROUP){
            remoteFolder = CmsUtils.groupChatToCmsFolder(mRcsSettings, (String)syncParams.mParam, (String)syncParams.mParam);
        }

        try {
            Synchronizer synchronizer = new Synchronizer(mContext, mRcsSettings, mLocalStorage, basicImapService);
            switch (syncParams.mSyncType){
                case ONE_TO_ONE:
                case GROUP:
                    synchronizer.syncFolder(remoteFolder);
                    break;
                case ALL:
                    synchronizer.syncAll();
                    break;
            }
        } catch (PayloadException | NetworkException | FileAccessException
                | RuntimeException e) {
            sLogger.error("Failed to sync with message store", e);
        }
    }

    void executePush(BasicImapService basicImapService, ContactId contact){
        PushMessageTask task = new PushMessageTask(mContext, mRcsSettings, mXmsLog, mImapLog, contact, this);
        task.setBasicImapService(basicImapService);
        task.run();
    }

    void executeUpdate(BasicImapService basicImapService){
        UpdateFlagTask task = new UpdateFlagTask(mImapLog, this);
        task.setBasicImapService(basicImapService);
        task.run();
    }

    void executeCmsTask(BasicImapService basicImapService, CmsTask cmsTask){
        cmsTask.setBasicImapService(basicImapService);
        cmsTask.run();
    }

    class SyncParams<T> {

        final SyncType mSyncType;
        final T mParam;
        long mDelay; // delay before executing operation in ms

        SyncParams(SyncType syncType, T param, long delay){
            mSyncType = syncType;
            mParam = param;
            mDelay = delay;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SyncParams{");
            sb.append("mSyncType=").append(mSyncType);
            sb.append(", mParam=").append(mParam);
            sb.append(", mDelay=").append(mDelay);
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
            mImapLog.updateXmsPushStatus(uid, baseId, PushStatus.PUSHED);
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
                    mImapLog.updateDeleteStatus(folderName, uid, DeleteStatus.DELETED);
                } else if (seen) {
                    mImapLog.updateReadStatus(folderName, uid, ReadStatus.READ);
                }
            }
        }
    }

    private void traceScheduler(){
        sLogger.debug(" >>> Scheduler state : ");
        sLogger.debug("     --> CurrentOperation : " + (mCurrentOperation==null ? "null" : mCurrentOperation));
        if( mSyncRequestHandler.hasMessages(CmsOperation.SYNC_PERIODIC.toInt())){
            sLogger.debug("     --> has messages of type " + CmsOperation.SYNC_PERIODIC);
        }
        if( mSyncRequestHandler.hasMessages(CmsOperation.SYNC_FOR_DATA_CONNECTION.toInt())){
            sLogger.debug("     --> has messages of type " + CmsOperation.SYNC_FOR_DATA_CONNECTION);
        }
        if( mSyncRequestHandler.hasMessages(CmsOperation.SYNC_FOR_USER_ACTIVITY.toInt())){
            sLogger.debug("     --> has messages of type " + CmsOperation.SYNC_FOR_USER_ACTIVITY);
        }
        if( mSyncRequestHandler.hasMessages(CmsOperation.PUSH_MESSAGES.toInt())){
            sLogger.debug("     --> has messages of type " + CmsOperation.PUSH_MESSAGES);
        }
        if( mSyncRequestHandler.hasMessages(CmsOperation.UPDATE_FLAGS.toInt())){
            sLogger.debug("     --> has messages of type " + CmsOperation.UPDATE_FLAGS);
        }
        sLogger.debug(" <<< Scheduler state : ");
    }
}

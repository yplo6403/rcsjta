package com.gsma.rcs.cms.fordemo;


import com.gsma.rcs.cms.event.ILocalEventHandler;
import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.event.RcsXmsEventListener;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.imap.task.PushMessageTask.PushMessageTaskListener;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask.UpdateFlagTaskListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;

import java.util.Iterator;
import java.util.List;


/**
 * This class should be removed.
 * Should be used for testing purpose only
 * It creates SMS messages on the CMS server with IMAP command.
 *
 */
public class ImapCommandController  implements INativeXmsEventListener, RcsXmsEventListener, PushMessageTaskListener, UpdateFlagTaskListener {

    private static final Logger sLogger = Logger.getLogger(ImapCommandController.class.getSimpleName());

    private static ImapCommandController sInstance = null;

    private CmsSettings mSettings;
    private LocalStorage mLocalStorage;
    private XmsLog mXmsLog;
    private PartLog mPartLog;
    private ImapLog mImapLog;

    private ImapContext mImapContext;

    /**
     * @param context
     * @param settings
     * @param localStorage
     * @param imapLog
     * @param xmsLog
     * @return ImapCommandController instance
     */
    public static ImapCommandController createInstance(
            Context context,
            CmsSettings settings,
            LocalStorage localStorage,
            ImapLog imapLog,
            XmsLog xmsLog,
            PartLog partLog){
        if(sInstance == null){
            sInstance = new ImapCommandController(settings, localStorage, imapLog, xmsLog, partLog);
        }
        return sInstance;
    }

    /**
     * @return ImapCommandController instance
     */
    public static ImapCommandController getInstance(){
        return sInstance;
    }


    private ImapCommandController(CmsSettings settings,LocalStorage localStorage, ImapLog imapLog, XmsLog xmsLog, PartLog partLog){
        mSettings = settings;
        mLocalStorage = localStorage;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
        mPartLog = partLog;
        mImapContext = new ImapContext();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onIncomingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.info("onIncomingSms");
        }
        try {
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mPartLog, mSettings.getMyNumber(), new ImapContext(), this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public void onOutgoingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.info("onOutgoingSms");
        }
        try {
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mPartLog, mSettings.getMyNumber(), new ImapContext(), this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }
    }

    @Override
    public void onDeliverNativeSms(long nativeProviderId, long sentDate) {

    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        boolean isLogActivated = sLogger.isActivated();
        if(isLogActivated){
            sLogger.info("onDeleteNativeSms");
        }

        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onIncomingMms(MmsData message) {
        if(sLogger.isActivated()){
            sLogger.info("onIncomingMms");
        }
        try {
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mPartLog, mSettings.getMyNumber(), new ImapContext(), this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }
    }

    @Override
    public void onOutgoingMms(MmsData message) {
        if(sLogger.isActivated()){
            sLogger.info("onOutgoingMms");
        }
        try {
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mPartLog, mSettings.getMyNumber(), new ImapContext(), this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }
    }

    @Override
    public void onDeleteNativeMms(String mmsId) {
        boolean isLogActivated = sLogger.isActivated();
        if(isLogActivated){
            sLogger.info("onDeleteNativeMms");
        }

        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onReadNativeConversation(long nativeThreadId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadNativeConversation");
        }

        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteNativeConversation(long nativeThreadId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onDeleteNativeConversation : " + nativeThreadId);
        }

        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @SuppressWarnings("unchecked")
    private void updateFlags(){
        try {
            new UpdateFlagTask(ImapServiceManager.getService(mSettings), mSettings, mXmsLog, mImapLog, mImapContext, this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }
    }

    @Override
    public void onReadRcsConversation(String contact) {
        boolean isLogActivated = sLogger.isActivated();
        if(isLogActivated){
            sLogger.info("onReadRcsConversation : ".concat(contact));
        }

        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteRcsSms(String contact, String baseId) {
        boolean isLogActivated = sLogger.isActivated();
        if(sLogger.isActivated()){
            sLogger.info(new StringBuilder("onDeleteRcsSms :").append(contact).append(",").append(baseId).toString());
        }
        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteRcsMms(String contact, String baseId, String mms_id) {
        boolean isLogActivated = sLogger.isActivated();
        if(sLogger.isActivated()){
            sLogger.info(new StringBuilder("onDeleteRcsMms :").append(contact).append(",").append(mms_id).toString());
        }
        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteRcsConversation(String contact) {
        boolean isLogActivated = sLogger.isActivated();
        if(sLogger.isActivated()){
        sLogger.info("onDeleteRcsConversation :".concat(contact));
        }
        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onReadRcsMessage(String contact, String baseId) {
        boolean isLogActivated = sLogger.isActivated();
        if(isLogActivated){
            sLogger.info(new StringBuilder("onReadRcsMessage : ").append(contact).append(",").append(baseId).toString());
        }
        if(!mSettings.getUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onPushMessageTaskCallbackExecuted(ImapContext localContext,  Boolean result) {
        if(sLogger.isActivated()){
            sLogger.info("onPushMessageTaskCallbackExecuted");
        }

        for(String baseId : localContext.getUids().keySet()){
            mXmsLog.updatePushStatus(baseId, XmsData.PushStatus.PUSHED);
        }
        mImapContext.importLocalContext(localContext);
    }

    @Override
    public void onUpdateFlagTaskExecuted(ImapContext localContext, List<FlagChange> successFullFlagChanges) {
        if(sLogger.isActivated()){
            sLogger.info("onUpdateFlagTaskExecuted");
        }

        if(successFullFlagChanges==null){
            return;
        }

        Iterator<FlagChange> iter = successFullFlagChanges.iterator();
        while (iter.hasNext()) {
            FlagChange fg = iter.next();
            boolean deleted = fg.addDeletedFlag();
            boolean seen = fg.addSeenFlag();
            String folderName = fg.getFolder();
            for(Integer uid : fg.getUids()){
                String baseId = localContext.getBaseId(folderName, uid);
                MessageData msg = mImapLog.getMessage(fg.getFolder(), uid);
                if(deleted){
                    mXmsLog.deleteMessage(baseId);
                }
                else if(seen){
                    mXmsLog.updateReadStatusdWithBaseId(baseId, XmsData.ReadStatus.READ);
                }
            }
        }
        mImapContext.importLocalContext(localContext);
    }

    public ImapContext getContext(){
        return mImapContext;
    }
}

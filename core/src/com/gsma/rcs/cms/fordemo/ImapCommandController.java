package com.gsma.rcs.cms.fordemo;


import android.content.Context;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.event.IRcsXmsEventListener;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.imap.task.PushMessageTask.PushMessageTaskListener;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask.UpdateFlagTaskListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * This class should be removed.
 * Should be used for testing purpose only
 * It creates SMS messages on the CMS server with IMAP command.
 *
 */
public class ImapCommandController  implements INativeXmsEventListener, IRcsXmsEventListener, PushMessageTaskListener, UpdateFlagTaskListener {

    private static final Logger sLogger = Logger.getLogger(ImapCommandController.class.getSimpleName());
    private final Context mContext;
    private final RcsSettings mSettings;
    private final LocalStorage mLocalStorage;
    private final XmsLog mXmsLog;
    private final ImapLog mImapLog;

    public ImapCommandController(Context context, RcsSettings settings,LocalStorage localStorage, ImapLog imapLog, XmsLog xmsLog){
        mContext = context;
        mSettings = settings;
        mLocalStorage = localStorage;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onIncomingSms(SmsDataObject message) {
        if(sLogger.isActivated()){
            sLogger.info("onIncomingSms");
        }
        try {
            new PushMessageTask(mContext, mSettings, ImapServiceManager.getService(mSettings), mXmsLog, mImapLog, this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public void onOutgoingSms(SmsDataObject message) {
        if(sLogger.isActivated()){
            sLogger.info("onOutgoingSms");
        }
        try {
            new PushMessageTask(mContext, mSettings, ImapServiceManager.getService(mSettings), mXmsLog, mImapLog, this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        boolean isLogActivated = sLogger.isActivated();
        if(isLogActivated){
            sLogger.info("onDeleteNativeSms");
        }

        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        if(sLogger.isActivated()){
            sLogger.info("onIncomingMms");
        }
        try {
            new PushMessageTask(mContext, mSettings, ImapServiceManager.getService(mSettings), mXmsLog, mImapLog, this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) {
        if(sLogger.isActivated()){
            sLogger.info("onOutgoingMms");
        }
        try {
            new PushMessageTask(mContext ,mSettings, ImapServiceManager.getService(mSettings), mXmsLog, mImapLog,this).execute();
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

        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onMessageStateChanged(Long nativeProviderId, String mimeType, State state) {

    }

    @Override
    public void onReadNativeConversation(long nativeThreadId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadNativeConversation");
        }

        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
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

        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
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
            new UpdateFlagTask(ImapServiceManager.getService(mSettings), mSettings, mXmsLog, mImapLog, this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }
    }

    @Override
    public void onReadRcsMessage(String messageId) {
        boolean isLogActivated = sLogger.isActivated();
        if(isLogActivated){
            sLogger.info(new StringBuilder("onReadRcsMessage : ").append(messageId).toString());
        }
        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onReadRcsConversation(ContactId contactId) {
        boolean isLogActivated = sLogger.isActivated();
        if(isLogActivated){
            sLogger.info("onReadRcsConversation : ".concat(contactId.toString()));
        }

        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteRcsMessage(String messageId) {
        boolean isLogActivated = sLogger.isActivated();
        if(sLogger.isActivated()){
            sLogger.info(new StringBuilder("onDeleteRcsMessage :").append(messageId).toString());
        }
        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteRcsConversation(ContactId contactId) {
        boolean isLogActivated = sLogger.isActivated();
        if(sLogger.isActivated()){
        sLogger.info("onDeleteRcsConversation :".concat(contactId.toString()));
        }
        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onMessageStateChanged(ContactId contact, String messageId, String mimeType, State state) {

    }

    @Override
    public void onDeleteAll() {
        boolean isLogActivated = sLogger.isActivated();
        if(sLogger.isActivated()){
            sLogger.info("onDeleteAll");
        }
        if(!mSettings.getCmsUpdateFlagsWithImapXms()){
            if(isLogActivated){
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onPushMessageTaskCallbackExecuted(Map<String,Integer> uidsMap) {
        if(sLogger.isActivated()){
            sLogger.info("onPushMessageTaskCallbackExecuted");
        }
        if(uidsMap==null){
            return;
        }
        Iterator<Entry<String,Integer>> iter = uidsMap.entrySet().iterator();
        while(iter.hasNext()){
            Entry<String,Integer> entry = iter.next();
            String baseId = entry.getKey();
            Integer uid = entry.getValue();
            mImapLog.updateXmsPushStatus(uid, baseId, PushStatus.PUSHED);
        }
    }

    @Override
    public void onUpdateFlagTaskExecuted( List<FlagChange> successFullFlagChanges) {
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
                if(deleted){
                    mImapLog.updateDeleteStatus(folderName, uid, DeleteStatus.DELETED);
                }
                else if(seen){
                    mImapLog.updateReadStatus(folderName, uid, ReadStatus.READ);
                }
            }
        }
    }

}

package com.gsma.rcs.cms.fordemo;


import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.event.RcsXmsEventListener;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.imap.task.PushMessageTask.PushMessageTaskListener;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask.UpdateFlagTaskListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.cms.sync.strategy.FlagChange.Operation;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.Flag;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private XmsLog mXmsLog;
    private PartLog mPartLog;
    private ImapLog mImapLog;

    // key base id, value uid from cms server
    private Map<String,Integer> mUids = new HashMap<String,Integer>();

    /**
     * @param context
     * @param settings
     * @param imapLog
     * @param xmsLog
     * @return ImapCommandController instance
     */
    public static ImapCommandController createInstance(Context context, CmsSettings settings, ImapLog imapLog, XmsLog xmsLog, PartLog partLog){
        if(sInstance == null){
            sInstance = new ImapCommandController(settings, imapLog, xmsLog, partLog);
        }
        return sInstance;
    }

    /**
     * @return ImapCommandController instance
     */
    public static ImapCommandController getInstance(){
        return sInstance;
    }


    private ImapCommandController(CmsSettings settings, ImapLog imapLog, XmsLog xmsLog, PartLog partLog){
        mSettings = settings;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
        mPartLog = partLog;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onIncomingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.info("onIncomingSms");
        }
        try {
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mPartLog, mSettings.getMyNumber(), this).execute();
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
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mPartLog, mSettings.getMyNumber(), this).execute();
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

        XmsData xmsData = mXmsLog.getMessageByNativeProviderId(XmsData.MimeType.SMS, nativeProviderId);
        if(xmsData==null){
            return;
        }
        String folderName = Constants.TEL_PREFIX.concat(xmsData.getContact());
        Integer uid = mUids.get(xmsData.getBaseId());
        if(uid==null){
            uid = mImapLog.getUid(folderName, xmsData.getBaseId());
            if(uid==null){
                return;
            }
        }
        updateFlag(new FlagChange(folderName, uid, Flag.Deleted,  Operation.ADD_FLAG));
    }

    @Override
    public void onIncomingMms(MmsData message) {
        if(sLogger.isActivated()){
            sLogger.info("onIncomingMms");
        }
        try {
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mPartLog, mSettings.getMyNumber(), this).execute();
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
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mPartLog, mSettings.getMyNumber(), this).execute();
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

        XmsData xmsData = mXmsLog.getMessage(mmsId);
        if(xmsData==null){
            return;
        }
        String folderName = Constants.TEL_PREFIX.concat(xmsData.getContact());
        Integer uid = mUids.get(xmsData.getBaseId());
        if(uid==null){
            uid = mImapLog.getUid(folderName, xmsData.getBaseId());
            if(uid==null){
                return;
            }
        }
        updateFlag(new FlagChange(folderName, uid, Flag.Deleted,  Operation.ADD_FLAG));
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

        List<FlagChange> flagChanges = new ArrayList<FlagChange>();
        for (XmsData xmsData : mXmsLog.getMessages(nativeThreadId, ReadStatus.READ_REQUESTED)) {
            Integer uid = mUids.get(xmsData.getBaseId());
            String folderName = Constants.TEL_PREFIX.concat(xmsData.getContact());
            if (uid == null) {
                uid = mImapLog.getUid(folderName, xmsData.getBaseId());
                if (uid == null) {
                    continue;
                }
            }
            flagChanges.add(new FlagChange(folderName, uid, Flag.Seen, Operation.ADD_FLAG));
        }
        updateFlag(flagChanges);
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

        List<FlagChange> flagChanges = new ArrayList<FlagChange>();
        for(XmsData xmsData : mXmsLog.getMessages(nativeThreadId, DeleteStatus.DELETED_REQUESTED)){
            String folderName = Constants.TEL_PREFIX.concat(xmsData.getContact());
            Integer uid = mImapLog.getUid(folderName, xmsData.getBaseId());
            if(uid==null){
                uid = mUids.get(xmsData.getBaseId());
                if(uid==null){
                    continue;
                }
            }
            flagChanges.add(new FlagChange(folderName, uid, Flag.Deleted,  Operation.ADD_FLAG));
        }
        updateFlag(flagChanges);
    }

    @SuppressWarnings("unchecked")
    private void updateFlag(FlagChange flagChange){
        updateFlag(Arrays.asList(flagChange));
    }

    @SuppressWarnings("unchecked")
    private void updateFlag(List<FlagChange> flagChanges){
        if(flagChanges.isEmpty()){
            return;
        }

        try {
            new UpdateFlagTask(ImapServiceManager.getService(mSettings), flagChanges, this).execute();
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
       List<FlagChange> flagChanges = new ArrayList<FlagChange>();
       for(XmsData xmsData : mXmsLog.getMessages(contact, ReadStatus.READ_REQUESTED)){
           Integer uid = mUids.get(xmsData.getBaseId());
           String folderName = Constants.TEL_PREFIX.concat(contact);
           if(uid==null){
               uid = mImapLog.getUid(folderName, xmsData.getBaseId());
               if(uid==null){
                   continue;
               }
           }
           flagChanges.add(new FlagChange(folderName, uid, Flag.Seen,  Operation.ADD_FLAG));
       }
       updateFlag(flagChanges);
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
        Integer uid = mUids.get(baseId);
        String folderName = Constants.TEL_PREFIX.concat(contact);
        if(uid == null){
            uid = mImapLog.getUid(folderName, baseId);
            if(uid==null){
                return;
            }
        }
        updateFlag(new FlagChange(folderName, uid, Flag.Deleted,  Operation.ADD_FLAG));
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
        Integer uid = mUids.get(baseId);
        String folderName = Constants.TEL_PREFIX.concat(contact);
        if(uid == null){
            uid = mImapLog.getUid(folderName, baseId);
            if(uid==null){
                return;
            }
        }
        updateFlag(new FlagChange(folderName, uid, Flag.Deleted,  Operation.ADD_FLAG));
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
        List<FlagChange> flagChanges = new ArrayList<FlagChange>();
        for(XmsData xmsData : mXmsLog.getMessages(contact, DeleteStatus.DELETED_REQUESTED)){
            Integer uid = mUids.get(xmsData.getBaseId());
            String folderName = Constants.TEL_PREFIX.concat(contact);
            if(uid == null){
                uid = mImapLog.getUid(folderName, xmsData.getBaseId());
                if(uid==null){
                    continue;
                }
            }
            flagChanges.add(new FlagChange(folderName, uid, Flag.Deleted,  Operation.ADD_FLAG));
        }
        updateFlag(flagChanges);
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
        String folderName = Constants.TEL_PREFIX.concat(contact);
        Integer uid = mUids.get(baseId);
        if (uid == null) {
            uid = mImapLog.getUid(folderName, baseId);
            if(uid==null){
                return;
            }
        }
        updateFlag(new FlagChange(folderName, uid, Flag.Seen, Operation.ADD_FLAG));
    }

    @Override
    public void onPushMessageTaskCallbackExecuted(PushMessageTask.PushMessageResult result) {
        if(sLogger.isActivated()){
            sLogger.info("onPushMessageTaskCallbackExecuted");
        }
        if(result!=null){
            mUids.putAll(result.mUids);
        }
    }

    @Override
    public void onUpdateFlagTaskExecuted(String[] params, Boolean result) {
        if(sLogger.isActivated()){
            sLogger.info("onUpdateFlagTaskExecuted");
        }
    }
}

package com.gsma.rcs.cms.fordemo;


import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.INativeSmsEventListener;
import com.gsma.rcs.cms.event.IRcsSmsEventListener;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.imap.task.PushMessageTask.PushMessageTaskListener;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask.UpdateFlagTaskListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
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
public class ImapCommandController  implements INativeSmsEventListener, IRcsSmsEventListener, PushMessageTaskListener, UpdateFlagTaskListener {

    private static final Logger sLogger = Logger.getLogger(ImapCommandController.class.getSimpleName());
    
    private static ImapCommandController sInstance = null;
    
    private CmsSettings mSettings;
    private Context mContext;    
    private XmsLog mXmsLog;
    private ImapLog mImapLog;
    
    // key native provider id, value uid from cms server
    private Map<Long,Integer> mUids = new HashMap<Long,Integer>();
   
    /**
     * @param context
     * @param settings
     * @param imapLog
     * @param xmsLog
     * @return ImapCommandController instance
     */
    public static ImapCommandController createInstance(Context context, CmsSettings settings, ImapLog imapLog, XmsLog xmsLog){
        if(sInstance == null){
            sInstance = new ImapCommandController(context, settings, imapLog, xmsLog);
        }
        return sInstance;
    }

    /**
     * @return ImapCommandController instance
     */
    public static ImapCommandController getInstance(){
        return sInstance;
    }


    private ImapCommandController(Context context, CmsSettings settings, ImapLog imapLog, XmsLog xmsLog){
        mSettings = settings; 
        mImapLog = imapLog;
        mXmsLog = xmsLog;   
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onIncomingSms(SmsData message) {
        if(sLogger.isActivated()){
            sLogger.info("onIncomingSms");    
        }       
        
        try {            
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mSettings.getMyNumber(), this).execute();
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
            new PushMessageTask(ImapServiceManager.getService(mSettings), mXmsLog, mSettings.getMyNumber(), this).execute();
        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn(e.getMessage());
        }

    }
    
    @Override
    public void onDeliverNativeSms(long nativeProviderId, long sentDate) {
        
    }

    @Override
    public void onReadNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.info("onReadNativeSms");    
        }        
        SmsData smsData = mXmsLog.getMessageByNativeProviderId(nativeProviderId);
        if(smsData==null){
            return;
        }
        String folderName = Constants.TEL_PREFIX.concat(smsData.getContact());
        Integer uid = mUids.get(nativeProviderId);        
        if(uid==null){   
            uid = mImapLog.getUid(folderName, smsData.getBaseId());
            if(uid==null){
                return;
            }                
        }       
        updateFlag(new FlagChange(folderName, uid, Flag.Seen,  Operation.ADD_FLAG));
    }

    @Override
    public void onDeleteNativeSms(long nativeProviderId) {
        if(sLogger.isActivated()){
            sLogger.info("onDeleteNativeSms");    
        }
        SmsData smsData = mXmsLog.getMessageByNativeProviderId(nativeProviderId);
        if(smsData==null){
            return;
        }
        String folderName = Constants.TEL_PREFIX.concat(smsData.getContact());
        Integer uid = mUids.get(nativeProviderId);        
        if(uid==null){   
            uid = mImapLog.getUid(folderName, smsData.getBaseId());
            if(uid==null){
                return;
            }                
        }       
        updateFlag(new FlagChange(folderName, uid, Flag.Deleted,  Operation.ADD_FLAG));
    }
    
    @Override
    public void onDeleteNativeConversation(String contact) {
        if(sLogger.isActivated()){
            sLogger.info("onDeleteNativeConversation : to be implemented");    
        }
        List<FlagChange> flagChanges = new ArrayList<FlagChange>();
        for(SmsData smsData : mXmsLog.getMessages(contact, DeleteStatus.DELETED_REQUESTED)){
            String folderName = Constants.TEL_PREFIX.concat(contact);
            Integer uid = mImapLog.getUid(Constants.TEL_PREFIX.concat(contact), smsData.getBaseId());
            if(uid==null){
                uid = mUids.get(smsData.getNativeProviderId());
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
        if(sLogger.isActivated()){
            sLogger.info("onReadRcsConversation : ".concat(contact));    
        }
        
       List<FlagChange> flagChanges = new ArrayList<FlagChange>();
       for(SmsData smsData : mXmsLog.getMessages(contact, ReadStatus.READ_REQUESTED)){
           Integer uid = mUids.get(smsData.getNativeProviderId());
           String folderName = Constants.TEL_PREFIX.concat(contact);
           if(uid==null){
               uid = mImapLog.getUid(folderName, smsData.getBaseId());
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
        if(sLogger.isActivated()){
            sLogger.info(new StringBuilder("onDeleteRcsSms :").append(contact).append(",").append(baseId).toString());    
        }
        Integer uid = mUids.get(mXmsLog.getNativeProviderId(baseId));
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
        if(sLogger.isActivated()){
            sLogger.info("onDeleteRcsConversation :".concat(contact));    
        }
        List<FlagChange> flagChanges = new ArrayList<FlagChange>();
        for(SmsData smsData : mXmsLog.getMessages(contact, DeleteStatus.DELETED_REQUESTED)){            
            Integer uid = mUids.get(smsData.getNativeProviderId());
            String folderName = Constants.TEL_PREFIX.concat(contact);
            if(uid == null){
                uid = mImapLog.getUid(folderName, smsData.getBaseId());
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
        
        new AsyncTask<String,String,Boolean>(){
            @Override
            protected Boolean doInBackground(String... params) {                
                String contact = params[0]; 
                String baseId = params[1];                
                if(sLogger.isActivated()){
                    sLogger.info(new StringBuilder("onReadRcsMessage : ").append(contact).append(",").append(baseId).toString());
                }
                String folderName = Constants.TEL_PREFIX.concat(contact);
                Integer uid = mUids.get(mXmsLog.getNativeProviderId(baseId));
                if(uid == null){
                    uid = mImapLog.getUid(folderName, baseId);
                    if(uid==null){
                        return false;
                    }
                }        
                updateFlag(new FlagChange(folderName, uid, Flag.Seen, Operation.ADD_FLAG));
                return true;
            }            
        }.execute(contact, baseId);
    }



    @Override
    public void onPushMessageTaskCallbackExecuted(Map<Long, Integer> result) {
        if(sLogger.isActivated()){
            sLogger.info("onPushMessageTaskCallbackExecuted");    
        }
        if(result!=null){
            mUids.putAll(result);            
        }
    }

    @Override
    public void onUpdateFlagTaskExecuted(String[] params, Boolean result) {
        if(sLogger.isActivated()){
            sLogger.info("onUpdateFlagTaskExecuted");    
        }
    }
}

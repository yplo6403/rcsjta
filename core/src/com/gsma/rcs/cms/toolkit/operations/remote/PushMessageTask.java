package com.gsma.rcs.cms.toolkit.operations.remote;

import android.os.AsyncTask;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PushMessageTask extends AsyncTask<String, String, List<String>> {

    private CmsSettings mCmsSettings;
    private PushMessageTaskCallback mCallback;
    private BasicImapService mImapService;
    private String mMyNumber;
    private SmsData[] mMessages;
    private List<Flag> mFlags;
    
    /**
     * @param cmsSettings 
     * @param messages 
     * @param myNumber
     * @param flags 
     * @param callback
     */
    public PushMessageTask(CmsSettings cmsSettings, SmsData[] messages, String myNumber,List<Flag> flags, PushMessageTaskCallback callback) {   
        mCmsSettings = cmsSettings;
        mMyNumber = myNumber;
        mCallback = callback;
        mMessages = messages;
        mFlags = flags;
    }

    @Override
    protected List<String> doInBackground(String... params) {
        
        Thread currentThread = Thread.currentThread();
        String currentName = currentThread.getName();
        currentThread.setName(BasicSynchronizationTask.class.getSimpleName());

        try {
            mImapService = ImapServiceManager.getService(mCmsSettings);
            mImapService.init();
            return pushMessages(mMessages);
        } catch (Exception e) {
            e.printStackTrace();
        } 
        finally {
            ImapServiceManager.releaseService(mImapService);
            Thread.currentThread().setName(currentName);
        }            
        return null;
    }

    /**
     * @param messages
     * @throws ImapException
     * @throws IOException
     */
    public List<String> pushMessages(SmsData[] messages) {
        String from, to, direction;
        from = to = direction = null;

        List<String> createdUids = new ArrayList<String>();
        try {
            List<String> existingFolders = new ArrayList<String>();
            for (ImapFolder imapFolder : mImapService.listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            for (SmsData message : messages) {
                switch (message.getDirection()) {
                    case INCOMING:
                        from = CmsUtils.contactToHeader(message.getContact());
                        to = CmsUtils.contactToHeader(mMyNumber);
                        direction = Constants.DIRECTION_RECEIVED;
                        break;
                    case OUTGOING:
                        from = CmsUtils.contactToHeader(mMyNumber);
                        to = CmsUtils.contactToHeader(message.getContact());
                        direction = Constants.DIRECTION_SENT;
                        break;
                    default:
                        break;
                }

                ImapSmsMessage imapSmsMessage = new ImapSmsMessage(from, to, direction,
                        message.getDate(), message.getContent(), "" + message.getDate(),
                        "" + message.getDate(), "" + message.getDate());
                
                String folder = CmsUtils.contactToCmsFolder(CmsSettings.getInstance(), message.getContact());
                if (!existingFolders.contains(folder)) {
                    mImapService.create(folder);
                    existingFolders.add(folder);
                }
                mImapService.selectCondstore(folder);
                int uid = mImapService.append(folder, mFlags,
                        imapSmsMessage.getPart());
                createdUids.add(String.valueOf(uid));
            }            
        } catch (IOException | ImapException e) {
            e.printStackTrace();
        }
        return createdUids;
    }

    @Override
    protected void onPostExecute(List<String> result) {
        if (mCallback != null) {
            mCallback.onPushMessageTaskCallbackExecuted(result);
        }
    }
    
    /**
    *
    */
    public interface PushMessageTaskCallback {

        /**
         * @param messages
         * @param result
         */
        public void onPushMessageTaskCallbackExecuted(List<String> result);
    }

}

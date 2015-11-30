package com.gsma.rcs.cms.toolkit.operations.remote;

import android.os.AsyncTask;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.services.rcs.contact.ContactId;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PushMessageTask extends AsyncTask<String, String, List<String>> {

    private RcsSettings mRcsSettings;
    private PushMessageTaskCallback mCallback;
    private BasicImapService mImapService;
    private ContactId mMyNumber;
    private SmsDataObject[] mMessages;
    private List<Flag> mFlags;
    
    /**
     * @param rcsSettings
     * @param messages 
     * @param myNumber
     * @param flags 
     * @param callback
     */
    public PushMessageTask(RcsSettings rcsSettings, SmsDataObject[] messages, ContactId myNumber,List<Flag> flags, PushMessageTaskCallback callback) {
        mRcsSettings = rcsSettings;
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
            mImapService = ImapServiceManager.getService(mRcsSettings);
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
    public List<String> pushMessages(SmsDataObject[] messages) {
        String from, to, direction;
        from = to = direction = null;

        List<String> createdUids = new ArrayList<String>();
        try {
            List<String> existingFolders = new ArrayList<String>();
            for (ImapFolder imapFolder : mImapService.listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            for (SmsDataObject message : messages) {
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
                        message.getTimestamp(), message.getBody(), "" + message.getTimestamp(),
                        "" + message.getTimestamp(), "" + message.getTimestamp());
                
                String folder = CmsUtils.contactToCmsFolder(mRcsSettings, message.getContact());
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
         * @param result
         */
        void onPushMessageTaskCallbackExecuted(List<String> result);
    }

}

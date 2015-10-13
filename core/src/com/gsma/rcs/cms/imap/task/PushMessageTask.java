
package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.sms.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PushMessageTask extends AsyncTask<String, String, Map<Long, Integer>> {

    private static final Logger sLogger = Logger.getLogger(PushMessageTask.class.getSimpleName());

    /*package private*/ final PushMessageTaskListener mListener;
    /*package private*/ final BasicImapService mImapService;
    /*package private*/ final XmsLog mXmsLog;
    /*package private*/ final String mMyNumber;
    
    /**
     * @param imapService
     * @param xmsLog 
     * @param myNumber
     * @param listener 
     */
    public PushMessageTask(BasicImapService imapService, XmsLog xmsLog, String myNumber, PushMessageTaskListener listener) {
        mImapService = imapService;    
        mXmsLog = xmsLog;
        mMyNumber = myNumber;
        mListener = listener;
    }

    @Override
    protected Map<Long, Integer> doInBackground(String... params) {
        
        Thread currentThread = Thread.currentThread();
        String currentName = currentThread.getName();
        currentThread.setName(BasicSynchronizationTask.class.getSimpleName());

        List<SmsData> messages = mXmsLog.getMessages(PushStatus.PUSH_REQUESTED);
        if(messages.isEmpty()){
            if (sLogger.isActivated()) {
                sLogger.debug("no message to push");
            }
            return null;
        }
        
        try {
            mImapService.init();
            return pushMessages(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
        finally {
            Thread.currentThread().setName(currentName);
            ImapServiceManager.releaseService(mImapService);    
        }
    }

    /**
     * @param messages
     * @throws ImapException
     * @throws IOException
     */
    public Map<Long, Integer> pushMessages(List<SmsData> messages) {
        String from, to, direction;
        from = to = direction = null;

        Map<Long, Integer> createdUids = new HashMap<Long, Integer>();
        try {
            List<String> existingFolders = new ArrayList<String>();
            for (ImapFolder imapFolder : mImapService.listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            for (SmsData message : messages) {
                List<Flag> flags = new ArrayList<Flag>();
                switch (message.getDirection()) {
                    case INCOMING:
                        from = message.getContact();
                        to = mMyNumber;
                        direction = Constants.DIRECTION_RECEIVED;
                        break;
                    case OUTGOING:
                        from = mMyNumber;
                        to = message.getContact();
                        direction = Constants.DIRECTION_SENT;
                        break;
                    default:
                        break;
                }
                
                if (message.getReadStatus() != ReadStatus.UNREAD) {
                    flags.add(Flag.Seen);
                }

                ImapSmsMessage imapSmsMessage = new ImapSmsMessage(from, to, direction,
                        message.getDate(), message.getContent(), "" + message.getDate(),
                        "" + message.getDate(), "" + message.getDate());
                
                String remoteFolder = Constants.TEL_PREFIX.concat(message.getContact());
                if (!existingFolders.contains(remoteFolder)) {
                    mImapService.create(remoteFolder);
                    existingFolders.add(remoteFolder);
                }
                mImapService.selectCondstore(remoteFolder);
                int uid = mImapService.append(remoteFolder, flags,
                        imapSmsMessage.getPart());
                mXmsLog.updatePushStatus(message.getBaseId(), PushStatus.PUSHED);
                createdUids.put(message.getNativeProviderId(), uid);
            }            
        } catch (IOException | ImapException e) {
            e.printStackTrace();
        }
        return createdUids;
    }

    @Override
    protected void onPostExecute(Map<Long, Integer> result) {
        if (mListener != null) {
            mListener.onPushMessageTaskCallbackExecuted(result);
        }
    }

    /**
    *
    */
    public interface PushMessageTaskListener {

        /**
         * @param messages
         * @param result
         */
        public void onPushMessageTaskCallbackExecuted(Map<Long, Integer> result);
    }
}


package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.ImapMmsMessage;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.MmsPart;
import com.gsma.rcs.cms.provider.xms.model.XmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PushMessageTask extends AsyncTask<String, String, PushMessageTask.PushMessageResult> {

    private static final Logger sLogger = Logger.getLogger(PushMessageTask.class.getSimpleName());

    /*package private*/ final PushMessageTaskListener mListener;
    /*package private*/ final BasicImapService mImapService;
    /*package private*/ final XmsLog mXmsLog;
    /*package private*/ final PartLog mPartLog;
    /*package private*/ final String mMyNumber;

    public class PushMessageResult{

        public Map<String, Integer> mUids;

        public PushMessageResult(Map<String, Integer> uids){
            mUids = uids;
        }
    }

    /**
     * @param imapService
     * @param xmsLog 
     * @param myNumber
     * @param listener 
     */
    public PushMessageTask(BasicImapService imapService, XmsLog xmsLog, PartLog partLog, String myNumber, PushMessageTaskListener listener) {
        mImapService = imapService;    
        mXmsLog = xmsLog;
        mPartLog = partLog;
        mMyNumber = myNumber;
        mListener = listener;
    }

    @Override
    protected PushMessageResult doInBackground(String... params) {

        Thread currentThread = Thread.currentThread();
        String currentName = currentThread.getName();
        try {
            currentThread.setName(BasicSynchronizationTask.class.getSimpleName());
            List<XmsData> messages = mXmsLog.getMessages(PushStatus.PUSH_REQUESTED);
            if(messages.isEmpty()){
                if (sLogger.isActivated()) {
                    sLogger.debug("no message to push");
                }
                return null;
            }


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
    public PushMessageResult pushMessages(List<XmsData> messages) {
        String from, to, direction;
        from = to = direction = null;

        Map<String, Integer> createdUids = new HashMap<String, Integer>();
        try {
            List<String> existingFolders = new ArrayList<String>();
            for (ImapFolder imapFolder : mImapService.listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            for (XmsData message : messages) {
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

                boolean isSms = false;
                IImapMessage imapMessage = null;
                if(message instanceof SmsData){
                    isSms = true;
                    imapMessage = new ImapSmsMessage(
                            from,
                            to,
                            direction,
                            message.getDate(),
                            message.getContent(),
                            "" + message.getDate(),
                            "" + message.getDate(),
                            "" + message.getDate());
                }
                else if(message instanceof MmsData) {
                    MmsData mms = (MmsData) message;
                    imapMessage = new ImapMmsMessage(
                            from,
                            to,
                            direction,
                            mms.getDate(),
                            mms.getSubject(),
                            "" + mms.getDate(),
                            "" + mms.getDate(),
                            "" + message.getDate(),
                            mms.getMmsId(),
                            mPartLog.getParts(mms.getMmsId(), false));
                }
                String remoteFolder = Constants.TEL_PREFIX.concat(message.getContact());
                if (!existingFolders.contains(remoteFolder)) {
                    mImapService.create(remoteFolder);
                    existingFolders.add(remoteFolder);
                }
                mImapService.selectCondstore(remoteFolder);
                int uid = mImapService.append(remoteFolder, flags,
                        imapMessage.getPart());
                mXmsLog.updatePushStatus(message.getBaseId(), PushStatus.PUSHED);
                createdUids.put(message.getBaseId(),uid);
            }
        } catch (IOException | ImapException e) {
            e.printStackTrace();
        }
        return new PushMessageResult(createdUids);
    }

    @Override
    protected void onPostExecute(PushMessageResult result) {
        if (mListener != null) {
            mListener.onPushMessageTaskCallbackExecuted(result);
        }
    }

    /**
    *
    */
    public interface PushMessageTaskListener {

        /**
         * @param result
         */
        public void onPushMessageTaskCallbackExecuted(PushMessageResult result);
    }
}

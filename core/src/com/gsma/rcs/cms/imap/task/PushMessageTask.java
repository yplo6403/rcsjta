
package com.gsma.rcs.cms.imap.task;

import android.content.Context;
import android.os.AsyncTask;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.ImapMmsMessage;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObjectFactory;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PushMessageTask extends AsyncTask<String, String, Boolean> {

    private static final Logger sLogger = Logger.getLogger(PushMessageTask.class.getSimpleName());

    /*package private*/ final PushMessageTaskListener mListener;
    /*package private*/ final BasicImapService mImapService;
    /*package private*/ final RcsSettings mRcsSettings;
    /*package private*/ final Context mContext;
    /*package private*/ final XmsLog mXmsLog;
    /*package private*/ final ImapLog mImapLog;

    /*package private*/ final Map<String,Integer> mCreatedUidsMap = new HashMap<>();

    /**
     * @param imapService
     * @param xmsLog 
     * @param listener
     */
    public PushMessageTask(Context context, RcsSettings rcsSettings,  BasicImapService imapService, XmsLog xmsLog, ImapLog imapLog, PushMessageTaskListener listener) {
        mRcsSettings = rcsSettings;
        mContext = context;
        mImapService = imapService;
        mXmsLog = xmsLog;
        mImapLog = imapLog;
        mListener = listener;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        Thread currentThread = Thread.currentThread();
        String currentName = currentThread.getName();
        try {
            currentThread.setName(BasicSynchronizationTask.class.getSimpleName());

            List<XmsDataObject> messagesToPush = new ArrayList<>();
            for(MessageData messageData : mImapLog.getXmsMessages(PushStatus.PUSH_REQUESTED)){
                XmsDataObject xms = XmsDataObjectFactory.createXmsDataObject(mXmsLog, messageData.getMessageId());
                if(xms != null){
                    messagesToPush.add(xms);
                }
            }
            if(messagesToPush.isEmpty()){
                if (sLogger.isActivated()) {
                    sLogger.debug("no message to push");
                }
                return null;
            }
                mImapService.init();
                return pushMessages(messagesToPush);
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
    public Boolean pushMessages(List<XmsDataObject> messages) {
        String from, to, direction;
        from = to = direction = null;

        try {
            List<String> existingFolders = new ArrayList<String>();
            for (ImapFolder imapFolder : mImapService.listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            for (XmsDataObject message : messages) {
                List<Flag> flags = new ArrayList<Flag>();
                switch (message.getDirection()) {
                    case INCOMING:
                        from = CmsUtils.contactToHeader(message.getContact());
                        to = CmsUtils.contactToHeader(mRcsSettings.getUserProfileImsUserName());
                        direction = Constants.DIRECTION_RECEIVED;
                        break;
                    case OUTGOING:
                        from = CmsUtils.contactToHeader(mRcsSettings.getUserProfileImsUserName());
                        to = CmsUtils.contactToHeader(message.getContact());
                        direction = Constants.DIRECTION_SENT;
                        break;
                    default:
                        break;
                }
                if (message.getReadStatus() != ReadStatus.UNREAD) {
                    flags.add(Flag.Seen);
                }

                IImapMessage imapMessage = null;
                if(message instanceof SmsDataObject){
                    imapMessage = new ImapSmsMessage(
                            from,
                            to,
                            direction,
                            message.getTimestamp(),
                            message.getBody(),
                            "" + message.getTimestamp(),
                            "" + message.getTimestamp(),
                            "" + message.getTimestamp());
                }
                else if(message instanceof MmsDataObject) {
                    MmsDataObject mms = (MmsDataObject) message;
                    imapMessage = new ImapMmsMessage(
                            mContext,
                            from,
                            to,
                            direction,
                            mms.getTimestamp(),
                            "" + mms.getTimestamp(),
                            "" + mms.getTimestamp(),
                            "" + mms.getTimestamp(),
                            mms.getMmsId(),
                            mms.getMmsPart());
                }
                String remoteFolder = CmsUtils.contactToCmsFolder(mRcsSettings, message.getContact());
                if (!existingFolders.contains(remoteFolder)) {
                    mImapService.create(remoteFolder);
                    existingFolders.add(remoteFolder);
                }
                mImapService.selectCondstore(remoteFolder);
                int uid = mImapService.append(remoteFolder, flags,
                        imapMessage.getPart());
                mCreatedUidsMap.put(message.getMessageId(), uid);
            }
        } catch (IOException | ImapException e) {
            e.printStackTrace();
        }
        return true;
    }

    public Map<String,Integer> getCreatedUids(){
        return mCreatedUidsMap;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (mListener != null) {
            mListener.onPushMessageTaskCallbackExecuted(mCreatedUidsMap);
        }
    }

    /**
    *
    */
    public interface PushMessageTaskListener {

        /**
         * @param uidsMap
         */
        void onPushMessageTaskCallbackExecuted(Map<String,Integer> uidsMap);
    }
}

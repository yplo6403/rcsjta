
package com.gsma.rcs.cms.toolkit.operations.remote;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.BasicSynchronizationTask;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapService;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PushMessageTask extends AsyncTask<String, String, List<String>> {

    private final ImapServiceController mImapServiceController;
    private final RcsSettings mRcsSettings;
    private final PushMessageTaskCallback mCallback;
    private final ContactId mMyNumber;
    private final SmsDataObject[] mMessages;
    private final List<Flag> mFlags;

    private static final Logger sLogger = Logger.getLogger(PushMessageTask.class.getSimpleName());
    private BasicImapService mImapService;

    /**
     * @param rcsSettings
     * @param messages
     * @param myNumber
     * @param flags
     * @param callback
     */
    public PushMessageTask(ImapServiceController imapServiceController, RcsSettings rcsSettings,
            SmsDataObject[] messages, ContactId myNumber, List<Flag> flags,
            PushMessageTaskCallback callback) {
        mImapServiceController = imapServiceController;
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
            mImapService = mImapServiceController.createService();
            mImapService.init();
            return pushMessages(mMessages);

        } catch (ImapServiceNotAvailableException | IOException e) {
            if (sLogger.isActivated()) {
                sLogger.info("Failed to push messages! error=" + e.getMessage());
            }
        } catch (ImapException | RuntimeException e) {
            sLogger.error("Failed to push messages!", e);

        } finally {
            try {
                mImapServiceController.closeService();
            } catch (NetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.info("Failed to close CMS service! error=" + e.getMessage());
                }
            } catch (PayloadException | RuntimeException e) {
                sLogger.error("Failed to close CMS service", e);
            }
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
                        message.getTimestamp(), message.getBody(), "" + message.getTimestamp(), ""
                                + message.getTimestamp(), "" + message.getTimestamp());

                String folder = CmsUtils.contactToCmsFolder(mRcsSettings, message.getContact());
                if (!existingFolders.contains(folder)) {
                    mImapService.create(folder);
                    existingFolders.add(folder);
                }
                mImapService.selectCondstore(folder);
                int uid = mImapService.append(folder, mFlags, imapSmsMessage.toPayload());
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

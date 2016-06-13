/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.sync.scheduler.task;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.protocol.cmd.ImapFolder;
import com.gsma.rcs.core.cms.protocol.message.IImapMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapMmsMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapSmsMessage;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncSchedulerTask;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Task executed to push messages or update flags on the CMS server
 */
public class CmsSyncPushRequestTask extends CmsSyncSchedulerTask {

    private static Logger sLogger = Logger.getLogger(CmsSyncPushRequestTask.class.getName());
    private final RcsSettings mRcsSettings;
    private final Context mContext;
    private final XmsLog mXmsLog;
    private final CmsLog mCmsLog;
    private final ICmsSyncPushRequestTask mICmsSyncPushRequestTask;

    /**
     * Call this constructor when executing this task in a synchronous way
     *
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param xmsLog the XMS log accessor
     * @param cmsLog the MS log accessor
     * @param iCmsSyncPushRequestTask the callback
     */
    public CmsSyncPushRequestTask(Context context, RcsSettings rcsSettings, XmsLog xmsLog,
            CmsLog cmsLog, ICmsSyncPushRequestTask iCmsSyncPushRequestTask) {
        mRcsSettings = rcsSettings;
        mContext = context;
        mXmsLog = xmsLog;
        mCmsLog = cmsLog;
        mICmsSyncPushRequestTask = iCmsSyncPushRequestTask;
    }

    @Override
    public void execute(BasicImapService basicImapService) throws NetworkException,
            PayloadException, FileAccessException {
        Set<CmsObject> cmsObjects = mCmsLog.getXmsMessagesToPush();
        if (cmsObjects.isEmpty()) {
            return;
        }
        Set<CmsObject> pushOperations = new HashSet<>();
        Map<String, Set<CmsObject>> updateOperations = new HashMap<>();
        for (CmsObject cmsObject : cmsObjects) {
            if (CmsObject.PushStatus.PUSH_REQUESTED == cmsObject.getPushStatus()) {
                pushOperations.add(cmsObject);
            } else {
                // It is a CMS update flag operation
                String folder = cmsObject.getFolder();
                if (updateOperations.containsKey(folder)) {
                    Set<CmsObject> updates = updateOperations.get(folder);
                    updates.add(cmsObject);
                } else {
                    Set<CmsObject> updates = new HashSet<>();
                    updates.add(cmsObject);
                    updateOperations.put(folder, updates);
                }
            }
        }
        try {
            List<String> remoteFolders = getCmsFolder(basicImapService);
            if (!pushOperations.isEmpty()) {
                List<XmsDataObject> messagesToPush = filterMessagesToPush(pushOperations);
                if (!messagesToPush.isEmpty()) {
                    pushMessages(basicImapService, messagesToPush, remoteFolders);
                }
            }
            if (!updateOperations.isEmpty()) {
                for (Map.Entry<String, Set<CmsObject>> entry : updateOperations.entrySet()) {
                    String localFolder = entry.getKey();
                    // Only update flags if remote folder exists
                    if (remoteFolders.contains(localFolder)) {
                        updateFlags(basicImapService, localFolder, entry.getValue());
                    }
                }
            }
        } catch (IOException e) {
            throw new NetworkException("Failed to push requests", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to push requests", e);
        }
    }

    /**
     * Filter messages to push to CMS in order to only keep those that exist in XMS log.
     * 
     * @param pushOperations CmsObject to push on remote server
     * @return list of XmsDataObject
     */
    private List<XmsDataObject> filterMessagesToPush(Set<CmsObject> pushOperations) {
        List<XmsDataObject> msgToPush = new ArrayList<>();
        for (CmsObject cmsObject : pushOperations) {
            ContactId contact = CmsUtils.cmsFolderToContact(cmsObject.getFolder());
            XmsDataObject xms = mXmsLog.getXmsDataObject(contact, cmsObject.getMessageId());
            if (xms != null) {
                msgToPush.add(xms);
            }
        }
        return msgToPush;
    }

    private void pushMessages(BasicImapService basicImapService, List<XmsDataObject> messages,
            List<String> remoteFolders) throws IOException, ImapException {
        String from, to, direction;
        from = to = direction = null;
        Map<XmsDataObject, Integer> newUids = new HashMap<>();
        /*
         * Sort list of XMS objects by timestamp so that UIDs generated by CMS will be aligned with
         * the chronological order. This allows correlation for SMS messages.
         */
        Collections.sort(messages, new Comparator<XmsDataObject>() {
            @Override
            public int compare(XmsDataObject obj1, XmsDataObject obj2) {
                return Long.valueOf(obj1.getTimestamp()).compareTo(obj2.getTimestamp());
            }
        });
        String prevSelectedFolder = "";
        for (XmsDataObject message : messages) {
            if (sLogger.isActivated()) {
                sLogger.debug("Push " + message.toString());
            }
            List<Flag> flags = new ArrayList<>();
            ContactId remote = message.getContact();
            switch (message.getDirection()) {
                case INCOMING:
                    from = CmsUtils.contactToHeader(remote);
                    to = CmsUtils.contactToHeader(mRcsSettings.getUserProfileImsUserName());
                    direction = Constants.DIRECTION_RECEIVED;
                    break;
                case OUTGOING:
                    from = CmsUtils.contactToHeader(mRcsSettings.getUserProfileImsUserName());
                    to = CmsUtils.contactToHeader(remote);
                    direction = Constants.DIRECTION_SENT;
                    break;
                default:
                    break;
            }
            if (message.getReadStatus() != ReadStatus.UNREAD) {
                flags.add(Flag.Seen);
            }
            IImapMessage imapMessage = null;
            if (message instanceof SmsDataObject) {
                SmsDataObject sms = (SmsDataObject) message;
                imapMessage = new ImapSmsMessage(remote, from, to, direction, sms.getTimestamp(),
                        sms.getBody(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        sms.getMessageId());
            } else if (message instanceof MmsDataObject) {
                MmsDataObject mms = (MmsDataObject) message;
                imapMessage = new ImapMmsMessage(mContext, remote, from, to, direction,
                        mms.getTimestamp(), mms.getSubject(), UUID.randomUUID().toString(), UUID
                                .randomUUID().toString(), UUID.randomUUID().toString(),
                        mms.getMessageId(), mms.getMmsParts());
            }
            String remoteFolder = CmsUtils.contactToCmsFolder(message.getContact());
            if (!remoteFolders.contains(remoteFolder)) {
                basicImapService.create(remoteFolder);
                remoteFolders.add(remoteFolder);
            }
            if (!remoteFolder.equals(prevSelectedFolder)) {
                basicImapService.selectCondstore(remoteFolder);
                prevSelectedFolder = remoteFolder;
            }
            basicImapService.selectCondstore(remoteFolder);
            int uid = basicImapService.append(remoteFolder, flags, imapMessage.toPayload());
            newUids.put(message, uid);
        }
        if (!newUids.isEmpty() && mICmsSyncPushRequestTask != null) {
            mICmsSyncPushRequestTask.onMessagesPushed(newUids);
        }
    }

    private List<String> getCmsFolder(BasicImapService basicImapService) throws IOException,
            ImapException {
        List<String> cmsFolders = new ArrayList<>();
        for (ImapFolder imapFolder : basicImapService.listStatus()) {
            cmsFolders.add(imapFolder.getName());
        }
        return cmsFolders;
    }

    private void updateFlags(BasicImapService basicImapService, String remoteFolder,
            Set<CmsObject> cmsObjects) throws NetworkException, PayloadException, IOException,
            ImapException {
        Set<Integer> readRequestedUids = new HashSet<>();
        Set<Integer> deletedRequestedUids = new HashSet<>();
        basicImapService.select(remoteFolder);
        for (CmsObject cmsObject : cmsObjects) {
            Integer uid = cmsObject.getUid();
            if (uid == null) { // search uid on CMS server
                MessageType messageType = cmsObject.getMessageType();
                switch (messageType) {
                    case CHAT_MESSAGE:
                    case FILE_TRANSFER:
                        String msgId = cmsObject.getMessageId();
                        uid = basicImapService.searchUidWithHeader(
                                Constants.HEADER_IMDN_MESSAGE_ID, msgId);
                        if (uid != null) {
                            cmsObject.setUid(uid);
                            mCmsLog.updateUid(remoteFolder, msgId, uid);
                        }
                        break;

                    case SMS:
                        // TODO FGI
                        break;
                    case MMS:
                        // TODO FGI
                        break;
                }
            }
            if (uid == null) { // we are not able to update flags without UID
                continue;
            }
            if (CmsObject.ReadStatus.READ_REPORT_REQUESTED == cmsObject.getReadStatus()) {
                readRequestedUids.add(uid);
            }
            if (CmsObject.DeleteStatus.DELETED_REPORT_REQUESTED == cmsObject.getDeleteStatus()) {
                deletedRequestedUids.add(uid);
            }
        }
        if (!readRequestedUids.isEmpty()) {
            basicImapService.addFlags(TextUtils.join(",", readRequestedUids), Flag.Seen);
            if (mICmsSyncPushRequestTask != null) {
                mICmsSyncPushRequestTask.onReadRequestsReported(remoteFolder, readRequestedUids);
            }
        }
        if (!deletedRequestedUids.isEmpty()) {
            basicImapService.addFlags(TextUtils.join(",", deletedRequestedUids), Flag.Deleted);
            if (mICmsSyncPushRequestTask != null) {
                mICmsSyncPushRequestTask.onDeleteRequestsReported(remoteFolder,
                        deletedRequestedUids);
            }
        }
    }

    /**
     * A callback executed when requests are pushed on CMS server.
     */
    public interface ICmsSyncPushRequestTask {

        /**
         * Callback executed when messages are pushed on CMS
         *
         * @param uids the UIDS (value) associated with the XMS message (key)
         */
        void onMessagesPushed(Map<XmsDataObject, Integer> uids);

        /**
         * Callback executed when read requests for a given folder are reported on CMS
         *
         * @param uids the UIDS
         * @param folder the folder
         */
        void onReadRequestsReported(String folder, Set<Integer> uids);

        /**
         * Callback executed when delete requests for a given folder are reported on CMS
         *
         * @param uids the UIDS
         * @param folder the folder
         */
        void onDeleteRequestsReported(String folder, Set<Integer> uids);

    }
}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.gsma.rcs.core.cms.integration;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.protocol.cmd.ImapFolder;
import com.gsma.rcs.core.cms.protocol.message.IImapMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapMmsMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapSmsMessage;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.provider.cms.CmsData;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

//import com.gsma.rcs.utils.logger.Logger;

/**
 * A class to provide utilities to set up CMS test environment.
 */
public class ImapCmsUtilTest {

    private final RcsSettings mRcsSettings;
    private final Context mContext;
    private final BasicImapService mImapService;
    // private static final Logger sLogger = Logger.getLogger(ImapCmsUtilTest.class.getName());
    private final CmsLog mCmsLog;

    /**
     * Constructor
     *
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param imapService the basic IMAP service
     */
    public ImapCmsUtilTest(Context context, RcsSettings rcsSettings, BasicImapService imapService,
            CmsLog cmsLog) {
        mRcsSettings = rcsSettings;
        mContext = context;
        mImapService = imapService;
        mCmsLog = cmsLog;
    }

    /**
     * Push RCS messages onto the CMS
     * 
     * @param messages the list of IMAP interface
     * @return map of message IDs with created UID
     * @throws NetworkException
     * @throws PayloadException
     */
    public Map<String, Integer> createRemoteRcsMessages(
            List<ImapCmsUtilTest.IImapRcsMessage> messages) throws NetworkException,
            PayloadException {
        try {
            List<String> cmsFolders = getCmsFolder(mImapService);
            String prevSelectedFolder = "";
            Map<String, Integer> newUids = new HashMap<>();
            for (IImapRcsMessage message : messages) {
                String messageId = message.getMessageId();
                List<Flag> flags = new ArrayList<>();
                if (message.isSeen()) {
                    flags.add(Flag.Seen);
                }
                String remoteFolder = message.getFolder();
                if (!cmsFolders.contains(remoteFolder)) {
                    mImapService.create(remoteFolder);
                    cmsFolders.add(remoteFolder);
                }
                if (!remoteFolder.equals(prevSelectedFolder)) {
                    mImapService.selectCondstore(remoteFolder);
                    prevSelectedFolder = remoteFolder;
                }
                int uid = mImapService.append(remoteFolder, flags, message.toPayload());
                newUids.put(messageId, uid);
                message.setUid(uid);
            }
            return newUids;

        } catch (IOException e) {
            throw new NetworkException("Failed to push messages", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to push messages", e);
        }
    }

    /**
     * Push XMS messages on to the CMS.
     *
     * @param messages the list of messages to push
     * @return map of message IDs with created UID
     * @throws NetworkException
     * @throws PayloadException
     */
    private Map<String, Integer> createRemoteXmsMessages(List<XmsDataObject> messages)
            throws NetworkException, PayloadException {
        String from, to, direction;
        from = to = direction = null;
        try {
            /*
             * Sort list of XMS objects by timestamp so that UIDs generated by CMS will be aligned
             * with the chronological order. This allows correlation for SMS messages.
             */
            Collections.sort(messages, new Comparator<XmsDataObject>() {
                @Override
                public int compare(XmsDataObject obj1, XmsDataObject obj2) {
                    return Long.valueOf(obj1.getTimestamp()).compareTo(obj2.getTimestamp());
                }
            });
            List<String> cmsFolders = getCmsFolder(mImapService);
            String prevSelectedFolder = "";
            Map<String, Integer> newUids = new HashMap<>();
            for (XmsDataObject message : messages) {
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
                    imapMessage = new ImapSmsMessage(remote, from, to, direction,
                            sms.getTimestamp(), sms.getBody(), UUID.randomUUID().toString(), UUID
                                    .randomUUID().toString(), message.getMessageId());

                } else if (message instanceof MmsDataObject) {
                    MmsDataObject mms = (MmsDataObject) message;
                    imapMessage = new ImapMmsMessage(mContext, remote, from, to, direction,
                            mms.getTimestamp(), mms.getSubject(), UUID.randomUUID().toString(),
                            UUID.randomUUID().toString(), mms.getMessageId(), mms.getMessageId(),
                            mms.getMmsParts());
                }
                String remoteFolder = CmsUtils.contactToCmsFolder(message.getContact());
                if (!cmsFolders.contains(remoteFolder)) {
                    mImapService.create(remoteFolder);
                    cmsFolders.add(remoteFolder);
                }
                if (!remoteFolder.equals(prevSelectedFolder)) {
                    mImapService.selectCondstore(remoteFolder);
                    prevSelectedFolder = remoteFolder;
                }
                mImapService.selectCondstore(remoteFolder);
                int uid = mImapService.append(remoteFolder, flags, imapMessage.toPayload());
                newUids.put(message.getMessageId(), uid);
            }
            return newUids;

        } catch (IOException e) {
            throw new NetworkException("Failed to push messages", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to push messages", e);
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

    /**
     * Deletes all mailboxes
     *
     * @throws NetworkException
     * @throws PayloadException
     */
    public void deleteRemoteStorage() throws NetworkException, PayloadException {
        try {
            List<String> folders = mImapService.list();
            if (folders.isEmpty()) {
                return;
            }
            /*
             * Sort in reverse alphabetic order to delete first the sub-folders.
             */
            Collections.sort(folders, Collections.reverseOrder());
            mImapService.unselect();
            for (String folder : folders) {
                mImapService.delete(folder);
            }
        } catch (IOException e) {
            throw new NetworkException("Failed to delete all mailboxes", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to delete all mailboxes", e);
        }
    }

    /**
     * Deletes a mailbox
     *
     * @param mailbox the mailbox
     */
    public void deleteRemoteMailbox(String mailbox) throws NetworkException, PayloadException {
        try {
            // Selected mailbox shall not be deleted
            mImapService.unselect();
            mImapService.delete(mailbox);

        } catch (IOException e) {
            throw new NetworkException("Failed to delete mailbox " + mailbox, e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to delete mailbox " + mailbox, e);
        }
    }

    /**
     * Purge deleted remote CMS messages for a mailbox
     *
     * @param mailbox the mailbox
     */
    public void purgeDeleteRemoteMessages(String mailbox) throws NetworkException, PayloadException {
        try {
            mImapService.select(mailbox);
            mImapService.expunge();

        } catch (IOException e) {
            throw new NetworkException("Failed to delete mailbox " + mailbox, e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to delete mailbox " + mailbox, e);
        }
    }

    /**
     * Update flags for a remote folder
     *
     * @param mailbox the remote folder
     * @param cmsObjects the CMS objects for which flags should be updated
     */
    public void updateRemoteFlags(String mailbox, List<CmsObject> cmsObjects)
            throws NetworkException, PayloadException {
        Set<Integer> mReadRequestedUids = new HashSet<>();
        Set<Integer> mDeletedRequestedUids = new HashSet<>();
        try {
            mImapService.select(mailbox);
            for (CmsObject cmsObject : cmsObjects) {
                Integer uid = cmsObject.getUid();
                if (uid == null) { // search uid on CMS server
                    MessageType messageType = cmsObject.getMessageType();
                    switch (messageType) {
                        case CHAT_MESSAGE:
                        case FILE_TRANSFER:
                        case CPM_SESSION:
                            String msgId = cmsObject.getMessageId();
                            uid = mImapService.searchUidWithHeader(
                                    Constants.HEADER_IMDN_MESSAGE_ID, msgId);
                            if (uid != null) {
                                cmsObject.setUid(uid);
                                mCmsLog.updateUid(mailbox, msgId, uid);
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
                if (CmsData.ReadStatus.READ_REPORT_REQUESTED == cmsObject.getReadStatus()) {
                    mReadRequestedUids.add(uid);
                }
                if (DeleteStatus.DELETED_REPORT_REQUESTED == cmsObject.getDeleteStatus()) {
                    mDeletedRequestedUids.add(uid);
                }
            }
            if (!mReadRequestedUids.isEmpty()) {
                mImapService.addFlags(TextUtils.join(",", mReadRequestedUids), Flag.Seen);
            }
            if (!mDeletedRequestedUids.isEmpty()) {
                mImapService.addFlags(TextUtils.join(",", mDeletedRequestedUids), Flag.Deleted);
            }
        } catch (IOException e) {
            throw new NetworkException("Failed to update flags!", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to update flags!", e);
        }
    }

    public void createRemoteXmsMessages(XmsDataObject[] messages) throws NetworkException,
            PayloadException {
        createRemoteXmsMessages(Arrays.asList(messages));
    }

    public void updateRemoteFlags(String mailbox, CmsObject[] cmsObjects) throws NetworkException,
            PayloadException {
        updateRemoteFlags(mailbox, Arrays.asList(cmsObjects));
    }

    public interface IImapRcsMessage extends IImapMessage {

        String getChatId();

        String getMessageId();

        ContactId getRemote();

        void setUid(Integer uid);

        void markAsSeen();

        long getTimestamp();
    }

    public static String convertDirToImap(RcsService.Direction dir) {
        if (RcsService.Direction.INCOMING == dir) {
            return Constants.DIRECTION_RECEIVED;
        }
        return Constants.DIRECTION_SENT;
    }
}

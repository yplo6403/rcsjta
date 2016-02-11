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

package com.gsma.rcs.core.cms.sync.process;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.event.CmsEventListener;
import com.gsma.rcs.core.cms.event.exception.CmsSyncException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.protocol.message.IImapMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapMessageResolver;
import com.gsma.rcs.provider.cms.CmsFolder;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.utils.logger.Logger;

import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * LocalStorage implementation Apply remote changes from CMS server Resolve the IMAP message and
 * select the correct content provider to apply changes Retrieve changes from local content
 * providers which must be applied on the CMS server
 */
public class LocalStorage implements SyncProcessorHandler {

    private static Logger sLogger = Logger.getLogger(LocalStorage.class.getSimpleName());
    protected final CmsLog mCmsLog;
    private final CmsEventListener mCmsEventListener;
    private ImapMessageResolver mMessageResolver;

    /**
     * Constructor
     * 
     * @param cmsLog IMAP log accessor
     */
    public LocalStorage(CmsLog cmsLog, CmsEventListener cmsEventListener) {
        mCmsLog = cmsLog;
        mCmsEventListener = cmsEventListener;
        mMessageResolver = new ImapMessageResolver();
    }

    /**
     * Return the local folders from content provider
     *
     * @return map of folder
     */
    public Map<String, CmsFolder> getLocalFolders() {
        Map<String, CmsFolder> localFolders = mCmsLog.getFolders();
        for (Entry<String, CmsFolder> entry : localFolders.entrySet()) {
            Integer maxUid = mCmsLog.getMaxUidForMessages(entry.getKey());
            entry.getValue().setMaxUid(maxUid);
        }
        return localFolders;
    }

    /**
     * Apply folder change
     *
     * @param folder the folder
     */
    public void applyFolderChange(CmsFolder folder) {
        mCmsLog.addFolder(folder);
    }

    @Override
    public void applyFlagChange(List<FlagChange> flagchanges) {
        for (FlagChange fg : flagchanges) {
            String folder = fg.getFolder();
            boolean deleteFlag = fg.isDeleted();
            boolean seenFlag = fg.isSeen();
            for (Integer uid : fg.getUids()) {
                CmsObject msg = mCmsLog.getMessage(folder, uid);
                if (msg == null) {
                    if (sLogger.isActivated()) {
                        sLogger.info("Cannot find (" + folder + "," + uid
                                + ") in imap message provider");
                    }
                    continue;
                }
                if (deleteFlag) {
                    mCmsEventListener.onRemoteDeleteEvent(msg);
                } else if (seenFlag) {
                    mCmsEventListener.onRemoteReadEvent(msg);
                }
                mCmsLog.updateMessage(msg.getMessageType(), msg.getMessageId(), msg.getFolder(),
                        msg.getUid(), seenFlag, deleteFlag);
            }
        }
    }

    @Override
    public void removeLocalFolder(String folderName) {
        mCmsLog.removeFolder(folderName, true);
    }

    @Override
    public void updateLocalFolder(CmsFolder folder) {
        mCmsLog.addFolder(folder);
    }

    @Override
    public Set<Integer> filterNewMessages(List<ImapMessage> messages) {
        Set<Integer> uids = new TreeSet<>();
        for (ImapMessage msg : messages) {
            try {
                MessageType messageType = mMessageResolver.resolveType(msg);
                if (!checkMessageType(messageType)) {
                    continue;
                }
                IImapMessage resolvedMessage = mMessageResolver.resolveMessage(messageType, msg);
                CmsObject imapData = mCmsEventListener.searchLocalMessage(messageType,
                        resolvedMessage);
                boolean isDeleted = msg.getMetadata().getFlags().contains(Flag.Deleted);
                if (imapData == null) { // message not present in local storage
                    if (!isDeleted) { // prevent from downloading a new deleted message
                        uids.add(msg.getUid());
                    }
                } else {
                    // update flag for local message
                    boolean isSeen = msg.getMetadata().getFlags().contains(Flag.Seen);
                    if (isDeleted) {
                        mCmsEventListener.onRemoteDeleteEvent(imapData);
                    } else if (isSeen) {
                        mCmsEventListener.onRemoteReadEvent(imapData);
                    }
                    mCmsLog.updateMessage(imapData.getMessageType(), imapData.getMessageId(),
                            msg.getFolderPath(), msg.getUid(), isSeen, isDeleted);
                }
            } catch (CmsSyncHeaderFormatException | CmsSyncMissingHeaderException e) {
                /*
                 * There is a wrongly formatted IMAP message on the CMS server. Keep processing
                 * remaining IMAP messages but log error since it MUST be fixed on CMS server.
                 */
                sLogger.warn("FIX ME: badly formatted CMS message! [" + msg + "]", e);

            } catch (CmsSyncException e) {
                if (sLogger.isActivated()) {
                    sLogger.info(e.getMessage());
                }
            }
        }
        return uids;
    }

    private boolean checkMessageType(MessageType messageType) {
        boolean isActivated = sLogger.isActivated();
        if (messageType == MessageType.SMS || messageType == MessageType.MMS
                || messageType == MessageType.MESSAGE_CPIM
                || messageType == MessageType.CPM_SESSION) {
            return true;
        }
        if (isActivated) {
            sLogger.debug("This type of message is not synchronized : " + messageType);
        }
        return false;
    }

    @Override
    public void createMessages(Set<ImapMessage> messages) throws FileAccessException {
        Map<MessageType, List<IImapMessage>> mapOfMessages = resolveMessagesByType(messages);
        for (Entry<MessageType, List<IImapMessage>> entry : mapOfMessages.entrySet()) {
            MessageType messageType = entry.getKey();
            List<IImapMessage> resolvedMessages = entry.getValue();
            for (IImapMessage resolvedMessage : resolvedMessages) {
                try {
                    String messageId = mCmsEventListener.onRemoteNewMessage(messageType,
                            resolvedMessage);
                    CmsObject cmsObject = new CmsObject(resolvedMessage.getFolder(),
                            resolvedMessage.getUid(), resolvedMessage.isSeen() ? ReadStatus.READ
                                    : ReadStatus.UNREAD,
                            resolvedMessage.isDeleted() ? DeleteStatus.DELETED
                                    : DeleteStatus.NOT_DELETED, PushStatus.PUSHED, messageType,
                            messageId, null);
                    mCmsLog.addMessage(cmsObject);
                } catch (CmsSyncHeaderFormatException | CmsSyncMissingHeaderException e) {
                    /*
                     * There is a wrongly formatted IMAP message on the CMS server. Keep processing
                     * remaining IMAP messages but log error since it MUST be fixed on CMS server.
                     */
                    sLogger.warn("FIX ME: badly formatted CMS message! [" + resolvedMessage + "]",
                            e);
                } catch (CmsSyncException e) {
                    if (sLogger.isActivated()) {
                        sLogger.info(e.getMessage());
                    }
                }
            }
        }
    }

    private Map<MessageType, List<IImapMessage>> resolveMessagesByType(Set<ImapMessage> rawMessages) {
        Map<MessageType, List<IImapMessage>> mapOfMessages = new LinkedHashMap<>();
        mapOfMessages.put(MessageType.CPM_SESSION, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.CHAT_MESSAGE, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.SMS, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.MMS, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.IMDN, new ArrayList<IImapMessage>());

        for (ImapMessage msg : rawMessages) {
            try {
                MessageType messageType = mMessageResolver.resolveType(msg);
                List msgList = mapOfMessages.get(messageType);
                if (msgList == null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("This type of message is not synchronized : " + messageType);
                    }
                    continue;
                }
                msgList.add(mMessageResolver.resolveMessage(messageType, msg));
            } catch (CmsSyncMissingHeaderException e) {
                /*
                 * Missing mandatory header on the CMS server. Keep processing remaining IMAP
                 * messages but log error since it MUST be fixed on CMS server.
                 */
                sLogger.warn("FIX ME: badly formatted CMS message! [" + msg + "]", e);
            } catch (CmsSyncException e) {
                if (sLogger.isActivated()) {
                    sLogger.info(e.getMessage());
                }
            }
        }
        return mapOfMessages;
    }

    @Override
    public Set<FlagChange> getLocalFlagChanges(String folder) {
        Set<FlagChange> changes = new HashSet<>();
        Set<Integer> readUids = new HashSet<>();
        Set<Integer> deletedUids = new HashSet<>();
        for (CmsObject cmsObject : mCmsLog.getMessages(folder, ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.DELETED_REPORT_REQUESTED)) {
            Integer uid = cmsObject.getUid();
            if (uid != null) {
                if (ReadStatus.READ_REPORT_REQUESTED == cmsObject.getReadStatus()) {
                    readUids.add(uid);
                }
                if (DeleteStatus.DELETED_REPORT_REQUESTED == cmsObject.getDeleteStatus()) {
                    deletedUids.add(uid);
                }
            }
        }
        if (!readUids.isEmpty()) {
            changes.add(new FlagChange(folder, readUids, Flag.Seen));
        }
        if (!deletedUids.isEmpty()) {
            changes.add(new FlagChange(folder, deletedUids, Flag.Deleted));
        }
        return changes;
    }

    /**
     * @param flagChanges set of UIDs with changed flags
     */
    public void finalizeLocalFlagChanges(Set<FlagChange> flagChanges) {
        for (FlagChange fg : flagChanges) {
            String folder = fg.getFolder();
            boolean seenFlag = fg.isSeen();
            boolean deleteFlag = fg.isDeleted();
            for (Integer uid : fg.getUids()) {
                if (seenFlag) {
                    mCmsLog.updateReadStatus(folder, uid, ReadStatus.READ);
                }
                if (deleteFlag) {
                    mCmsLog.updateDeleteStatus(folder, uid, DeleteStatus.DELETED);
                }
            }
        }
    }
}

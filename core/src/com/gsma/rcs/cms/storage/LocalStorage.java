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

package com.gsma.rcs.cms.storage;

import com.gsma.rcs.cms.event.CmsEventListener;
import com.gsma.rcs.cms.event.exception.CmsSyncException;
import com.gsma.rcs.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.provider.imap.FolderData;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.sync.ISyncProcessorHandler;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;

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
public class LocalStorage implements ISyncProcessorHandler {

    private static Logger sLogger = Logger.getLogger(LocalStorage.class.getSimpleName());
    protected final ImapLog mImapLog;
    private final CmsEventListener mCmsEventListener;
    private ImapMessageResolver mMessageResolver;

    /**
     * Constructor
     * 
     * @param imapLog IMAP log accessor
     */
    public LocalStorage(ImapLog imapLog, CmsEventListener cmsEventListener) {
        mImapLog = imapLog;
        mCmsEventListener = cmsEventListener;
        mMessageResolver = new ImapMessageResolver();
    }

    /**
     * Return the local folders from content provider
     *
     * @return map of folder
     */
    public Map<String, FolderData> getLocalFolders() {
        Map<String, FolderData> localFolders = mImapLog.getFolders();
        for (Entry<String, FolderData> entry : localFolders.entrySet()) {
            Integer maxUid = mImapLog.getMaxUidForMessages(entry.getKey());
            entry.getValue().setMaxUid(maxUid);
        }
        return localFolders;
    }

    /**
     * Apply folder change
     *
     * @param folder the folder
     */
    public void applyFolderChange(FolderData folder) {
        mImapLog.addFolder(folder);
    }

    @Override
    public void applyFlagChange(List<FlagChange> flagchanges) {
        for (FlagChange fg : flagchanges) {
            String folder = fg.getFolder();
            boolean deleteFlag = fg.isDeleted();
            boolean seenFlag = fg.isSeen();
            for (Integer uid : fg.getUids()) {
                MessageData msg = mImapLog.getMessage(folder, uid);
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
                mImapLog.updateMessage(msg.getMessageType(), msg.getMessageId(), msg.getFolder(),
                        msg.getUid(), seenFlag, deleteFlag);
            }
        }
    }

    @Override
    public void removeLocalFolder(String folderName) {
        mImapLog.removeFolder(folderName, true);
    }

    @Override
    public void updateLocalFolder(FolderData folder) {
        mImapLog.addFolder(folder);
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
                MessageData imapData = mCmsEventListener.searchLocalMessage(messageType,
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
                    mImapLog.updateMessage(imapData.getMessageType(), imapData.getMessageId(),
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
                || messageType == MessageType.GROUP_STATE) {
            return true;
        }
        if (isActivated) {
            sLogger.debug("This type of message is not synchronized : " + messageType);
        }
        return false;
    }

    @Override
    public void createMessages(List<ImapMessage> messages) throws FileAccessException {
        Map<MessageType, List<IImapMessage>> mapOfMessages = resolveMessagesByType(messages);
        for (Entry<MessageType, List<IImapMessage>> entry : mapOfMessages.entrySet()) {
            MessageType messageType = entry.getKey();
            List<IImapMessage> resolvedMessages = entry.getValue();
            for (IImapMessage resolvedMessage : resolvedMessages) {
                try {
                    String messageId = mCmsEventListener.onRemoteNewMessage(messageType,
                            resolvedMessage);
                    MessageData messageData = new MessageData(resolvedMessage.getFolder(),
                            resolvedMessage.getUid(), resolvedMessage.isSeen() ? ReadStatus.READ
                                    : ReadStatus.UNREAD,
                            resolvedMessage.isDeleted() ? DeleteStatus.DELETED
                                    : DeleteStatus.NOT_DELETED, PushStatus.PUSHED, messageType,
                            messageId, null);
                    mImapLog.addMessage(messageData);
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

    private Map<MessageType, List<IImapMessage>> resolveMessagesByType(List<ImapMessage> rawMessages) {

        Map<MessageType, List<IImapMessage>> mapOfMessages = new LinkedHashMap();
        mapOfMessages.put(MessageType.GROUP_STATE, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.SESSION_INFO, new ArrayList<IImapMessage>());
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
        for (MessageData messageData : mImapLog.getMessages(folder,
                ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.DELETED_REPORT_REQUESTED)) {
            Integer uid = messageData.getUid();
            if (uid != null) {
                if (ReadStatus.READ_REPORT_REQUESTED == messageData.getReadStatus()) {
                    readUids.add(uid);
                }
                if (DeleteStatus.DELETED_REPORT_REQUESTED == messageData.getDeleteStatus()) {
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
                    mImapLog.updateReadStatus(folder, uid, ReadStatus.READ);
                }
                if (deleteFlag) {
                    mImapLog.updateDeleteStatus(folder, uid, DeleteStatus.DELETED);
                }
            }
        }
    }
}

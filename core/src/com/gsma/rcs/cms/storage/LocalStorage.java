/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

import com.gsma.rcs.cms.event.IRemoteEventHandler;
import com.gsma.rcs.cms.event.ImapHeaderFormatException;
import com.gsma.rcs.cms.event.MissingImapHeaderException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * LocalStorage implementation
 * Apply remote changes from CMS server
 * Resolve the IMAP message and select the correct content provider to apply changes
 * Retrieve changes from local content providers which must be applied on the CMS server
 */
public class LocalStorage implements ISyncProcessorHandler {

    private static Logger sLogger = Logger.getLogger(LocalStorage.class.getSimpleName());
    protected ImapLog mImapLog;
    /* package private */Map<MessageType, IRemoteEventHandler> mRemoteEventHandlers = new HashMap<>();
    private ImapMessageResolver mMessageResolver;

    /**
     * Constructor
     * @param imapLog
     */
    public LocalStorage(ImapLog imapLog) {
        mImapLog = imapLog;
        mMessageResolver = new ImapMessageResolver();
    }

    /**
     * @param messageType
     * @param handler
     */
    public void registerRemoteEventHandler(MessageType messageType, IRemoteEventHandler handler) {
        mRemoteEventHandlers.put(messageType, handler);
    }

    /**
     * @param messageType
     */
    public void unregisterRemoteEventHandler(MessageType messageType) {
        mRemoteEventHandlers.remove(messageType);
    }

    public void removeListeners() {
        mRemoteEventHandlers.clear();
        mRemoteEventHandlers = null;
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
     * @param folder
     */
    public void applyFolderChange(FolderData folder) {
        mImapLog.addFolder(folder);
    }

    @Override
    public void applyFlagChange(List<FlagChange> flagchanges) {
        for (FlagChange fg : flagchanges) {
            String folder = fg.getFolder();
            boolean deleteFlag = fg.addDeletedFlag();
            boolean seenFlag = fg.addSeenFlag();
            for (Integer uid : fg.getUids()) {
                MessageData msg = mImapLog.getMessage(folder, uid);
                if (msg == null) {
                    if (sLogger.isActivated()) {
                        sLogger.info("Cannot find (" + folder + "," + uid
                                + ") in imap message provider");
                    }
                    continue;
                }
                MessageType messageType = msg.getMessageType();
                IRemoteEventHandler storageHandler = mRemoteEventHandlers.get(messageType);
                if (deleteFlag) {
                    msg.setDeleteStatus(DeleteStatus.DELETED);
                    storageHandler.onRemoteDeleteEvent(messageType, msg.getMessageId());
                } else if (seenFlag) {
                    msg.setReadStatus(ReadStatus.READ);
                    storageHandler.onRemoteReadEvent(messageType, msg.getMessageId());
                }
                mImapLog.addMessage(msg);
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
                if (messageType == null) {
                    continue;
                }
                IImapMessage resolvedMessage = mMessageResolver.resolveMessage(messageType, msg);
                if (resolvedMessage == null) {
                    continue;
                }
                IRemoteEventHandler remoteEventHandler = mRemoteEventHandlers.get(messageType);
                String messageId = remoteEventHandler.getMessageId(messageType, resolvedMessage);
                boolean isDeleted = msg.getMetadata().getFlags().contains(Flag.Deleted);
                if (messageId == null) { // message not present in local storage
                    if(!isDeleted){ // prevent from downloading a new deleted message
                        uids.add(msg.getUid());
                    }
                } else {
                    // update flag for local message
                    boolean isSeen = msg.getMetadata().getFlags().contains(Flag.Seen);
                    if (isDeleted) {
                        remoteEventHandler.onRemoteDeleteEvent(messageType, messageId);
                    }
                    if (isSeen) {
                        remoteEventHandler.onRemoteReadEvent(messageType, messageId);
                    }
                    mImapLog.updateMessage(messageType, messageId, msg.getFolderPath(),
                            msg.getUid(), isSeen, isDeleted);
                }
            } catch (ImapHeaderFormatException e) {
                /* There is a wrongly formatted IMAP message on the CMS server. Keep processing
                   remaining IMAP messages but log error since it MUST be fixed on CMS server.
                */
                sLogger.error("FIX ME: badly formatted CMS message! [" + msg + "]", e);
            } catch (MissingImapHeaderException e) {
                /* Missing mandatory header on the CMS server. Keep processing
                   remaining IMAP messages but log error since it MUST be fixed on CMS server.
                */
                sLogger.error("FIX ME: badly formatted CMS message! [" + msg + "]", e);
            }
        }
        return uids;
    }

    @Override
    public void createMessages(List<ImapMessage> messages) throws FileAccessException {
        for (ImapMessage msg : messages) {

            MessageType messageType = mMessageResolver.resolveType(msg);
            IImapMessage resolvedMessage = mMessageResolver.resolveMessage(messageType, msg);
            if (resolvedMessage == null) {
                continue;
            }
            try {
                String messageId = mRemoteEventHandlers.get(messageType).onRemoteNewMessage(
                        messageType, resolvedMessage);
                MessageData messageData = new MessageData(resolvedMessage.getFolder(),
                        resolvedMessage.getUid(), resolvedMessage.isSeen() ? ReadStatus.READ
                                : ReadStatus.UNREAD,
                        resolvedMessage.isDeleted() ? DeleteStatus.DELETED
                                : DeleteStatus.NOT_DELETED, PushStatus.PUSHED, messageType,
                        messageId, null);
                mImapLog.addMessage(messageData);

            } catch (ImapHeaderFormatException e) {
                /* There is a wrongly formatted IMAP message on the CMS server. Keep processing
                   remaining IMAP messages but log error since it MUST be fixed on CMS server.
                */
                sLogger.error("FIX ME: badly formatted CMS message! [" + msg + "]", e);
            } catch (MissingImapHeaderException e) {
                /* Missing mandatory header on the CMS server. Keep processing
                   remaining IMAP messages but log error since it MUST be fixed on CMS server.
                */
                sLogger.error("FIX ME: badly formatted CMS message! [" + msg + "]", e);
            }
        }
    }

    @Override
    public List<FlagChange> getLocalFlagChanges(String folder) {
        List<FlagChange> changes = new ArrayList<>();
        List<Integer> readUids = new ArrayList<>();
        List<Integer> deletedUids = new ArrayList<>();
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
     * @param flagChanges
     */
    public void finalizeLocalFlagChanges(List<FlagChange> flagChanges) {
        for (FlagChange fg : flagChanges) {
            String folder = fg.getFolder();
            boolean seenFlag = fg.addSeenFlag();
            boolean deleteFlag = fg.addDeletedFlag();
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

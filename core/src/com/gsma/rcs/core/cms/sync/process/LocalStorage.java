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
import com.gsma.rcs.core.cms.protocol.message.IImapMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapMessageResolver;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsFolder;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * LocalStorage<br>
 * <ul>
 * <li>Apply remote changes from CMS server</li>
 * <li>Resolve the IMAP message and select the correct content provider to apply changes</li>
 * <li>Retrieve changes from local content providers which must be applied on the CMS server</li>
 * </ul>
 */
public class LocalStorage {

    private static final Logger sLogger = Logger.getLogger(LocalStorage.class.getName());

    protected final CmsLog mCmsLog;
    private final CmsEventListener mCmsEventListener;
    private ImapMessageResolver mMessageResolver;

    /**
     * Constructor
     * 
     * @param cmsLog IMAP log accessor
     */
    public LocalStorage(RcsSettings rcsSettings, CmsLog cmsLog, CmsEventListener cmsEventListener) {
        mCmsLog = cmsLog;
        mCmsEventListener = cmsEventListener;
        mMessageResolver = new ImapMessageResolver(rcsSettings);
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
        if (mCmsLog.isFolderPersisted(folder.getName())) {
            mCmsLog.updateFolder(folder);
        } else {
            mCmsLog.addFolder(folder);
        }
    }

    /**
     * Apply flag changes from a remote folder
     *
     * @param flagChanges the list of flag changes
     */
    public void applyFlagChange(List<FlagChangeOperation> flagChanges) {
        for (FlagChangeOperation fg : flagChanges) {
            String folder = fg.getFolder();
            boolean deleteFlag = fg.isDeleted();
            boolean seenFlag = fg.isSeen();
            for (Integer uid : fg.getUids()) {
                CmsObject msg = mCmsLog.getMessage(folder, uid);
                if (msg == null) {
                    if (sLogger.isActivated()) {
                        sLogger.info("Cannot find (" + folder + "," + uid
                                + ") in CMS message provider");
                    }
                    continue;
                }
                if (deleteFlag) {
                    mCmsEventListener.onRemoteDeleteEvent(msg);

                } else if (seenFlag) {
                    mCmsEventListener.onRemoteReadEvent(msg);
                }
            }
        }
    }

    /**
     * Delete local IMAP folder when it is no more valid by checking the UIDVALIDITY value retrieved
     * from CMS.
     *
     * @param folderName the folder
     */
    public void removeLocalFolder(String folderName) {
        mCmsLog.removeFolder(folderName, true);
    }

    /**
     * Return messages that are not already present in local storage. The content of these messages
     * should be downloaded from CMS server
     *
     * @param unSyncImapHeaders the list of un-synchronized IMAP headers
     * @param remote the remote contact or null for group chat
     * @return uids of new messages
     */
    public Set<Integer> filterNewMessages(List<ImapMessage> unSyncImapHeaders, ContactId remote) {
        Set<Integer> uids = new TreeSet<>();
        for (ImapMessage header : unSyncImapHeaders) {
            try {
                MessageType msgType = mMessageResolver.resolveType(header);
                if (!checkMessageType(msgType)) {
                    continue;
                }
                IImapMessage resolvedMessage = mMessageResolver.resolveMessage(msgType, header,
                        remote, true);
                CmsObject imapData = mCmsEventListener.searchLocalMessage(msgType, resolvedMessage);
                Set<Flag> flags = header.getMetadata().getFlags();
                boolean isDeleted = flags.contains(Flag.Deleted);
                if (imapData == null) {
                    // message not present in local storage
                    if (!isDeleted) {
                        // prevent from downloading a new deleted message
                        uids.add(header.getUid());
                    }
                } else {
                    // If we are here, this means that local message is pushed
                    boolean isSeen = flags.contains(Flag.Seen);
                    switch (imapData.getDeleteStatus()) {
                        case NOT_DELETED:
                            if (isDeleted) {
                                // Remote is deleted but local is not: report deletion event
                                imapData.setUid(header.getUid());
                                mCmsEventListener.onRemoteDeleteEvent(imapData);
                                continue;

                            } else {
                                /*
                                 * Both remote and local are not deleted. Check if local is seen as
                                 * pushed.
                                 */
                                if (imapData.getUid() == null && !isSeen) {
                                    // Update local with UID and set state to pushed.
                                    mCmsLog.updateUid(header.getFolderPath(),
                                            imapData.getMessageId(), header.getUid());
                                    continue;
                                }
                            }
                            break;

                        case DELETED_REPORT_REQUESTED:
                        case DELETED_REPORTED:
                            if (isDeleted) {
                                // Remote is deleted but local is in progress: complete deletion.
                                mCmsLog.updateMessage(imapData.getMessageId(),
                                        header.getFolderPath(), header.getUid(), true, true);
                            } else {
                                /*
                                 * Remote is not deleted and local is in progress. Check if local is
                                 * seen as pushed.
                                 */
                                if (imapData.getUid() == null) {
                                    // Update local with UID and set state to pushed.
                                    mCmsLog.updateUid(header.getFolderPath(),
                                            imapData.getMessageId(), header.getUid());
                                }
                            }
                            continue;

                        case DELETED:
                            // Check if local is seen as pushed.
                            if (imapData.getUid() == null) {
                                if (sLogger.isActivated()) {
                                    sLogger.warn("CMS message should have already been resolved="
                                            + imapData);
                                }
                                // Update local with UID and set state to pushed.
                                mCmsLog.updateUid(header.getFolderPath(), imapData.getMessageId(),
                                        header.getUid());
                            }
                            if (!isDeleted) {
                                // Local is deleted but remote not: state is impossible.
                                if (sLogger.isActivated()) {
                                    sLogger.warn("CMS message should have already been deleted="
                                            + imapData);
                                }
                            }
                            continue;
                    }
                    /*
                     * If we arrive here, the message is not deleted and deletion is not in
                     * progress.
                     */
                    switch (imapData.getReadStatus()) {
                        case UNREAD:
                            if (isSeen) {
                                // Remote is seen but local is not: report event.
                                imapData.setUid(header.getUid());
                                mCmsEventListener.onRemoteReadEvent(imapData);

                            } else {
                                /*
                                 * Both remote and local are not seen. Check if local is seen as
                                 * pushed.
                                 */
                                if (imapData.getUid() == null) {
                                    // Update local with UID and set state to pushed.
                                    mCmsLog.updateUid(header.getFolderPath(),
                                            imapData.getMessageId(), header.getUid());
                                }
                            }
                            break;

                        case READ_REPORT_REQUESTED:
                        case READ_REPORTED:
                            if (isSeen) {
                                // Remote is seen but local is in progress: update local.
                                mCmsLog.updateMessage(imapData.getMessageId(),
                                        header.getFolderPath(), header.getUid(), true, false);
                            } else {
                                /*
                                 * Remote is not seen and local is in progress. Check if local is
                                 * seen as pushed.
                                 */
                                if (imapData.getUid() == null) {
                                    // Local has just been resolved
                                    mCmsLog.updateUid(header.getFolderPath(),
                                            imapData.getMessageId(), header.getUid());
                                }
                            }
                            break;

                        case READ:
                            if (imapData.getUid() == null) {
                                // Local has just been resolved
                                mCmsLog.updateUid(header.getFolderPath(), imapData.getMessageId(),
                                        header.getUid());
                            }
                            if (!isSeen) {
                                // Local is seen but remote not
                                if (sLogger.isActivated()) {
                                    sLogger.warn("CMS message should have already been seen="
                                            + imapData);
                                }
                            }
                    }
                }
            } catch (CmsSyncException e) {
                /*
                 * There is a wrongly formatted IMAP message on the CMS server. Keep processing
                 * remaining IMAP messages but log error since it MUST be fixed on CMS server.
                 */
                sLogger.warn("FIX ME: badly formatted CMS message! [" + header + "]", e);

            } catch (RuntimeException e) {
                /*
                 * Intentionally catch runtime exceptions as else it will abruptly end the sync
                 * process
                 */
                sLogger.error("Failed to filter new messages : ", e);
            }
        }
        return uids;
    }

    private boolean checkMessageType(MessageType messageType) {
        switch (messageType) {
            case SMS:
            case MMS:
            case MESSAGE_CPIM:
            case CPM_SESSION:
            case GROUP_STATE:
                return true;
            default:
                sLogger.error("This type of message is not synchronized : " + messageType);
                return false;
        }
    }

    /**
     * Create new messages in local storage.
     *
     * @param messages the set of messages
     * @param remote the remote contact or null if group conversation
     */
    public void createMessages(Set<ImapMessage> messages, ContactId remote)
            throws FileAccessException {
        Map<MessageType, List<IImapMessage>> mapOfMessages = resolveMessagesByType(messages, remote);
        for (Entry<MessageType, List<IImapMessage>> entry : mapOfMessages.entrySet()) {
            MessageType messageType = entry.getKey();
            List<IImapMessage> resolvedMessages = entry.getValue();
            for (IImapMessage resolvedMessage : resolvedMessages) {
                try {
                    String[] result = mCmsEventListener.onRemoteNewMessage(messageType,
                            resolvedMessage, remote);
                    String messageId = result[0];
                    String chatId = null;
                    if (result.length == 2) {
                        chatId = result[1];
                    }
                    ReadStatus readStatus = resolvedMessage.isSeen() ? ReadStatus.READ
                            : ReadStatus.UNREAD;
                    DeleteStatus delStatus = resolvedMessage.isDeleted() ? DeleteStatus.DELETED
                            : DeleteStatus.NOT_DELETED;
                    if (CmsObject.isXmsData(messageType)) {
                        CmsXmsObject cmsObject = new CmsXmsObject(messageType,
                                resolvedMessage.getFolder(), messageId, resolvedMessage.getUid(),
                                PushStatus.PUSHED, readStatus, delStatus, null);
                        mCmsLog.addXmsMessage(cmsObject);
                    } else {
                        CmsRcsObject cmsObject = new CmsRcsObject(messageType,
                                resolvedMessage.getFolder(), messageId, resolvedMessage.getUid(),
                                PushStatus.PUSHED, readStatus, delStatus, chatId);
                        mCmsLog.addRcsMessage(cmsObject);
                    }
                } catch (CmsSyncException e) {
                    /*
                     * There is a wrongly formatted IMAP message on the CMS server. Keep processing
                     * remaining IMAP messages but log error since it MUST be fixed on CMS server.
                     */
                    sLogger.warn("FIX ME: badly formatted CMS message! [" + resolvedMessage + "]",
                            e);

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the sync
                     * process
                     */
                    sLogger.error("Failed to create message : ", e);
                }
            }
        }
    }

    private Map<MessageType, List<IImapMessage>> resolveMessagesByType(
            Set<ImapMessage> rawMessages, ContactId remote) {
        Map<MessageType, List<IImapMessage>> mapOfMessages = new LinkedHashMap<>();
        mapOfMessages.put(MessageType.CPM_SESSION, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.GROUP_STATE, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.CHAT_MESSAGE, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.FILE_TRANSFER, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.SMS, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.MMS, new ArrayList<IImapMessage>());
        mapOfMessages.put(MessageType.IMDN, new ArrayList<IImapMessage>());
        for (ImapMessage msg : rawMessages) {
            try {
                MessageType messageType = mMessageResolver.resolveType(msg);
                List<IImapMessage> msgList = mapOfMessages.get(messageType);
                if (msgList == null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("This type of message is not synchronized : " + messageType);
                    }
                    continue;
                }
                msgList.add(mMessageResolver.resolveMessage(messageType, msg, remote, false));

            } catch (CmsSyncException e) {
                /*
                 * Missing mandatory header on the CMS server. Keep processing remaining IMAP
                 * messages but log error since it MUST be fixed on CMS server.
                 */
                sLogger.warn("FIX ME: badly formatted CMS message! [" + msg + "]", e);

            } catch (RuntimeException e) {
                /*
                 * Intentionally catch runtime exceptions as else it will abruptly end the sync
                 * process
                 */
                sLogger.error("Failed to resolve message : ", e);
            }
        }
        return mapOfMessages;
    }

    /**
     * Get flag changes from local storage for a folder
     *
     * @param folder the folder
     * @return flagChanges the set of flag changes
     */
    public Set<FlagChangeOperation> getLocalFlagChanges(String folder) {
        Set<FlagChangeOperation> changes = new HashSet<>();
        Set<Integer> readUids = new HashSet<>();
        Set<Integer> deletedUids = new HashSet<>();
        for (CmsObject cmsObject : mCmsLog.getMessagesToSync(folder)) {
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
            changes.add(new FlagChangeOperation(folder, readUids, Flag.Seen));
        }
        if (!deletedUids.isEmpty()) {
            changes.add(new FlagChangeOperation(folder, deletedUids, Flag.Deleted));
        }
        return changes;
    }

    /**
     * @param flagChanges set of UIDs with changed flags
     */
    public void finalizeLocalFlagChanges(Set<FlagChangeOperation> flagChanges) {
        for (FlagChangeOperation fg : flagChanges) {
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

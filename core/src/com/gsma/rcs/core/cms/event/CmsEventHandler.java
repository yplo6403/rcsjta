/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2019-2016 Orange.
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

package com.gsma.rcs.core.cms.event;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncBadStateException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMessageNotSupportedException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.protocol.message.IImapMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapChatMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapCpimMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapCpmSessionMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapFileTransferMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapGroupStateMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapImdnMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapMmsMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapSmsMessage;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.messaging.GroupChatDeleteTask;
import com.gsma.rcs.provider.messaging.GroupChatMessageDeleteTask;
import com.gsma.rcs.provider.messaging.GroupChatPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.GroupFileTransferDeleteTask;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.messaging.OneToOneChatMessageDeleteTask;
import com.gsma.rcs.provider.messaging.OneToOneFileTransferDeleteTask;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsDeleteTask;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObjectFactory;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.content.Context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmsEventHandler implements CmsEventListener {

    private static final Logger sLogger = Logger.getLogger(CmsEventHandler.class.getName());

    private final Context mCtx;
    private final XmsLog mXmsLog;
    private final CmsLog mCmsLog;
    private final MessagingLog mMessagingLog;
    private final ChatServiceImpl mChatService;
    private final FileTransferServiceImpl mFileTransferService;
    private final RcsSettings mSettings;
    private final CmsServiceImpl mCmsService;
    private final LocalContentResolver mLocalContentResolver;
    private final InstantMessagingService mImService;

    /**
     * Default constructor
     *
     * @param context the context
     * @param localContentResolver the local content resolver
     * @param cmsLog the CMS log accessor
     * @param xmsLog the XMS log accessor
     * @param messagingLog the messaging log accessor
     * @param chatService the chat service impl
     * @param fileTransferService the file transfer service impl
     * @param cmsService the CMS service impl
     * @param imService the instant messaging service
     * @param settings the RCS settings accessor
     */
    public CmsEventHandler(Context context, LocalContentResolver localContentResolver,
            CmsLog cmsLog, XmsLog xmsLog, MessagingLog messagingLog, ChatServiceImpl chatService,
            FileTransferServiceImpl fileTransferService, CmsServiceImpl cmsService,
            InstantMessagingService imService, RcsSettings settings) {
        mCtx = context;
        mLocalContentResolver = localContentResolver;
        mXmsLog = xmsLog;
        mCmsLog = cmsLog;
        mMessagingLog = messagingLog;
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mCmsService = cmsService;
        mImService = imService;
        mSettings = settings;
    }

    @Override
    public void onRemoteReadEvent(CmsObject cmsObject) {
        switch (cmsObject.getMessageType()) {
            case SMS:
            case MMS:
                onEventReadXmsMessage((CmsXmsObject) cmsObject);
                break;
            case CHAT_MESSAGE:
                onEventReadChatMessage((CmsRcsObject) cmsObject);
                break;
            case FILE_TRANSFER:
                onEventReadFileTransfer((CmsRcsObject) cmsObject);
                break;
        }
    }

    private void onEventReadXmsMessage(CmsXmsObject imapData) {
        String msgId = imapData.getMessageId();
        ContactId contact = CmsUtils.cmsFolderToContact(imapData.getFolder());
        mCmsLog.updateXmsReadStatus(contact, msgId, ReadStatus.READ, imapData.getUid());
        // There is not timestamp displayed for XMS
        if (mXmsLog.markMessageAsRead(contact, msgId)) {
            mCmsService.broadcastMessageRead(contact, msgId);
            if (sLogger.isActivated()) {
                sLogger.debug("onEventReadXmsMessage ID=" + msgId + " contact=" + contact
                        + " success");
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("onEventReadXmsMessage ID=" + msgId + " contact=" + contact
                        + " failed: already read or deleted");
            }
        }
    }

    private void onEventReadChatMessage(CmsRcsObject imapData) {
        String msgId = imapData.getMessageId();
        ContactId contact = CmsUtils.cmsFolderToContact(imapData.getFolder());
        mCmsLog.updateRcsReadStatus(MessageType.CHAT_MESSAGE, msgId, ReadStatus.READ,
                imapData.getUid());
        if (mMessagingLog.markMessageAsRead(msgId, NtpTrustedTime.currentTimeMillis())) {
            mChatService.broadcastMessageRead(msgId);
            if (sLogger.isActivated()) {
                sLogger.debug("onEventReadChatMessage ID=" + msgId + " contact=" + contact
                        + " success");
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("onEventReadChatMessage ID=" + msgId + " contact=" + contact
                        + " failed: read or deleted");
            }
        }
    }

    private void onEventReadFileTransfer(CmsRcsObject imapData) {
        String msgId = imapData.getMessageId();
        mCmsLog.updateRcsReadStatus(MessageType.FILE_TRANSFER, msgId, ReadStatus.READ,
                imapData.getUid());
        if (mMessagingLog.markFileTransferAsRead(msgId, NtpTrustedTime.currentTimeMillis())) {
            mFileTransferService.broadcastFileTransferRead(msgId);
            if (sLogger.isActivated()) {
                sLogger.debug("onEventReadFileTransfer ID=" + msgId + " success");
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("onEventReadFileTransfer ID=" + msgId + " failed: read or deleted");
            }
        }
    }

    @Override
    public void onRemoteDeleteEvent(CmsObject cmsObject) {
        switch (cmsObject.getMessageType()) {
            case SMS:
            case MMS:
                onEventDeleteXmsMessage((CmsXmsObject) cmsObject);
                break;

            case CHAT_MESSAGE:
                onEventDeleteChatMessage((CmsRcsObject) cmsObject);
                break;

            case CPM_SESSION:
                onEventDeleteGroupChat((CmsRcsObject) cmsObject);
                break;

            case FILE_TRANSFER:
                onEventDeleteFileTransfer((CmsRcsObject) cmsObject);
                break;
        }
    }

    private void onEventDeleteXmsMessage(CmsXmsObject imapData) {
        if (sLogger.isActivated()) {
            sLogger.debug("onEventDeleteXmsMessage " + imapData);
        }
        ContactId contact = CmsUtils.cmsFolderToContact(imapData.getFolder());
        String msgId = imapData.getMessageId();
        XmsDeleteTask task = new XmsDeleteTask(mCmsService, mLocalContentResolver, contact, msgId,
                mCmsLog);
        // Execute synchronously
        task.run();
    }

    private void onEventDeleteChatMessage(CmsRcsObject imapData) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteChatMessage " + imapData);
        }
        String msgId = imapData.getMessageId();
        if (mMessagingLog.isOneToOneChatMessage(msgId)) {
            OneToOneChatMessageDeleteTask task = new OneToOneChatMessageDeleteTask(mChatService,
                    mImService, mLocalContentResolver, msgId, mCmsLog);
            // Call synchronously
            task.run();
        } else {
            GroupChatMessageDeleteTask task = new GroupChatMessageDeleteTask(mChatService,
                    mImService, mLocalContentResolver, msgId, mCmsLog);
            // Execute synchronously
            task.run();
        }
    }

    private void onEventDeleteGroupChat(CmsRcsObject imapData) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteGroupChat " + imapData);
        }
        GroupChatDeleteTask task = new GroupChatDeleteTask(mChatService, mImService,
                mLocalContentResolver, imapData.getChatId(), mCmsLog);
        task.run();
    }

    private void onEventDeleteFileTransfer(CmsRcsObject imapData) {
        if (sLogger.isActivated()) {
            sLogger.debug("onDeleteFileTransfer " + imapData);
        }
        String ftId = imapData.getMessageId();
        if (mMessagingLog.isGroupFileTransfer(ftId)) {
            GroupFileTransferDeleteTask task = new GroupFileTransferDeleteTask(
                    mFileTransferService, mImService, mLocalContentResolver, ftId, mCmsLog);
            // Execute synchronously
            task.run();
        } else {
            OneToOneFileTransferDeleteTask task = new OneToOneFileTransferDeleteTask(
                    mFileTransferService, mImService, mLocalContentResolver, ftId, mCmsLog);
            // Execute synchronously
            task.run();
        }
    }

    @Override
    public String[] onRemoteNewMessage(MessageType messageType, IImapMessage message,
            ContactId remote) throws CmsSyncException, FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.debug("onRemoteNewMessage : " + messageType + " uid=" + message.getUid()
                    + " folder=" + message.getFolder());
        }
        switch (messageType) {
            case SMS:
                String smsMsgId = onNewSmsMessage((ImapSmsMessage) message);
                return new String[] {
                    smsMsgId
                };
            case MMS:
                String mmsMsgId = onNewMmsMessage((ImapMmsMessage) message);
                return new String[] {
                    mmsMsgId
                };
            case CHAT_MESSAGE:
                return onNewChatMessage((ImapChatMessage) message, remote);

            case IMDN:
                return onNewImdnMessage((ImapImdnMessage) message, remote);

            case CPM_SESSION:
                return onNewCpmSessionMessage((ImapCpmSessionMessage) message);

            case FILE_TRANSFER:
                return onNewFileTransferMessage((ImapFileTransferMessage) message, remote);

            case GROUP_STATE:
                return onNewGroupStateMessage((ImapGroupStateMessage) message);

            default:
                throw new CmsSyncMessageNotSupportedException("This messageType is not supported :"
                        + messageType.toString());
        }
    }

    private String onNewSmsMessage(ImapSmsMessage message) {
        if (message.isDeleted()) { // no need to add a deleted message in xms content provider
            return IdGenerator.generateMessageID();
        }
        SmsDataObject smsDataObject = XmsDataObjectFactory.createSmsDataObject(message);
        mXmsLog.addSms(smsDataObject);
        String messageId = smsDataObject.getMessageId();
        if (RcsService.ReadStatus.UNREAD == smsDataObject.getReadStatus()) {
            mCmsService.broadcastNewMessage(MimeType.TEXT_MESSAGE, messageId);
        }
        return messageId;
    }

    private String onNewMmsMessage(ImapMmsMessage message) throws FileAccessException {
        if (message.isDeleted()) { // no need to add a deleted message in xms content provider
            return IdGenerator.generateMessageID();
        }
        MmsDataObject mmsDataObject = XmsDataObjectFactory.createMmsDataObject(mCtx, mSettings,
                message);
        if (Direction.OUTGOING == mmsDataObject.getDirection()) {
            mXmsLog.addOutgoingMms(mmsDataObject);
        } else {
            mXmsLog.addIncomingMms(mmsDataObject);
        }
        String messageId = mmsDataObject.getMessageId();
        if (RcsService.ReadStatus.UNREAD == mmsDataObject.getReadStatus()) {
            mCmsService.broadcastNewMessage(MimeType.MULTIMEDIA_MESSAGE, messageId);
        }
        return messageId;
    }

    private String[] onNewChatMessage(ImapChatMessage imapChatMessage, ContactId remote)
            throws CmsSyncBadStateException {
        boolean singleChat = remote != null;
        if (!singleChat) {
            // this is a GC
            remote = imapChatMessage.getContact();
        }
        ChatMessage chatMessage = createChatMessage(remote, imapChatMessage);
        Direction direction = imapChatMessage.getDirection();
        boolean isSeen = imapChatMessage.isSeen();
        String msgId = chatMessage.getMessageId();
        String chatId = imapChatMessage.getChatId();
        if (!mMessagingLog.isMessagePersisted(msgId)) {
            if (singleChat) {
                onNewOneToOneChatMessage(chatMessage, null, direction, isSeen);
            } else {
                onNewGroupChatMessage(chatMessage, null, chatId, direction, isSeen);
            }
        }
        if (direction == Direction.INCOMING) {
            if (isSeen) {
                mMessagingLog.markMessageAsRead(msgId, NtpTrustedTime.currentTimeMillis());
            } else {
                mChatService.broadcastNewChatMessage(chatMessage);
            }
        }
        if (singleChat) {
            return new String[] {
                msgId
            };
        }
        return new String[] {
                msgId, chatId
        };
    }

    private String onNewOneToOneChatMessage(ChatMessage chatMessage, String remoteSipInstance,
            Direction direction, boolean isSeen) {
        if (direction == Direction.OUTGOING) {
            mMessagingLog.addOutgoingOneToOneChatMessage(chatMessage, Status.SENT,
                    Content.ReasonCode.UNSPECIFIED, 0);
        } else {
            mMessagingLog.addIncomingOneToOneChatMessage(chatMessage, remoteSipInstance, !isSeen);
        }
        return chatMessage.getMessageId();
    }

    private String onNewGroupChatMessage(ChatMessage chatMessage, String sipInstance,
            String chatId, Direction direction, boolean isSeen) throws CmsSyncBadStateException {
        if (direction == Direction.OUTGOING) {
            if (!mMessagingLog.isGroupChatPersisted(chatId)) {
                throw new CmsSyncBadStateException("Group chat message " + chatMessage
                        + " discarded: not group chat!");
            }
            GroupChatPersistedStorageAccessor accessor = new GroupChatPersistedStorageAccessor(
                    chatId, mMessagingLog, mSettings);
            mMessagingLog.addOutgoingGroupChatMessage(chatId, chatMessage, accessor
                    .getParticipants().keySet(), Status.SENT, Content.ReasonCode.UNSPECIFIED);
        } else {
            mMessagingLog.addIncomingGroupChatMessage(chatId, chatMessage, sipInstance, !isSeen);
        }
        return chatMessage.getMessageId();
    }

    private String[] onNewImdnMessage(ImapImdnMessage imapImdnMessage, ContactId remote)
            throws CmsSyncMissingHeaderException {
        String messageId = imapImdnMessage.getImdnDocument().getMsgId();
        boolean single = remote != null;
        if (mMessagingLog.isMessagePersisted(messageId)) {
            String msgId = onNewChatImdn(imapImdnMessage, remote);
            if (single) {
                return new String[] {
                    msgId
                };
            } else {
                String chatId = mMessagingLog.getMessageChatId(messageId);
                return new String[] {
                        msgId, chatId
                };
            }
        }
        if (mMessagingLog.isFileTransfer(messageId)) {
            String ftId = onNewFileTransferImdn(imapImdnMessage, remote);
            if (single) {
                return new String[] {
                    ftId
                };
            } else {
                String chatId = mMessagingLog.getFileTransferChatId(ftId);
                return new String[] {
                        ftId, chatId
                };
            }
        }
        if (sLogger.isActivated()) {
            sLogger.debug("No message (chat or file transfer) found for IMDN with id: " + messageId);
        }
        return new String[] {
            imapImdnMessage.getImdnId()
        };
    }

    private String onNewChatImdn(ImapImdnMessage imapImdnMessage, ContactId remote) {
        boolean logActivated = sLogger.isActivated();
        String imdnId = imapImdnMessage.getImdnId();
        if (logActivated) {
            sLogger.debug("onNewChatImdn: " + imdnId);
        }
        ImdnDocument imdn = imapImdnMessage.getImdnDocument();
        if (Direction.OUTGOING == imapImdnMessage.getDirection()) {
            switch (imdn.getStatus()) {
                case DELIVERED:
                    break;

                case DISPLAYED:
                    mMessagingLog.markMessageAsRead(imdn.getMsgId(), imdn.getDateTime());
                    break;

                default:
                    /*
                     * This should never occur because only delivered or displayed notifications are
                     * stored on CMS. Message is stored on CMS only once delivered.
                     */
                    if (logActivated) {
                        sLogger.warn("onNewChatImdnMessage : failed to process IMDN");
                    }
            }
        } else {
            boolean singleChat = remote != null;
            if (singleChat) {
                mChatService.onOneToOneMessageDeliveryStatusReceived(remote, imdn);
            } else {
                // this is a GC: get remote from CPIM
                remote = imapImdnMessage.getContact();
                String chatId = mMessagingLog.getMessageChatId(imdn.getMsgId());
                if (chatId != null) {
                    mChatService.getOrCreateGroupChat(chatId).onMessageDeliveryStatusReceived(
                            remote, imdn, imdnId);
                }
            }
        }
        return imdnId;
    }

    private String onNewFileTransferImdn(ImapImdnMessage imapImdnMessage, ContactId remote) {
        boolean logActivated = sLogger.isActivated();
        String imdnId = imapImdnMessage.getImdnId();
        if (logActivated) {
            sLogger.debug("onNewFileTransferImdn: " + imdnId);
        }
        ImdnDocument imdnDoc = imapImdnMessage.getImdnDocument();
        String fileTransferId = imdnDoc.getMsgId();
        if (Direction.OUTGOING == imapImdnMessage.getDirection()) {
            switch (imdnDoc.getStatus()) {
                case DELIVERED:
                    break;

                case DISPLAYED:
                    mMessagingLog.markFileTransferAsRead(fileTransferId, imdnDoc.getDateTime());
                    break;

                default:
                    /*
                     * This should never occur because only delivered or displayed notifications are
                     * stored on CMS. Message is stored on CMS only once delivered.
                     */
                    if (logActivated) {
                        sLogger.warn("onNewFileTransferImdn : failed to process IMDN");
                    }
            }
        } else {
            boolean singleChat = remote != null;
            if (singleChat) {
                mFileTransferService.receiveOneToOneFileDeliveryStatus(imdnDoc, remote);
            } else {
                String chatId = mMessagingLog.getFileTransferChatId(fileTransferId);
                if (chatId != null) {
                    remote = imapImdnMessage.getContact();
                    mFileTransferService.receiveGroupFileDeliveryStatus(chatId, imdnDoc, remote);
                }
            }
        }
        return imdnId;
    }

    private String[] onNewFileTransferMessage(ImapFileTransferMessage imapFileTransferMessage,
            ContactId remote) throws CmsSyncBadStateException {
        String fileTransferId = imapFileTransferMessage.getImdnId();
        String chatId = imapFileTransferMessage.getChatId();
        FileTransferHttpInfoDocument ftInfo = imapFileTransferMessage.getInfoDocument();
        long timestamp = DateUtils.decodeDate(imapFileTransferMessage.getCpimMessage().getHeader(
                Constants.HEADER_DATE_TIME));
        FileTransfer.State state = FileTransfer.State.TRANSFERRED;
        FileTransfer.ReasonCode reasonCode = FileTransfer.ReasonCode.UNSPECIFIED;
        boolean singleChat = remote != null;
        if (mMessagingLog.isFileTransfer(fileTransferId)) {
            // file transfer is already persisted.
            if (singleChat) {
                return new String[] {
                    fileTransferId
                };
            } else {
                return new String[] {
                        fileTransferId, chatId
                };
            }
        }
        if (singleChat) {
            onNewOneToOneFileTransferMessage(fileTransferId, remote,
                    imapFileTransferMessage.getDirection(), timestamp, state, reasonCode, ftInfo,
                    imapFileTransferMessage.isSeen());
            return new String[] {
                fileTransferId
            };
        } else {
            remote = imapFileTransferMessage.getContact();
            onNewGroupFileTransferMessage(fileTransferId, remote, chatId,
                    imapFileTransferMessage.getDirection(), timestamp, state, reasonCode, ftInfo,
                    imapFileTransferMessage.isSeen());
            return new String[] {
                    fileTransferId, chatId
            };
        }
    }

    private void onNewOneToOneFileTransferMessage(String fileTransferId, ContactId contact,
            Direction direction, long timestamp, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, FileTransferHttpInfoDocument ftInfo, boolean seen) {
        mMessagingLog.addOneToOneFileTransferOnSecondaryDevice(fileTransferId, contact, direction,
                ftInfo.getUri(), ftInfo.getLocalMmContent(), state, reasonCode, timestamp,
                timestamp, ftInfo.getExpiration(), ftInfo.getFileDisposition(), seen);
    }

    private void onNewGroupFileTransferMessage(String fileTransferId, ContactId contact,
            String chatId, Direction direction, long timestamp, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, FileTransferHttpInfoDocument ftInfo, boolean seen)
            throws CmsSyncBadStateException {
        if (Direction.INCOMING == direction) {
            mMessagingLog
                    .addIncomingGroupFileTransferOnSecondaryDevice(fileTransferId, chatId, contact,
                            ftInfo.getUri(), ftInfo.getLocalMmContent(), state, reasonCode,
                            timestamp, timestamp, ftInfo.getExpiration(),
                            ftInfo.getFileDisposition(), seen);
        } else {
            if (!mMessagingLog.isGroupChatPersisted(chatId)) {
                throw new CmsSyncBadStateException("Group file transfer " + fileTransferId
                        + " discarded: not group chat!");
            }
            GroupChatPersistedStorageAccessor gcAccessor = new GroupChatPersistedStorageAccessor(
                    chatId, mMessagingLog, mSettings);
            mMessagingLog.addOutgoingGroupFileTransferOnSecondaryDevice(fileTransferId, chatId,
                    ftInfo.getUri(), ftInfo.getLocalMmContent(), gcAccessor.getParticipants()
                            .keySet(), state, reasonCode, timestamp, timestamp, ftInfo
                            .getExpiration(), ftInfo.getFileDisposition());
        }
    }

    private String[] onNewCpmSessionMessage(ImapCpmSessionMessage imapCpmSessionMessage) {
        String chatId = imapCpmSessionMessage.getChatId();
        Map<ContactId, ParticipantStatus> participants = new HashMap<>();
        for (ContactId contact : imapCpmSessionMessage.getParticipants()) {
            participants.put(contact, ParticipantStatus.CONNECTED);
        }
        /*
         * Insert Group Chat in the state ABORTED (BY_INACTIVITY) so that session will not be
         * started autonomously.
         */
        if (!mMessagingLog.isGroupChatPersisted(chatId)) {
            mMessagingLog.addGroupChat(chatId, null, imapCpmSessionMessage.getSubject(),
                    participants, GroupChat.State.ABORTED,
                    GroupChat.ReasonCode.ABORTED_BY_INACTIVITY,
                    imapCpmSessionMessage.getDirection(), imapCpmSessionMessage.getTimestamp());
        }
        return new String[] {
                imapCpmSessionMessage.getMessageId(), chatId
        };
    }

    private String[] onNewGroupStateMessage(ImapGroupStateMessage imapGroupStateMessage) {
        String chatId = imapGroupStateMessage.getChatId();
        if (mMessagingLog.isGroupChatPersisted(chatId)) {
            mMessagingLog.setGroupChatRejoinId(chatId, imapGroupStateMessage.getRejoinId(), false);
            Map<ContactId, ParticipantStatus> participants = new HashMap<>();
            for (ContactId contact : imapGroupStateMessage.getParticipants()) {
                participants.put(contact, ParticipantStatus.CONNECTED);
            }
            mMessagingLog.setGroupChatParticipants(chatId, participants);
        }
        return new String[] {
                imapGroupStateMessage.getMessageId(), chatId
        };
    }

    @Override
    public CmsObject searchLocalMessage(MessageType messageType, IImapMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        switch (messageType) {
            case SMS:
                return searchLocalSmsMessage((ImapSmsMessage) message);

            case MMS:
                return searchLocalMmsMessage((ImapMmsMessage) message);

            case MESSAGE_CPIM:
                return searchLocalCpimMessage((ImapCpimMessage) message);

            case CPM_SESSION:
                return searchLocalCpmSessionMessage((ImapCpmSessionMessage) message);

            case GROUP_STATE:
                return searchLocalGroupStateMessage((ImapGroupStateMessage) message);

            default:
                if (sLogger.isActivated()) {
                    sLogger.debug("This messageType is not supported :" + messageType.toString());
                }
                return null;
        }
    }

    private CmsXmsObject searchLocalSmsMessage(ImapSmsMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        boolean logActive = sLogger.isActivated();
        if (logActive) {
            sLogger.debug("searchLocalSmsMessage " + message);
        }
        String folder = message.getFolder();
        Integer uid = message.getUid();
        // check if an entry already exists in imapData provider
        CmsObject cmsObject = mCmsLog.getMessage(folder, uid);
        String msgId = message.getHeader(Constants.HEADER_IMDN_MESSAGE_ID);
        if (msgId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_IMDN_MESSAGE_ID
                    + " IMAP header is missing");
        }
        if (cmsObject != null) {
            if (!msgId.equals(cmsObject.getMessageId())) {
                if (logActive) {
                    sLogger.warn("searchLocalSmsMessage messageID does not match " + msgId);
                }
            }
            if (cmsObject instanceof CmsXmsObject) {
                return (CmsXmsObject) cmsObject;

            } else {
                if (logActive) {
                    sLogger.warn("searchLocalSmsMessage messageID " + msgId
                            + " matches but not type!");
                }
                return null;
            }
        }
        /*
         * get messages from provider with contact, direction and correlator fields messages are
         * sorted by Date DESC (more recent first).
         */
        SmsDataObject smsData = XmsDataObjectFactory.createSmsDataObject(message);
        String correlator = smsData.getCorrelator();
        ContactId contact = smsData.getContact();
        Direction dir = smsData.getDirection();
        if (logActive) {
            sLogger.debug("searchLocalSmsMessage correlator='" + correlator + "' dir=" + dir);
        }
        List<String> ids = mXmsLog.getMessageIdsMatchingCorrelator(contact, dir, correlator);
        if (logActive) {
            sLogger.debug("searchLocalSmsMessage matching IDs=" + Arrays.toString(ids.toArray()));
        }
        // take the first message which is not synchronized with CMS server (have no uid)
        for (String id : ids) {
            CmsXmsObject xmsObject = mCmsLog.getSmsData(contact, id);
            if (xmsObject != null && xmsObject.getUid() == null) {
                if (logActive) {
                    sLogger.debug("searchLocalSmsMessage select 1rst=" + xmsObject);
                }
                String cmsId = xmsObject.getMessageId();
                if (!msgId.equals(cmsId)) {
                    /*
                     * If SMS is pushed by SMSC then message-id should be different from the local
                     * one. Update the local SMS message ID to reflect CMS.
                     */
                    if (mCmsLog.updateSmsMessageId(contact, cmsId, msgId)) {
                        mXmsLog.updateSmsMessageId(contact, cmsId, msgId);
                    } else {
                        if (logActive) {
                            sLogger.warn("searchLocalSmsMessage: failed to update message ID "
                                    + cmsId);
                        }
                    }
                    xmsObject = new CmsXmsObject(MessageType.SMS, folder, msgId, null,
                            xmsObject.getPushStatus(), xmsObject.getReadStatus(),
                            xmsObject.getDeleteStatus(), xmsObject.getNativeProviderId());
                }
                return xmsObject;
            }
        }
        if (logActive) {
            sLogger.warn("searchLocalSmsMessage no matching record in imap log");
        }
        return null;
    }

    private CmsXmsObject searchLocalMmsMessage(ImapMmsMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        String mmsId = message.getHeader(Constants.HEADER_MESSAGE_CORRELATOR);
        if (mmsId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_MESSAGE_CORRELATOR
                    + " IMAP header is missing");
        }
        return mCmsLog.getMmsData(message.getContact(), mmsId);
    }

    private CmsRcsObject searchLocalCpimMessage(ImapCpimMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        String messageId = message.getHeader(Constants.HEADER_IMDN_MESSAGE_ID);
        if (messageId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_IMDN_MESSAGE_ID
                    + " IMAP header is missing");
        }
        return mCmsLog.getChatOrImdnOrFileTransferData(messageId);
    }

    private CmsRcsObject searchLocalGroupStateMessage(ImapGroupStateMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        String messageId = message.getHeader(Constants.HEADER_IMDN_MESSAGE_ID);
        if (messageId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_IMDN_MESSAGE_ID
                    + " IMAP header is missing");
        }
        return mCmsLog.getGroupChatObjectData(messageId);
    }

    private CmsRcsObject searchLocalCpmSessionMessage(ImapCpmSessionMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        String messageId = message.getHeader(Constants.HEADER_IMDN_MESSAGE_ID);
        if (messageId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_IMDN_MESSAGE_ID
                    + " IMAP header is missing");
        }
        return mCmsLog.getCpmSessionData(messageId);
    }

    private ChatMessage createChatMessage(ContactId contact, ImapChatMessage imapChatMessage) {
        long timestamp = DateUtils.decodeDate(imapChatMessage.getCpimMessage().getHeader(
                Constants.HEADER_DATE_TIME));
        return ChatUtils.createChatMessage(
                imapChatMessage.getCpimMessage().getHeader("imdn.Message-ID"),
                Message.MimeType.TEXT_MESSAGE, imapChatMessage.getText(), contact, null, timestamp,
                timestamp);
    }
}

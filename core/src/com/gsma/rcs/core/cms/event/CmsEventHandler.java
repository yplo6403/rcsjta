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

import static com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession.isFileCapacityAcceptable;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncImdnFormatException;
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
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.FileTransferData.DownloadState;
import com.gsma.rcs.provider.messaging.GroupChatPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObjectFactory;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
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
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmsEventHandler implements CmsEventListener {

    private static final Logger sLogger = Logger.getLogger(CmsEventHandler.class.getSimpleName());
    private final Context mCtx;
    private final XmsLog mXmsLog;
    private final CmsLog mCmsLog;
    private final MessagingLog mMessagingLog;
    private final ChatServiceImpl mChatService;
    private final FileTransferServiceImpl mFileTransferService;
    private final RcsSettings mSettings;
    private final CmsServiceImpl mCmsService;
    private final InstantMessagingService mImService;

    /**
     * Default constructor
     *
     * @param context the context
     * @param cmsLog the CMS log accessor
     * @param xmsLog the XMS log accessor
     * @param messagingLog the messaging log accessor
     * @param chatService the chat service impl
     * @param fileTransferService the file transfer service impl
     * @param cmsService the CMS service impl
     * @param settings the RCS settings accessor
     * @param imService the IMS service
     */
    public CmsEventHandler(Context context, CmsLog cmsLog, XmsLog xmsLog,
            MessagingLog messagingLog, ChatServiceImpl chatService, FileTransferServiceImpl fileTransferService, CmsServiceImpl cmsService,
            RcsSettings settings, InstantMessagingService imService) {
        mCtx = context;
        mXmsLog = xmsLog;
        mCmsLog = cmsLog;
        mMessagingLog = messagingLog;
        mChatService = chatService;
        mFileTransferService = fileTransferService;
        mCmsService = cmsService;
        mSettings = settings;
        mImService = imService;
    }



    @Override
    public void onRemoteReadEvent(CmsObject imapData) {
        if (sLogger.isActivated()) {
            sLogger.debug("onRemoteReadEvent");
        }
        MessageType messageType = imapData.getMessageType();
        if (MessageType.SMS == messageType
                || MessageType.MMS == messageType) {
            onReadXmsMessage(imapData);
        } else if (MessageType.CHAT_MESSAGE == messageType) {
            onReadChatMessage(imapData);
        } else if (MessageType.FILE_TRANSFER == messageType) {
            onReadFileTransfer(imapData);
        }
    }

    private void onReadXmsMessage(CmsObject imapData) {
        if (sLogger.isActivated()) {
            sLogger.debug("onReadXmsMessage");
        }
        String messageId = imapData.getMessageId();
        mXmsLog.markMessageAsRead(messageId);
        String number = mXmsLog.getContact(messageId);
        if (number == null) { // message is no more present in db
            return;
        }
        ContactId contact = ContactUtil.createContactIdFromTrustedData(number);
        String mimeType = MessageType.SMS == imapData.getMessageType() ? MimeType.TEXT_MESSAGE
                : MimeType.MULTIMEDIA_MESSAGE;
        mCmsService.broadcastMessageStateChanged(contact, mimeType, messageId, State.DISPLAYED,
                ReasonCode.UNSPECIFIED);
    }

    private void onReadChatMessage(CmsObject imapData) {
        String msgId = imapData.getMessageId();
        if (mMessagingLog.isMessagePersisted(msgId)) {
            mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.RECEIVED,
                    Content.ReasonCode.UNSPECIFIED);
            mMessagingLog.markMessageAsRead(msgId);
        }
    }

    private void onReadFileTransfer(CmsObject imapData) {
        String msgId = imapData.getMessageId();
        if (mMessagingLog.isFileTransfer(msgId)) {
            mMessagingLog.markFileTransferAsRead(msgId);
        }
    }

    @Override
    public void onRemoteDeleteEvent(CmsObject imapData) {
        if (sLogger.isActivated()) {
            sLogger.debug("onRemoteDeleteEvent");
        }
        MessageType messageType = imapData.getMessageType();
        if (MessageType.SMS == messageType
                || MessageType.MMS == messageType) {
            onDeleteXmsMessage(imapData);
        } else if (MessageType.CHAT_MESSAGE == messageType) {
            onDeleteChatMessage(imapData);
        } else if (MessageType.FILE_TRANSFER == messageType) {
            onDeleteFileTransfer(imapData);
        } else if (MessageType.CPM_SESSION == messageType) {
            onDeleteGroupChat(imapData);
        }
    }

    private void onDeleteXmsMessage(CmsObject imapData) {
        mCmsService.deleteXmsMessageById(imapData.getMessageId());
    }

    private void onDeleteChatMessage(CmsObject imapData) {
        mChatService.deleteMessage(imapData.getMessageId());
    }

    private void onDeleteGroupChat(CmsObject imapData) {
        mChatService.deleteGroupChat(imapData.getMessageId());
    }

    private void onDeleteFileTransfer(CmsObject imapData) {
        try {
            mFileTransferService.deleteFileTransfer(imapData.getMessageId());
        } catch (RemoteException e) {
            if (sLogger.isActivated()) {
                sLogger.error("onDeleteFileTransfer : " + imapData.getMessageId(), e);
            }
        }
    }

    @Override
    public String onRemoteNewMessage(MessageType messageType, IImapMessage message)
            throws CmsSyncException, FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.debug("onRemoteNewMessage : " + messageType);
        }
        if (MessageType.SMS == messageType) {
            return onNewSmsMessage((ImapSmsMessage) message);
        } else if (MessageType.MMS == messageType) {
            return onNewMmsMessage((ImapMmsMessage) message);
        } else if (MessageType.CHAT_MESSAGE == messageType) {
            return onNewChatMessage((ImapChatMessage) message);
        } else if (MessageType.IMDN == messageType) {
            return onNewImdnMessage((ImapImdnMessage) message);
        } else if (MessageType.FILE_TRANSFER == messageType) {
            return onNewFileTransferMessage((ImapFileTransferMessage) message);
        } else if (MessageType.GROUP_STATE == messageType) {
            return onNewGroupStateMessage((ImapGroupStateMessage) message);
        } else if (MessageType.CPM_SESSION == messageType) {
            return onNewCpmSessionMessage((ImapCpmSessionMessage) message);
        }

        throw new CmsSyncMessageNotSupportedException("This messageType is not supported :"
                + messageType.toString());
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

    private String onNewChatMessage(ImapChatMessage imapChatMessage) {
        ChatMessage chatMessage = createTextMessage(imapChatMessage.getContact(), imapChatMessage);
        Direction direction = imapChatMessage.getDirection();
        boolean isSeen = imapChatMessage.isSeen();

        String msgId = chatMessage.getMessageId();
        if(!mMessagingLog.isMessagePersisted(msgId)){
            if(imapChatMessage.isOneToOne()){
                onNewOneToOneChatMessage(chatMessage, direction, isSeen);
            } else{
                onNewGroupChatMessage(chatMessage, imapChatMessage.getChatId(), direction, isSeen);
            }
        }

        if(direction == Direction.INCOMING){
            if(isSeen){
                mMessagingLog.markMessageAsRead(msgId);
            }
            else{
                mChatService.broadcastNewChatMessage(chatMessage);
            }
        }

        return chatMessage.getMessageId();
    }

    private String onNewOneToOneChatMessage(ChatMessage chatMessage, Direction direction, boolean isSeen) {
        if (direction == Direction.OUTGOING) {
            mMessagingLog.addOutgoingOneToOneChatMessage(chatMessage, Status.SENT,
                    Content.ReasonCode.UNSPECIFIED, 0);
        } else {
            mMessagingLog.addIncomingOneToOneChatMessage(chatMessage, !isSeen);
        }
        return chatMessage.getMessageId();
    }

    private String onNewGroupChatMessage(ChatMessage chatMessage, String chatId, Direction direction, boolean isSeen) {

        if (direction == Direction.OUTGOING) {
                GroupChatPersistedStorageAccessor accessor = new GroupChatPersistedStorageAccessor(
                        chatId, mMessagingLog, mSettings);
                mMessagingLog.addOutgoingGroupChatMessage(chatId, chatMessage, accessor
                        .getParticipants().keySet(), Status.SENT, Content.ReasonCode.UNSPECIFIED);
        } else {
                mMessagingLog.addIncomingGroupChatMessage(chatId, chatMessage, !isSeen);
        }
        return chatMessage.getMessageId();
    }

    private String onNewImdnMessage(ImapImdnMessage imapImdnMessage) throws CmsSyncImdnFormatException {

        String messageId = imapImdnMessage.getImdnDocument().getMsgId();

        if (mMessagingLog.isMessagePersisted(messageId)) {
            return onNewChatImdn(imapImdnMessage);
        }

        if(mMessagingLog.isFileTransfer(messageId)){
            return onNewFileTransferImdn(imapImdnMessage);
        }

        if (sLogger.isActivated()) {
            sLogger.debug("No message (chat or file transfer) found for IMDN with id : " + messageId);
        }
        return imapImdnMessage.getImdnId();
    }

    private String onNewChatImdn(ImapImdnMessage imapImdnMessage)
            throws CmsSyncImdnFormatException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("onNewChatImdnMessage : " + imapImdnMessage.getImdnId());
        }

        if (Direction.OUTGOING == imapImdnMessage.getDirection()) {
            return imapImdnMessage.getImdnId();
        }

        if (imapImdnMessage.isOneToOne()) {
            mChatService.onOneToOneMessageDeliveryStatusReceived(imapImdnMessage.getContact(),
                    imapImdnMessage.getImdnDocument());
        } else {
            String chatId = mMessagingLog.getMessageChatId(imapImdnMessage.getImdnDocument()
                    .getMsgId());
            if (chatId != null) {
                mChatService.getOrCreateGroupChat(chatId).onMessageDeliveryStatusReceived(
                        imapImdnMessage.getContact(), imapImdnMessage.getImdnDocument(),
                        imapImdnMessage.getImdnId());
            }
        }
        return imapImdnMessage.getImdnId();
    }

    private String onNewFileTransferImdn(ImapImdnMessage imapImdnMessage)
            throws CmsSyncImdnFormatException {

        if (sLogger.isActivated()) {
            sLogger.debug("onNewFileTransferImdnMessage : " + imapImdnMessage.getImdnId());
        }

        if (Direction.OUTGOING == imapImdnMessage.getDirection()) {
            return imapImdnMessage.getImdnId();
        }

        String fileTransferId = imapImdnMessage.getImdnDocument().getMsgId();
        if (imapImdnMessage.isOneToOne()) {
            mFileTransferService.receiveOneToOneFileDeliveryStatus(imapImdnMessage.getImdnDocument(), imapImdnMessage.getContact());
        } else {
            String chatId = mMessagingLog.getFileTransferChatId(fileTransferId);
            if (chatId != null) {
                mFileTransferService.receiveGroupFileDeliveryStatus(chatId, imapImdnMessage.getImdnDocument(), imapImdnMessage.getContact());
            }
        }
        return imapImdnMessage.getImdnId();
    }

    private String onNewFileTransferMessage(ImapFileTransferMessage imapFileTransferMessage)
            throws CmsSyncImdnFormatException {

        boolean logActivated = sLogger.isActivated();
        String fileTransferId = imapFileTransferMessage.getImdnId();
        FileTransferHttpInfoDocument ftInfo = imapFileTransferMessage
                .getFileTransferHttpInfoDocument();
        Direction direction = imapFileTransferMessage.getDirection();
        boolean isSeen = imapFileTransferMessage.isSeen();
        long timestamp = DateUtils.decodeDate(imapFileTransferMessage.getCpimMessage().getHeader(
                Constants.HEADER_DATE_TIME));

        FileTransfer.State state = FileTransfer.State.TRANSFERRED;
        FileTransfer.ReasonCode reasonCode = FileTransfer.ReasonCode.UNSPECIFIED;

        FileSharingError error = isFileCapacityAcceptable(ftInfo.getSize(), mSettings);
        if (error != null) {
            int errorCode = error.getErrorCode();
            state = FileTransfer.State.FAILED;
            switch (errorCode) {
                case FileSharingError.MEDIA_SIZE_TOO_BIG:
                    if (logActivated) {
                        sLogger.warn("Rejecting file transfer, media size too big : "
                                + ftInfo.getSize());
                    }
                    reasonCode = FileTransfer.ReasonCode.REJECTED_MAX_SIZE;
                    break;
                case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                    if (logActivated) {
                        sLogger.warn("Rejecting file transfer, not enough space in local storage");
                    }
                    reasonCode = FileTransfer.ReasonCode.REJECTED_LOW_SPACE;
                    break;
            }
        }
        if(!mMessagingLog.isFileTransfer(fileTransferId)){
            if (imapFileTransferMessage.isOneToOne()) {
                onNewOneToOneFileTransferMessage(fileTransferId, imapFileTransferMessage.getContact(),
                        direction, timestamp, state, reasonCode, ftInfo);
            } else {
                onNewGroupFileTransferMessage(fileTransferId, imapFileTransferMessage.getContact(),
                        imapFileTransferMessage.getChatId(), direction, timestamp, state, reasonCode,
                        ftInfo);
            }
        }
        mMessagingLog.setFileDownloadAddress(fileTransferId, ftInfo.getUri());
        mMessagingLog.setFileTransferDownloadStateAndReasonCode(fileTransferId, DownloadState.QUEUED, FileTransfer.ReasonCode.UNSPECIFIED);

        if (direction == Direction.INCOMING) {
            if (isSeen) {
                mMessagingLog.markFileTransferAsRead(fileTransferId);
            } else {
                // TODO FGI : broadcast invitation for new file transfer message
            }
        }
        return fileTransferId;
    }

    private void onNewOneToOneFileTransferMessage(String fileTransferId, ContactId contact,
            Direction direction, long timestamp, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, FileTransferHttpInfoDocument ftInfo)
            throws CmsSyncImdnFormatException {
        mMessagingLog.addOneToOneFileTransfer(fileTransferId, contact, direction,
                ftInfo.getLocalMmContent(), null, state, reasonCode, timestamp, timestamp,
                ftInfo.getExpiration(), FileTransferData.UNKNOWN_EXPIRATION);

    }

    private void onNewGroupFileTransferMessage(String fileTransferId, ContactId contact,
            String chatId, Direction direction, long timestamp, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode, FileTransferHttpInfoDocument ftInfo)
            throws CmsSyncImdnFormatException {

        if (direction == Direction.INCOMING) {
            mMessagingLog.addIncomingGroupFileTransfer(fileTransferId, chatId, contact,
                    ftInfo.getLocalMmContent(), null, state, reasonCode, timestamp, timestamp,
                    ftInfo.getExpiration(), FileTransferData.UNKNOWN_EXPIRATION);
        } else {
            GroupChatPersistedStorageAccessor gcAccessor = new GroupChatPersistedStorageAccessor(chatId, mMessagingLog, mSettings);
            mMessagingLog.addOutgoingGroupFileTransfer(fileTransferId, chatId,
                    ftInfo.getLocalMmContent(), null, gcAccessor.getParticipants().keySet(), state,
                    reasonCode, timestamp, timestamp);
            mMessagingLog.setFileTransferFileExpiration(fileTransferId, ftInfo.getExpiration());
        }
    }

    private String onNewCpmSessionMessage(ImapCpmSessionMessage imapCpmSessionMessage) {
        String chatId = imapCpmSessionMessage.getChatId();

        Map<ContactId, ParticipantStatus> participants = new HashMap<>();
        for (ContactId contact : imapCpmSessionMessage.getParticipants()) {
            participants.put(contact, ParticipantStatus.CONNECTED);
        }
        if (!mMessagingLog.isGroupChatPersisted(chatId)) {
            mMessagingLog.addGroupChat(chatId, null, imapCpmSessionMessage.getSubject(),
                    participants, GroupChat.State.STARTED, GroupChat.ReasonCode.UNSPECIFIED,
                    imapCpmSessionMessage.getDirection(), imapCpmSessionMessage.getTimestamp());
        }
        return chatId;
    }

    private String onNewGroupStateMessage(ImapGroupStateMessage imapGroupStateMessage) {
        String chatId = imapGroupStateMessage.getChatId();
        Map<ContactId, ParticipantStatus> participants = new HashMap<>();
        for (ContactId contact : imapGroupStateMessage.getParticipants()) {
            participants.put(contact, ParticipantStatus.CONNECTED);
        }
        if (mMessagingLog.isGroupChatPersisted(chatId)) {
            mMessagingLog.setGroupChatRejoinId(chatId, imapGroupStateMessage.getRejoinId(), false);
            mMessagingLog.setGroupChatParticipants(chatId, participants);
        }
        return chatId;
    }

    @Override
    public CmsObject searchLocalMessage(MessageType messageType, IImapMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        // check if an entry already exists in imapData provider
        CmsObject cmsObject = mCmsLog.getMessage(message.getFolder(), message.getUid());
        if (cmsObject != null) {
            return cmsObject;
        }
        if (MessageType.SMS == messageType) {
            return searchLocalSmsMessage((ImapSmsMessage) message);
        } else if (MessageType.MMS == messageType) {
            return searchLocalMmsMessage((ImapMmsMessage) message);
        } else if (MessageType.MESSAGE_CPIM == messageType) {
            return searchLocalCpimMessage((ImapCpimMessage) message);
        } else if (MessageType.GROUP_STATE == messageType) {
            return searchLocalGroupStateMessage((ImapGroupStateMessage) message);
        } else if (MessageType.CPM_SESSION == messageType) {
            return searchLocalCpmSessionMessage((ImapCpmSessionMessage) message);
        }

        if (sLogger.isActivated()) {
            sLogger.debug("This messageType is not supported :" + messageType.toString());
        }
        return null;
    }

    private CmsObject searchLocalSmsMessage(ImapSmsMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        /*
         * get messages from provider with contact, direction and correlator fields messages are
         * sorted by Date DESC (more recent first).
         */
        SmsDataObject smsData = XmsDataObjectFactory.createSmsDataObject(message);
        List<String> ids = mXmsLog.getMessageIds(smsData.getContact().toString(),
                smsData.getDirection(), smsData.getCorrelator());
        // take the first message which s not synchronized with CMS server (have no uid)
        for (String id : ids) {
            CmsObject cmsObject = mCmsLog.getSmsData(id);
            if (cmsObject != null && cmsObject.getUid() == null) {
                return cmsObject;
            }
        }
        return null;
    }

    private CmsObject searchLocalMmsMessage(ImapMmsMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        String mmsId = message.getHeader(Constants.HEADER_MESSAGE_ID);
        if (mmsId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_MESSAGE_ID
                    + " IMAP header is missing");
        }
        Cursor cursor = null;
        try {
            cursor = mXmsLog.getMmsMessage(mmsId);
            if (!cursor.moveToNext()) {
                return null;
            }
            String messageId = cursor.getString(cursor
                    .getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
            if (messageId != null) {
                return mCmsLog.getMmsData(messageId);
            }
        } finally {
            CursorUtil.close(cursor);
        }
        return null;
    }

    private CmsObject searchLocalCpimMessage(ImapCpimMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        String messageId = message.getHeader(Constants.HEADER_IMDN_MESSAGE_ID);
        if (messageId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_IMDN_MESSAGE_ID
                    + " IMAP header is missing");
        }
        return mCmsLog.getChatOrImdnOrFileTransferData(messageId);
    }

    private CmsObject searchLocalGroupStateMessage(ImapGroupStateMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        String messageId = message.getHeader(Constants.HEADER_CONTRIBUTION_ID);
        if (messageId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID
                    + " IMAP header is missing");
        }
        return mCmsLog.getGroupChatObjectData(messageId);
    }

    private CmsObject searchLocalCpmSessionMessage(ImapCpmSessionMessage message)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {
        String messageId = message.getHeader(Constants.HEADER_CONTRIBUTION_ID);
        if (messageId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID
                    + " IMAP header is missing");
        }
        return mCmsLog.getCpmSessionData(messageId);
    }

    private ChatMessage createTextMessage(ContactId contact, ImapChatMessage imapChatMessage) {
        long timestamp = DateUtils.decodeDate(imapChatMessage.getCpimMessage().getHeader(
                Constants.HEADER_DATE_TIME));
        return ChatUtils.createChatMessage(
                imapChatMessage.getCpimMessage().getHeader("imdn.Message-ID"),
                Message.MimeType.TEXT_MESSAGE, imapChatMessage.getText(), contact, null, timestamp,
                timestamp);
    }
}

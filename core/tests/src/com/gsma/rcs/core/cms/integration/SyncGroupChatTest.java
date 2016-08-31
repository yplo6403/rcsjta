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
 ******************************************************************************/

package com.gsma.rcs.core.cms.integration;

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.protocol.message.ImapCpmSessionMessageImpl;
import com.gsma.rcs.core.cms.protocol.message.ImapGroupChatMessageImpl;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.smsmms.SmsMmsLog;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Philippe LEMORDANT
 */
public class SyncGroupChatTest extends AndroidTestCase {

    private ImapServiceHandler mImapServiceHandler;
    private BasicSyncStrategy mSyncStrategy;
    private MessagingLog mMessagingLog;
    private CmsLog mCmsLog;
    private ImapCmsUtilTest mImapCmsUtilTest;
    private ImapGroupChatMessageImpl mIncomingGroupChatMessage;
    private ImapGroupChatMessageImpl mOutgoingGroupChatMessage;
    private ImapCpmSessionMessageImpl mImapCpmSessionMessage;
    private SyncLogUtilTest mLogUtilTest;
    private ArrayList<ContactId> mParticipants;

    protected void setUp() throws Exception {
        super.setUp();
        RcsSettings mSettings = RcsSettingsMock.getMockSettings(mContext);
        AndroidFactory.setApplicationContext(mContext, mSettings);
        mCmsLog = CmsLog.getInstance(mContext);
        SmsMmsLog smsMmsLog = SmsMmsLog.getInstance(mContext, mContext.getContentResolver());
        LocalContentResolver mLocalContentResolver = new LocalContentResolver(mContext);
        XmsLog xmsLog = XmsLog.getInstance(mContext, mSettings, mLocalContentResolver);
        mMessagingLog = MessagingLog.getInstance(mLocalContentResolver, mSettings);
        CmsSessionController cmsSessionCtrl = new CmsSessionController(mContext, null, mSettings,
                mLocalContentResolver, xmsLog, mMessagingLog, mCmsLog, null);
        InstantMessagingService imService = new InstantMessagingService(null, mSettings, null,
                mMessagingLog, null, mLocalContentResolver, mContext, null, cmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, mMessagingLog, null,
                mSettings, null, cmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, mMessagingLog, mSettings, null, mContext, cmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, xmsLog, smsMmsLog);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, cmsSessionCtrl, chatService,
                fileTransferService, imService, xmsLog, xmsManager, mLocalContentResolver,
                smsMmsLog);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, mLocalContentResolver,
                mCmsLog, xmsLog, mMessagingLog, chatService, fileTransferService, cmsServiceImpl,
                imService, mSettings);
        LocalStorage localStorage = new LocalStorage(mSettings, mCmsLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(mSettings);
        BasicImapService imapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(mContext, mSettings, imapService, localStorage,
                xmsLog, mCmsLog);
        imapService.init();
        mImapCmsUtilTest = new ImapCmsUtilTest(mContext, mSettings, imapService, mCmsLog);

        long timestamp = 24L * 3600L * 1000L + 1001L;
        ContactId contact = ContactUtil.createContactIdFromTrustedData("+330000001");
        mIncomingGroupChatMessage = new ImapGroupChatMessageImpl(IdGenerator.generateMessageID(),
                contact, "subject", RcsService.Direction.INCOMING, timestamp, "hello world", false,
                false);

        mOutgoingGroupChatMessage = new ImapGroupChatMessageImpl(IdGenerator.generateMessageID(),
                contact, "subject", RcsService.Direction.OUTGOING, timestamp, "hello world", true,
                false);
        mParticipants = new ArrayList<>();
        ContactId contact1 = ContactUtil.createContactIdFromTrustedData("+330000002");
        mParticipants.add(contact1);
        ContactId contact2 = ContactUtil.createContactIdFromTrustedData("+330000003");
        mParticipants.add(contact2);
        mImapCpmSessionMessage = new ImapCpmSessionMessageImpl(
                mOutgoingGroupChatMessage.getChatId(), contact, RcsService.Direction.INCOMING,
                timestamp, "subject", mParticipants, true, false);
        mLogUtilTest = SyncLogUtilTest.getInstance(mContext);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapCmsUtilTest.deleteRemoteStorage();
        mImapServiceHandler.closeService();
        mCmsLog.removeFolders(true);
        mMessagingLog.deleteAllEntries();
        RcsSettingsMock.restoreSettings();
    }

    public void testSyncNewIncomingUnReadGroupChatMessage() throws Exception {
        // Create group chat message on CMS
        String msgId = mIncomingGroupChatMessage.getMessageId();
        String chatId = mIncomingGroupChatMessage.getChatId();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mIncomingGroupChatMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(msgId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE,
                mIncomingGroupChatMessage.getFolder(), msgId, PushStatus.PUSHED, ReadStatus.UNREAD,
                DeleteStatus.NOT_DELETED, chatId);
        cmsObject.setUid(mIncomingGroupChatMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        ChatMessagePersistedStorageAccessor accessor = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        assertEquals(RcsService.Direction.INCOMING, accessor.getDirection());
        assertEquals(chatId, accessor.getChatId());
        assertEquals(mIncomingGroupChatMessage.getRemote(), accessor.getRemoteContact());
        assertEquals(mIncomingGroupChatMessage.getContent(), accessor.getContent());
        assertEquals(mIncomingGroupChatMessage.getMimeType(), accessor.getMimeType());
        long timestampInSecond = mIncomingGroupChatMessage.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(0L, accessor.getTimestampDelivered());
        // assertEquals(timestampInSecond, accessor.getTimestampDisplayed() / 1000L);
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertEquals(mIncomingGroupChatMessage.isSeen() ? ChatLog.Message.Content.Status.RECEIVED
                : ChatLog.Message.Content.Status.DISPLAY_REPORT_REQUESTED, accessor.getStatus());
        assertFalse(accessor.isRead());
        assertEquals(ChatLog.Message.Content.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
    }

    public void testSyncNewOutgoingGroupChatMessageNoGroupChat() throws Exception {
        // Create One to one chat message on CMS
        String msgId = mOutgoingGroupChatMessage.getMessageId();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mOutgoingGroupChatMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(msgId);
        assertNull(cmsObjectFromDb);
        assertFalse(mMessagingLog.isGroupChatPersisted(mOutgoingGroupChatMessage.getChatId()));
        assertFalse(mMessagingLog.isMessagePersisted(msgId));
    }

    public void testSyncNewOutgoingGroupChatMessageNormalCase() throws Exception {
        // Create One to one chat message on CMS
        String msgId = mOutgoingGroupChatMessage.getMessageId();
        String chatId = mOutgoingGroupChatMessage.getChatId();
        List<ImapCmsUtilTest.IImapRcsMessage> messages = new ArrayList<>();
        messages.add(mOutgoingGroupChatMessage);
        messages.add(mImapCpmSessionMessage);
        mImapCmsUtilTest.createRemoteRcsMessages(messages);
        // Synchronize
        mSyncStrategy.execute();
        /*
         * Check that CMS and message, groupchat and groupdeliveryinfo providers are correctly
         * updated.
         */
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(msgId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE,
                mOutgoingGroupChatMessage.getFolder(), msgId, PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.NOT_DELETED, chatId);
        cmsObject.setUid(mOutgoingGroupChatMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);

        assertTrue(mMessagingLog.isGroupChatPersisted(chatId));
        GroupChatInfo groupChatInfo = mMessagingLog.getGroupChatInfo(chatId);
        assertEquals("subject", groupChatInfo.getSubject());
        assertEquals(mImapCpmSessionMessage.getTimestamp() / 1000L,
                groupChatInfo.getTimestamp() / 1000L);
        Map<ContactId, GroupChat.ParticipantStatus> participantsFromDb = groupChatInfo
                .getParticipants();
        List<ContactId> participants = mImapCpmSessionMessage.getParticipants();
        for (ContactId participant : participantsFromDb.keySet()) {
            assertTrue(participants.contains(participant));
        }

        assertTrue(mMessagingLog.isMessagePersisted(msgId));
        ChatMessagePersistedStorageAccessor accessor = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        assertEquals(RcsService.Direction.OUTGOING, accessor.getDirection());
        assertEquals(chatId, accessor.getChatId());
        // TODO PLM check
        assertEquals(mOutgoingGroupChatMessage.getLocalContact(), accessor.getRemoteContact());
        assertEquals(ChatLog.Message.Content.Status.SENT, accessor.getStatus());
        assertEquals(mOutgoingGroupChatMessage.getContent(), accessor.getContent());
        assertEquals(mOutgoingGroupChatMessage.getMimeType(), accessor.getMimeType());
        long timestampInSecond = mOutgoingGroupChatMessage.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(0L, accessor.getTimestampDelivered());
        assertEquals(0L, accessor.getTimestampDisplayed());
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertFalse(accessor.isRead());
        assertEquals(ChatLog.Message.Content.ReasonCode.UNSPECIFIED, accessor.getReasonCode());

        List<SyncLogUtilTest.GroupDeliveryInfo> groupDeliveryInfo = mLogUtilTest
                .getGroupDeliveryInfo(msgId);
        assertEquals(2, groupDeliveryInfo.size());
        for (SyncLogUtilTest.GroupDeliveryInfo item : groupDeliveryInfo) {
            assertTrue(mParticipants.contains(item.mContact));
            assertEquals(chatId, item.mChatId);
            assertEquals(GroupDeliveryInfo.Status.NOT_DELIVERED, item.mStatus);
            assertEquals(GroupDeliveryInfo.ReasonCode.UNSPECIFIED, item.mReason);
            assertEquals(0, item.mTimestampDelivered);
            assertEquals(0, item.mTimestampDisplayed);
        }
    }

    public void testSyncSeenGroupChatMessage() throws Exception {
        testSyncNewIncomingUnReadGroupChatMessage();
        String msgId = mIncomingGroupChatMessage.getMessageId();
        String folder = mIncomingGroupChatMessage.getFolder();
        String chatId = mIncomingGroupChatMessage.getChatId();
        // Mark the remote CMS message as seen
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, msgId,
                mIncomingGroupChatMessage.getUid(), PushStatus.PUSHED,
                ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, chatId);
        mImapCmsUtilTest
                .updateRemoteFlags(folder, Collections.singletonList((CmsObject) cmsObject));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(msgId);
        assertEquals(DeleteStatus.NOT_DELETED, cmsObjectFromDb.getDeleteStatus());
        assertEquals(ReadStatus.READ, cmsObjectFromDb.getReadStatus());
        assertTrue(mMessagingLog.isMessageRead(msgId));
    }

    public void testSyncDeletedGroupChatMessage() throws Exception {
        testSyncNewIncomingUnReadGroupChatMessage();
        String msgId = mIncomingGroupChatMessage.getMessageId();
        String folder = mIncomingGroupChatMessage.getFolder();
        String chatId = mIncomingGroupChatMessage.getChatId();
        // Mark the remote CMS message as deleted
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, msgId,
                mIncomingGroupChatMessage.getUid(), PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.DELETED_REPORT_REQUESTED, chatId);
        mImapCmsUtilTest
                .updateRemoteFlags(folder, Collections.singletonList((CmsObject) cmsObject));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(msgId);
        assertEquals(DeleteStatus.DELETED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mMessagingLog.isMessagePersisted(msgId));
    }
}

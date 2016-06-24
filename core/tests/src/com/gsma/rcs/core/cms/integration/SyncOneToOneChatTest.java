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

import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.protocol.message.ImapOneToOneChatMessageImpl;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
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
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import android.test.AndroidTestCase;

import java.util.Collections;

/**
 * @author Philippe LEMORDANT
 */
public class SyncOneToOneChatTest extends AndroidTestCase {

    private static final Logger sLogger = Logger.getLogger(SyncOneToOneChatTest.class.getName());
    private ImapServiceHandler mImapServiceHandler;
    private BasicSyncStrategy mSyncStrategy;
    private MessagingLog mMessagingLog;
    private CmsLog mCmsLog;
    private ImapCmsUtilTest mImapCmsUtilTest;
    private ImapOneToOneChatMessageImpl mIncomingOneToOneChatMessage;
    private ImapOneToOneChatMessageImpl mOutgoingOneToOneChatMessage;

    protected void setUp() throws Exception {
        super.setUp();
        RcsSettings mSettings = RcsSettingsMock.getMockSettings(mContext);
        AndroidFactory.setApplicationContext(mContext, mSettings);
        mCmsLog = CmsLog.getInstance(mContext);
        LocalContentResolver localContentResolver = new LocalContentResolver(mContext);
        XmsLog xmsLog = XmsLog.getInstance(mContext, mSettings, localContentResolver);
        mMessagingLog = MessagingLog.getInstance(localContentResolver, mSettings);
        CmsSessionController cmsSessionCtrl = new CmsSessionController(mContext, null, null,
                mSettings, localContentResolver, xmsLog, mMessagingLog, mCmsLog);
        InstantMessagingService imService = new InstantMessagingService(null, mSettings, null,
                mMessagingLog, null, localContentResolver, mContext, null, cmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, mMessagingLog, null,
                mSettings, null, cmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, mMessagingLog, mSettings, null, mContext, cmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, mContext.getContentResolver());
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, cmsSessionCtrl, chatService,
                fileTransferService, imService, xmsLog, mSettings, xmsManager, localContentResolver);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, localContentResolver,
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
        ContactId toContact = ContactUtil.createContactIdFromTrustedData("+330000001");
        ContactId fromContact = ContactUtil.createContactIdFromTrustedData("+330000002");
        mIncomingOneToOneChatMessage = new ImapOneToOneChatMessageImpl(fromContact, toContact,
                RcsService.Direction.INCOMING, timestamp, "hello world", false, false);
        mOutgoingOneToOneChatMessage = new ImapOneToOneChatMessageImpl(fromContact, toContact,
                RcsService.Direction.OUTGOING, timestamp, "hello world", true, false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapCmsUtilTest.deleteRemoteStorage();
        mImapServiceHandler.closeService();
        mCmsLog.removeFolders(true);
        mMessagingLog.deleteAllEntries();
        RcsSettingsMock.restoreSettings();
    }

    public void testSyncNewIncomingReadOneToOneChatMessage() throws Exception {
        sLogger.debug("Start testSyncNewIncomingReadOneToOneChatMessage");
        // Create One to one chat message on CMS
        String msgId = mIncomingOneToOneChatMessage.getMessageId();
        mIncomingOneToOneChatMessage.markAsSeen();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mIncomingOneToOneChatMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(msgId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE,
                mIncomingOneToOneChatMessage.getFolder(), msgId, PushStatus.PUSHED,
                ReadStatus.READ, DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(mIncomingOneToOneChatMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        ChatMessagePersistedStorageAccessor accessor = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        assertEquals(RcsService.Direction.INCOMING, accessor.getDirection());
        assertEquals(mIncomingOneToOneChatMessage.getRemote().toString(), accessor.getChatId());
        assertEquals(mIncomingOneToOneChatMessage.getRemote(), accessor.getRemoteContact());
        assertEquals(mIncomingOneToOneChatMessage.getContent(), accessor.getContent());
        assertEquals(mIncomingOneToOneChatMessage.getMimeType(), accessor.getMimeType());
        long timestampInSecond = mIncomingOneToOneChatMessage.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(0L, accessor.getTimestampDelivered());
        // assertEquals(timestampInSecond, accessor.getTimestampDisplayed() / 1000L);
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertEquals(
                mIncomingOneToOneChatMessage.isSeen() ? ChatLog.Message.Content.Status.RECEIVED
                        : ChatLog.Message.Content.Status.DISPLAY_REPORT_REQUESTED,
                accessor.getStatus());
        assertTrue(accessor.isRead());
        assertEquals(ChatLog.Message.Content.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
        sLogger.debug("Stop testSyncNewIncomingReadOneToOneChatMessage");
    }

    public void testSyncNewIncomingUnreadOneToOneChatMessage() throws Exception {
        sLogger.debug("Start testSyncNewIncomingUnreadOneToOneChatMessage");
        // Create One to one chat message on CMS
        String msgId = mIncomingOneToOneChatMessage.getMessageId();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mIncomingOneToOneChatMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(msgId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE,
                mIncomingOneToOneChatMessage.getFolder(), msgId, PushStatus.PUSHED,
                ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(mIncomingOneToOneChatMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        ChatMessagePersistedStorageAccessor accessor = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        assertEquals(RcsService.Direction.INCOMING, accessor.getDirection());
        assertEquals(mIncomingOneToOneChatMessage.getRemote().toString(), accessor.getChatId());
        assertEquals(mIncomingOneToOneChatMessage.getRemote(), accessor.getRemoteContact());
        assertEquals(
                mIncomingOneToOneChatMessage.isSeen() ? ChatLog.Message.Content.Status.RECEIVED
                        : ChatLog.Message.Content.Status.DISPLAY_REPORT_REQUESTED,
                accessor.getStatus());
        assertFalse(accessor.isRead());
        sLogger.debug("Stop testSyncNewIncomingUnreadOneToOneChatMessage");
    }

    public void testSyncNewOutgoingOneToOneChatMessage() throws Exception {
        sLogger.debug("Start testSyncNewOutgoingOneToOneChatMessage");
        // Create One to one chat message on CMS
        String msgId = mOutgoingOneToOneChatMessage.getMessageId();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mOutgoingOneToOneChatMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(msgId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE,
                mOutgoingOneToOneChatMessage.getFolder(), msgId, PushStatus.PUSHED,
                ReadStatus.READ, DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(mOutgoingOneToOneChatMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        ChatMessagePersistedStorageAccessor accessor = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        assertEquals(RcsService.Direction.OUTGOING, accessor.getDirection());
        assertEquals(mOutgoingOneToOneChatMessage.getRemote().toString(), accessor.getChatId());
        assertEquals(mOutgoingOneToOneChatMessage.getRemote(), accessor.getRemoteContact());
        assertEquals(ChatLog.Message.Content.Status.SENT, accessor.getStatus());
        assertEquals(mOutgoingOneToOneChatMessage.getContent(), accessor.getContent());
        assertEquals(mOutgoingOneToOneChatMessage.getMimeType(), accessor.getMimeType());
        long timestampInSecond = mOutgoingOneToOneChatMessage.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(0L, accessor.getTimestampDelivered());
        assertEquals(0L, accessor.getTimestampDisplayed());
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertFalse(accessor.isRead());
        assertEquals(ChatLog.Message.Content.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
        sLogger.debug("Stop testSyncNewOutgoingOneToOneChatMessage");
    }

    public void testSyncSeenOneToOneChatMessage() throws Exception {
        sLogger.debug("Start testSyncSeenOneToOneChatMessage");
        testSyncNewIncomingUnreadOneToOneChatMessage();
        String msgId = mIncomingOneToOneChatMessage.getMessageId();
        String folder = mIncomingOneToOneChatMessage.getFolder();
        // Mark the remote CMS message as seen
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, msgId,
                mIncomingOneToOneChatMessage.getUid(), PushStatus.PUSHED,
                ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null);
        mImapCmsUtilTest
                .updateRemoteFlags(folder, Collections.singletonList((CmsObject) cmsObject));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(msgId);
        assertEquals(DeleteStatus.NOT_DELETED, cmsObjectFromDb.getDeleteStatus());
        assertEquals(ReadStatus.READ, cmsObjectFromDb.getReadStatus());
        assertTrue(mMessagingLog.isMessageRead(msgId));
        sLogger.debug("Stop testSyncSeenOneToOneChatMessage");
    }

    public void testSyncDeletedOneToOneChatMessage() throws Exception {
        sLogger.debug("Start testSyncDeletedOneToOneChatMessage");
        testSyncNewOutgoingOneToOneChatMessage();
        String msgId = mOutgoingOneToOneChatMessage.getMessageId();
        String folder = mOutgoingOneToOneChatMessage.getFolder();
        // Mark the remote CMS message as deleted
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, msgId,
                mOutgoingOneToOneChatMessage.getUid(), PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.DELETED_REPORT_REQUESTED, null);
        mImapCmsUtilTest
                .updateRemoteFlags(folder, Collections.singletonList((CmsObject) cmsObject));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(msgId);
        assertEquals(DeleteStatus.DELETED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mMessagingLog.isMessagePersisted(msgId));
        sLogger.debug("Stop testSyncDeletedOneToOneChatMessage");
    }
}

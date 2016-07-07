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
import com.gsma.rcs.core.cms.protocol.message.ImapImdnMessageImpl;
import com.gsma.rcs.core.cms.protocol.message.ImapOneToOneChatMessageImpl;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Philippe LEMORDANT
 */
public class SyncImdnOneToOneChatTest extends AndroidTestCase {

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

    public void testSyncNewImdnDisplayedOugoingOneToOneChatMessage() throws Exception {
        long timestampDisplay = 24L * 3600L * 1000L * 2L + 1001L;
        String msgId = mOutgoingOneToOneChatMessage.getMessageId();
        ImapImdnMessageImpl imdnMessage = new ImapImdnMessageImpl(msgId, null,
                mOutgoingOneToOneChatMessage.getRemote(),
                mOutgoingOneToOneChatMessage.getLocalContact(), RcsService.Direction.INCOMING,
                timestampDisplay, ImdnDocument.DeliveryStatus.DISPLAYED, false, false);
        // Create One to one chat message on CMS
        List<ImapCmsUtilTest.IImapRcsMessage> rcsMessages = new ArrayList<>();
        rcsMessages.add(mOutgoingOneToOneChatMessage);
        rcsMessages.add(imdnMessage);
        mImapCmsUtilTest.createRemoteRcsMessages(rcsMessages);
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS log is correctly updated
        String imdnId = imdnMessage.getMessageId();
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(imdnId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.IMDN, imdnMessage.getFolder(),
                imdnId, PushStatus.PUSHED, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(imdnMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        // Check that chat message log is correctly updated
        ChatMessagePersistedStorageAccessor accessor = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        long timestampInSecond = mOutgoingOneToOneChatMessage.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(0, accessor.getTimestampDelivered());
        assertEquals(timestampDisplay / 1000L, accessor.getTimestampDisplayed() / 1000L);
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertEquals(ChatLog.Message.Content.Status.DISPLAYED, accessor.getStatus());
        assertEquals(ChatLog.Message.Content.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
    }

    public void testSyncNewImdnDeliveredOutgoingOneToOneChatMessage() throws Exception {
        long timestampDeliver = 24L * 3600L * 1000L * 2L + 1001L;
        String msgId = mOutgoingOneToOneChatMessage.getMessageId();
        ImapImdnMessageImpl imdnMessage = new ImapImdnMessageImpl(msgId, null,
                mOutgoingOneToOneChatMessage.getRemote(),
                mOutgoingOneToOneChatMessage.getLocalContact(), RcsService.Direction.INCOMING,
                timestampDeliver, ImdnDocument.DeliveryStatus.DELIVERED, false, false);
        // Create One to one chat message on CMS
        List<ImapCmsUtilTest.IImapRcsMessage> rcsMessages = new ArrayList<>();
        rcsMessages.add(mOutgoingOneToOneChatMessage);
        rcsMessages.add(imdnMessage);
        mImapCmsUtilTest.createRemoteRcsMessages(rcsMessages);
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS log is correctly updated
        String imdnId = imdnMessage.getMessageId();
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(imdnId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.IMDN, imdnMessage.getFolder(),
                imdnId, PushStatus.PUSHED, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(imdnMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        // Check that chat message log is correctly updated
        ChatMessagePersistedStorageAccessor accessor = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        long timestampInSecond = mOutgoingOneToOneChatMessage.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(timestampDeliver / 1000L, accessor.getTimestampDelivered() / 1000L);
        assertEquals(0, accessor.getTimestampDisplayed());
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertEquals(ChatLog.Message.Content.Status.DELIVERED, accessor.getStatus());
        assertEquals(ChatLog.Message.Content.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
    }

    public void testSyncNewImdnDisplayedIncomingOneToOneChatMessage() throws Exception {
        long timestampDisplay = 24L * 3600L * 1000L * 2L + 1001L;
        String msgId = mIncomingOneToOneChatMessage.getMessageId();
        ImapImdnMessageImpl imdnMessage = new ImapImdnMessageImpl(msgId, null,
                mIncomingOneToOneChatMessage.getRemote(),
                mIncomingOneToOneChatMessage.getLocalContact(), RcsService.Direction.OUTGOING,
                timestampDisplay, ImdnDocument.DeliveryStatus.DISPLAYED, false, false);
        // Create One to one chat message on CMS
        List<ImapCmsUtilTest.IImapRcsMessage> rcsMessages = new ArrayList<>();
        rcsMessages.add(mIncomingOneToOneChatMessage);
        rcsMessages.add(imdnMessage);
        mImapCmsUtilTest.createRemoteRcsMessages(rcsMessages);
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS log is correctly updated
        String imdnId = imdnMessage.getMessageId();
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(imdnId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.IMDN, imdnMessage.getFolder(),
                imdnId, PushStatus.PUSHED, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(imdnMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        // Check that chat message log is correctly updated
        ChatMessagePersistedStorageAccessor accessor = new ChatMessagePersistedStorageAccessor(
                mMessagingLog, msgId);
        long timestampInSecond = mIncomingOneToOneChatMessage.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(0, accessor.getTimestampDelivered());
        assertEquals(timestampDisplay / 1000L, accessor.getTimestampDisplayed() / 1000L);
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertEquals(
                mIncomingOneToOneChatMessage.isSeen() ? ChatLog.Message.Content.Status.RECEIVED
                        : ChatLog.Message.Content.Status.DISPLAY_REPORT_REQUESTED,
                accessor.getStatus());
        assertEquals(ChatLog.Message.Content.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
    }

}

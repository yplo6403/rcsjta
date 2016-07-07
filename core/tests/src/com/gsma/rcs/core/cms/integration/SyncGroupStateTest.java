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
import com.gsma.rcs.core.cms.protocol.message.ImapGroupStateMessageImpl;
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
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.contact.ContactId;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Philippe LEMORDANT
 */
public class SyncGroupStateTest extends AndroidTestCase {

    private ImapServiceHandler mImapServiceHandler;
    private BasicSyncStrategy mSyncStrategy;
    private MessagingLog mMessagingLog;
    private CmsLog mCmsLog;
    private ImapCmsUtilTest mImapCmsUtilTest;
    private ImapGroupStateMessageImpl mGroupStateMessage;
    private ContactId mContact;
    private ContactId mContact1;
    private ContactId mContact2;
    private ContactId mContact3;

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

        String chatId = IdGenerator.generateMessageID();
        // Create a GC on CMS
        mContact = ContactUtil.createContactIdFromTrustedData("+330000001");
        List<ContactId> participants = new ArrayList<>();
        mContact1 = ContactUtil.createContactIdFromTrustedData("+330000002");
        participants.add(mContact1);
        mContact2 = ContactUtil.createContactIdFromTrustedData("+330000003");
        participants.add(mContact2);
        mContact3 = ContactUtil.createContactIdFromTrustedData("+330000004");
        participants.add(mContact3);
        long timestamp = System.currentTimeMillis();
        mGroupStateMessage = new ImapGroupStateMessageImpl(chatId, mContact, "subject",
                participants, RcsService.Direction.INCOMING, timestamp, true, false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapCmsUtilTest.deleteRemoteStorage();
        mImapServiceHandler.closeService();
        mCmsLog.removeFolders(true);
        mMessagingLog.deleteAllEntries();
        RcsSettingsMock.restoreSettings();
    }

    public void testSyncNewGroupStateWithoutGroupChat() throws Exception {
        // Create group state message on CMS
        String chatId = mGroupStateMessage.getChatId();
        String msgId = mGroupStateMessage.getMessageId();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mGroupStateMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and group chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getGroupChatObjectData(msgId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.GROUP_STATE,
                mGroupStateMessage.getFolder(), msgId, PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.NOT_DELETED, chatId);
        cmsObject.setUid(mGroupStateMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        assertFalse(mMessagingLog.isGroupChatPersisted(chatId));
        GroupChatInfo groupChatInfo = mMessagingLog.getGroupChatInfo(chatId);
        assertNull(groupChatInfo);

        mGroupStateMessage = new ImapGroupStateMessageImpl(chatId, mContact, "subject",
                mGroupStateMessage.getParticipants(), RcsService.Direction.INCOMING,
                mGroupStateMessage.getTimestamp() * 2L, true, false);

        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mGroupStateMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and group chat providers are correctly updated
        cmsObjectFromDb = mCmsLog.getGroupChatObjectData(mGroupStateMessage.getMessageId());
        cmsObject = new CmsRcsObject(MessageType.GROUP_STATE, mGroupStateMessage.getFolder(),
                mGroupStateMessage.getMessageId(), PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.NOT_DELETED, chatId);
        cmsObject.setUid(mGroupStateMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
    }

    public void testSyncNewGroupStateWithGroupChat() throws Exception {
        // Create group state message on CMS
        String chatId = mGroupStateMessage.getChatId();
        String msgId = mGroupStateMessage.getMessageId();
        List<ContactId> participants = new ArrayList<>();
        participants.add(mContact1);
        ImapCpmSessionMessageImpl cpmSessionMessage = new ImapCpmSessionMessageImpl(chatId,
                mContact, RcsService.Direction.INCOMING, System.currentTimeMillis(), "subject",
                participants, true, false);
        List<ImapCmsUtilTest.IImapRcsMessage> messages = new ArrayList<>();
        messages.add(mGroupStateMessage);
        messages.add(cpmSessionMessage);
        mImapCmsUtilTest.createRemoteRcsMessages(messages);
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and group chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getGroupChatObjectData(msgId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.GROUP_STATE,
                mGroupStateMessage.getFolder(), msgId, PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.NOT_DELETED, chatId);
        cmsObject.setUid(mGroupStateMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        assertTrue(mMessagingLog.isGroupChatPersisted(chatId));
        GroupChatInfo groupChatInfo = mMessagingLog.getGroupChatInfo(chatId);
        Map<ContactId, GroupChat.ParticipantStatus> remotes = groupChatInfo.getParticipants();
        assertEquals(3, remotes.size());
        assertTrue(remotes.containsKey(mContact1));
        assertTrue(remotes.containsKey(mContact2));
        assertTrue(remotes.containsKey(mContact3));
        for (GroupChat.ParticipantStatus status : remotes.values()) {
            assertEquals(GroupChat.ParticipantStatus.CONNECTED, status);
        }
        participants = new ArrayList<>(mGroupStateMessage.getParticipants());
        participants.add(ContactUtil.createContactIdFromTrustedData("+330000005"));
        mGroupStateMessage = new ImapGroupStateMessageImpl(chatId, mContact, "subject",
                participants, RcsService.Direction.INCOMING,
                mGroupStateMessage.getTimestamp() * 2L, true, false);

        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mGroupStateMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and group chat providers are correctly updated
        cmsObjectFromDb = mCmsLog.getGroupChatObjectData(mGroupStateMessage.getMessageId());
        cmsObject = new CmsRcsObject(MessageType.GROUP_STATE, mGroupStateMessage.getFolder(),
                mGroupStateMessage.getMessageId(), PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.NOT_DELETED, chatId);
        cmsObject.setUid(mGroupStateMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        groupChatInfo = mMessagingLog.getGroupChatInfo(chatId);
        remotes = groupChatInfo.getParticipants();
        assertEquals(4, remotes.size());
    }

}

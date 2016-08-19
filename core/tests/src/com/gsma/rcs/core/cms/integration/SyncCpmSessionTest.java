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
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.contact.ContactId;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SyncCpmSessionTest extends AndroidTestCase {

    private ImapServiceHandler mImapServiceHandler;
    private BasicSyncStrategy mSyncStrategy;
    private MessagingLog mMessagingLog;
    private CmsLog mCmsLog;
    private ImapCmsUtilTest mImapCmsUtilTest;
    private ImapCpmSessionMessageImpl mImapCpmSessionMessage;

    protected void setUp() throws Exception {
        super.setUp();
        RcsSettings mSettings = RcsSettingsMock.getMockSettings(mContext);
        AndroidFactory.setApplicationContext(mContext, mSettings);
        mCmsLog = CmsLog.getInstance(mContext);
        SmsMmsLog smsMmsLog = SmsMmsLog.getInstance(mContext, mContext.getContentResolver());
        LocalContentResolver localContentResolver = new LocalContentResolver(mContext);
        XmsLog xmsLog = XmsLog.getInstance(mContext, mSettings, localContentResolver);
        mMessagingLog = MessagingLog.getInstance(localContentResolver, mSettings);
        CmsSessionController cmsSessionCtrl = new CmsSessionController(mContext, null, mSettings,
                localContentResolver, xmsLog, mMessagingLog, mCmsLog);
        InstantMessagingService imService = new InstantMessagingService(null, mSettings, null,
                mMessagingLog, null, localContentResolver, mContext, null, cmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, mMessagingLog, null,
                mSettings, null, cmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, mMessagingLog, mSettings, null, mContext, cmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, xmsLog, smsMmsLog);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, cmsSessionCtrl, chatService,
                fileTransferService, imService, xmsLog, xmsManager, localContentResolver, smsMmsLog);
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
        ContactId contact = ContactUtil.createContactIdFromTrustedData("+330000001");
        List<ContactId> participants = new ArrayList<>();
        ContactId contact1 = ContactUtil.createContactIdFromTrustedData("+330000002");
        participants.add(contact1);
        ContactId contact2 = ContactUtil.createContactIdFromTrustedData("+330000003");
        participants.add(contact2);
        long timestamp = System.currentTimeMillis();
        mImapCpmSessionMessage = new ImapCpmSessionMessageImpl(chatId, contact,
                RcsService.Direction.INCOMING, timestamp, "subject", participants, true, false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapCmsUtilTest.deleteRemoteStorage();
        mImapServiceHandler.closeService();
        mCmsLog.removeFolders(true);
        mMessagingLog.deleteAllEntries();
        RcsSettingsMock.restoreSettings();
    }

    public void testSyncNewCpmSession() throws Exception {
        // Create CPM session message on CMS
        String chatId = mImapCpmSessionMessage.getChatId();
        String msgId = mImapCpmSessionMessage.getMessageId();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mImapCpmSessionMessage));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and group chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getCpmSessionData(msgId);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CPM_SESSION,
                mImapCpmSessionMessage.getFolder(), msgId, PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.NOT_DELETED, chatId);
        cmsObject.setUid(mImapCpmSessionMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        assertTrue(mMessagingLog.isGroupChatPersisted(chatId));
        GroupChatInfo groupChatInfo = mMessagingLog.getGroupChatInfo(chatId);
        assertEquals(mImapCpmSessionMessage.getSubject(), groupChatInfo.getSubject());
        assertEquals(mImapCpmSessionMessage.getTimestamp() / 1000L,
                groupChatInfo.getTimestamp() / 1000L);
        Map<ContactId, GroupChat.ParticipantStatus> participantsFromDB = groupChatInfo
                .getParticipants();
        List<ContactId> participants = mImapCpmSessionMessage.getParticipants();
        for (ContactId participant : participantsFromDB.keySet()) {
            assertTrue(participants.contains(participant));
        }
    }

    public void testSyncDeletedCpmSession() throws Exception {
        testSyncNewCpmSession();
        String chatId = mImapCpmSessionMessage.getChatId();
        String msgId = mImapCpmSessionMessage.getMessageId();
        String folder = mImapCpmSessionMessage.getFolder();
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CPM_SESSION, folder, msgId,
                mImapCpmSessionMessage.getUid(), PushStatus.PUSHED, ReadStatus.READ,
                DeleteStatus.DELETED_REPORT_REQUESTED, chatId);
        mImapCmsUtilTest
                .updateRemoteFlags(folder, Collections.singletonList((CmsObject) cmsObject));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and group chat providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getCpmSessionData(msgId);
        assertEquals(DeleteStatus.DELETED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mMessagingLog.isGroupChatPersisted(chatId));
    }

}

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

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.protocol.message.ImapChatMessageTest;
import com.gsma.rcs.core.cms.protocol.message.ImapImdnMessageTest;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsService;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncDeleteTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncDeleteTask.Operation;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsLogTestIntegration;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class GroupStateTest extends AndroidTestCase {

    private ImapServiceHandler mImapServiceHandler;
    private BasicImapService mBasicImapService;
    private BasicSyncStrategy mSyncStrategy;
    private MessagingLog mMessagingLog;
    private CmsLogTestIntegration mCmsLogTestIntegration;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContactUtil.getInstance(getContext());
        RcsSettings settings = RcsSettingsMock.getMockSettings(context);
        AndroidFactory.setApplicationContext(context, settings);
        CmsLog cmsLog = CmsLog.getInstance(context);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(context);
        LocalContentResolver localContentResolver = new LocalContentResolver(context);
        XmsLog xmsLog = XmsLog.getInstance(context, settings, localContentResolver);
        mMessagingLog = MessagingLog.getInstance(localContentResolver, settings);

        InstantMessagingService instantMessagingService = new InstantMessagingService(null,
                settings, null, mMessagingLog, null, localContentResolver, context, null);
        ChatServiceImpl chatService = new ChatServiceImpl(instantMessagingService, mMessagingLog,
                null, settings, null);
        XmsManager xmsManager = new XmsManager(context, context.getContentResolver());
        CmsService cmsService = new CmsService(null, null, context, settings, xmsLog,
                mMessagingLog, cmsLog);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(context, cmsService, chatService,
                xmsLog, settings, xmsManager, localContentResolver);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(context, cmsLog, xmsLog,
                mMessagingLog, chatService, cmsServiceImpl, settings);
        LocalStorage localStorage = new LocalStorage(cmsLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(settings);
        mBasicImapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(context, settings, mBasicImapService, localStorage);
        mBasicImapService.init();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapServiceHandler.closeService();
        RcsSettingsMock.restoreSettings();
    }

    public void testOutgoingGroupChatConversation() throws Exception {

        String from = "+33643209850";
        String to = "+33681639059";
        String direction = Constants.DIRECTION_SENT;

        Map<String, String> participants = new HashMap<>();
        participants.put("bob", to);

        String chatId = UUID.randomUUID().toString();
        String remoteFolder = "Default/" + chatId + "/" + chatId;

        List<String> payloads = new ArrayList<>();
        payloads.add(buildGroupStatePayload(from, chatId, chatId, participants));

        ImapChatMessageTest imapChatMessageTest = new ImapChatMessageTest();
        imapChatMessageTest.init();
        payloads.add(imapChatMessageTest.getPayload(false, from, to, chatId, direction));

        ImapImdnMessageTest imapImdnMessageTest = new ImapImdnMessageTest();
        imapImdnMessageTest.init();
        payloads.add(imapImdnMessageTest.getPayload(false, from, to, direction, chatId,
                ImdnDocument.DELIVERY_STATUS_DELIVERED));

        imapImdnMessageTest = new ImapImdnMessageTest();
        imapImdnMessageTest.init();
        payloads.add(imapImdnMessageTest.getPayload(false, from, to, direction, chatId,
                ImdnDocument.DELIVERY_STATUS_DISPLAYED));

        createRemoteMessages(remoteFolder, payloads.toArray(new String[payloads.size()]));

    }

    public void testIncomingGroupChatConversation() throws Exception {

        String from = "+33642575779";
        String to = "+33643209850";
        String direction = Constants.DIRECTION_RECEIVED;

        Map<String, String> participants = new HashMap<>();
        participants.put("bob", to);

        String chatId = UUID.randomUUID().toString();
        String remoteFolder = "Default/" + chatId + "/" + chatId;

        List<String> payloads = new ArrayList<>();
        payloads.add(buildGroupStatePayload(from, chatId, chatId, participants));

        ImapChatMessageTest imapChatMessageTest = new ImapChatMessageTest();
        imapChatMessageTest.init();
        payloads.add(imapChatMessageTest.getPayload(false, from, to, chatId, direction));
        createRemoteMessages(remoteFolder, payloads.toArray(new String[payloads.size()]));

    }

    /**
     * Test1 step 0 : purge local storage and CMS server folders step 1 : create a conversation on
     * CMS server step 2 : start a sync
     */
    public void testSyncGroupStateObject() throws Exception {

        String from = "+33643209850";
        Map<String, String> participants = new HashMap<>();
        participants.put("bob", "+33600000001");
        participants.put("alice", "+33600000002");
        participants.put("donald", "+33600000003");

        // create messages on CMS
        String chatId = UUID.randomUUID().toString();
        String remoteFolder = "Default/" + chatId + "/" + chatId;
        createRemoteMessage(remoteFolder,
                buildGroupStatePayload(from, chatId, chatId, participants));

        int initialNbMessages = mCmsLogTestIntegration.getMessages(remoteFolder).size();
        assertFalse(mMessagingLog.isGroupChatPersisted(chatId));
        // start synchro
        startSynchro();

        assertEquals(initialNbMessages + 1, mCmsLogTestIntegration.getMessages(remoteFolder).size());
        assertTrue(mMessagingLog.isGroupChatPersisted(chatId));

        deleteRemoteMailbox(remoteFolder);
        deleteRemoteMailbox("Default/" + chatId);
    }

    private void createRemoteMessage(String remoteFolder, String payload) throws Exception {
        createRemoteMessages(remoteFolder, new String[] {
            payload
        });
    }

    private void deleteRemoteMailbox(String mailbox) throws Exception {
        CmsSyncDeleteTask deleteTask = new CmsSyncDeleteTask(Operation.DELETE_MAILBOX, mailbox, null);
        deleteTask.setBasicImapService(mBasicImapService);
        deleteTask.delete(mailbox);
        try {
            mBasicImapService.close();
        } catch (IOException ignore) {
        }
        mBasicImapService.init();
    }

    private void createRemoteMessages(String remoteFolder, String[] payloads) throws Exception {
        mBasicImapService.create(remoteFolder);
        mBasicImapService.selectCondstore(remoteFolder);
        for (String payload : payloads) {
            mBasicImapService.append(remoteFolder, new ArrayList(), payload);
        }
    }

    private void startSynchro() throws Exception {
        mSyncStrategy.execute();
    }

    private String buildGroupStatePayload(String from, String conversationId,
            String contributionId, Map<String, String> participants) {

        String dateImap = DateUtils.getDateAsString(System.currentTimeMillis(),
                DateUtils.CMS_IMAP_DATE_FORMAT);
        String dateCpim = DateUtils.getDateAsString(System.currentTimeMillis(),
                DateUtils.CMS_CPIM_DATE_FORMAT);
        String imdnId = UUID.randomUUID().toString();

        StringBuilder participantsXml = new StringBuilder();
        for (Entry<String, String> entry : participants.entrySet()) {
            participantsXml.append(
                    "<participant name=\"" + entry.getKey() + "\" comm-addr=\"" + entry.getValue()
                            + "\"/>").append(Constants.CRLF);
        }

        String rejoindId = "sip:pfcf-imas-orange@RCS14lb-2.sip.imsnsn.fr:5060;transport=udp;oaid="
                + from + ";ocid=" + contributionId;

        String payload = "From: +33642575779" + Constants.CRLF + "To: +33640332859"
                + Constants.CRLF + "Date: " + dateImap + Constants.CRLF + "Subject: mySubject"
                + Constants.CRLF + "Conversation-ID: " + conversationId + Constants.CRLF
                + "Contribution-ID: " + contributionId + Constants.CRLF + "IMDN-Message-ID: "
                + imdnId + Constants.CRLF + "Content-Type: Application/group-state-object+xml"
                + Constants.CRLF + Constants.CRLF + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + Constants.CRLF + "<groupstate" + Constants.CRLF + "timestamp=\"" + dateCpim
                + "\"" + Constants.CRLF + "lastfocussessionid=\"" + rejoindId + "\""
                + Constants.CRLF + "group-type=\"Closed\">" + Constants.CRLF + participantsXml
                + "</groupstate>" + Constants.CRLF;

        return payload;
    }
}

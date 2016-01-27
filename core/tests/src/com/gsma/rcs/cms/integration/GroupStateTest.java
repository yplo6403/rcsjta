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

package com.gsma.rcs.cms.integration;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.CmsEventHandler;
import com.gsma.rcs.cms.imap.message.ImapChatMessageTest;
import com.gsma.rcs.cms.imap.message.ImapImdnMessageTest;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceHandler;
import com.gsma.rcs.cms.imap.task.DeleteTask;
import com.gsma.rcs.cms.imap.task.DeleteTask.Operation;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.ImapLogEnvIntegration;
import com.gsma.rcs.cms.provider.xms.XmsLogEnvIntegration;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.BasicSyncStrategy;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class GroupStateTest extends AndroidTestCase {

    private XmsLogEnvIntegration mXmsLogEnvIntegration;
    private RcsSettings mSettings;
    private LocalStorage mLocalStorage;
    private ImapServiceHandler mImapServiceHandler;
    private BasicImapService mBasicImapService;
    private BasicSyncStrategy mSyncStrategy;
    private ImapLog mImapLog;
    private MessagingLog mMessagingLog;
    private ImapLogEnvIntegration mImapLogEnvIntegration;
    private XmsLog mXmsLog;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContactUtil.getInstance(getContext());
        mSettings = RcsSettingsMock.getMockSettings(context);
        AndroidFactory.setApplicationContext(context, mSettings);
        mImapLog = ImapLog.createInstance(context);
        mImapLogEnvIntegration = ImapLogEnvIntegration.getInstance(context);
        LocalContentResolver localContentResolver = new LocalContentResolver(context);
        mXmsLog = XmsLog.createInstance(context, localContentResolver);
        mMessagingLog = MessagingLog.createInstance(localContentResolver, mSettings);
        mXmsLogEnvIntegration = XmsLogEnvIntegration.getInstance(context);

        InstantMessagingService instantMessagingService = new InstantMessagingService(null,
                mSettings, null, mMessagingLog, null, localContentResolver, context, null);
        ChatServiceImpl chatService = new ChatServiceImpl(instantMessagingService, mMessagingLog,
                null, mSettings, null);

        CmsEventHandler cmsEventHandler = new CmsEventHandler(context, mImapLog, mXmsLog,
                mMessagingLog, chatService, mSettings, null);
        mLocalStorage = new LocalStorage(mImapLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(mSettings);
        mBasicImapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(context, mSettings, mBasicImapService, mLocalStorage);
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

        int initialNbMessages = mImapLogEnvIntegration.getMessages(remoteFolder).size();
        assertFalse(mMessagingLog.isGroupChatPersisted(chatId));
        // start synchro
        startSynchro();

        assertEquals(initialNbMessages + 1, mImapLogEnvIntegration.getMessages(remoteFolder).size());
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
        DeleteTask deleteTask = new DeleteTask(Operation.DELETE_MAILBOX, mailbox, null);
        deleteTask.setBasicImapService(mBasicImapService);
        deleteTask.delete(mailbox);
        try {
            mBasicImapService.close();
        } catch (Exception e) {
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
        Iterator<Entry<String, String>> iter = participants.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String> entry = iter.next();
            participantsXml.append(
                    "<participant name=\"" + entry.getKey() + "\" comm-addr=\"" + entry.getValue()
                            + "\"/>").append(Constants.CRLF);
        }

        String rejoindId = "sip:pfcf-imas-orange@RCS14lb-2.sip.imsnsn.fr:5060;transport=udp;oaid="
                + from + ";ocid=" + contributionId;

        String payload = new StringBuilder().append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF).append("Date: ")
                .append(dateImap).append(Constants.CRLF).append("Subject: mySubject")
                .append(Constants.CRLF).append("Conversation-ID: " + conversationId)
                .append(Constants.CRLF).append("Contribution-ID: " + contributionId)
                .append(Constants.CRLF).append("IMDN-Message-ID: " + imdnId).append(Constants.CRLF)
                .append("Content-Type: Application/group-state-object+xml").append(Constants.CRLF)
                .append(Constants.CRLF).append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append(Constants.CRLF).append("<groupstate").append(Constants.CRLF)
                .append("timestamp=\"" + dateCpim + "\"").append(Constants.CRLF)
                .append("lastfocussessionid=\"").append(rejoindId).append("\"")
                .append(Constants.CRLF).append("group-type=\"Closed\">").append(Constants.CRLF)
                .append(participantsXml).append("</groupstate>").append(Constants.CRLF).toString();

        return payload;
    }
}

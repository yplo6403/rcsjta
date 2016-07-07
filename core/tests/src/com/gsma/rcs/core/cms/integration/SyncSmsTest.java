/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.integration;

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test1;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test2;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test5;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test7;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test8;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test9;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.TestLoad;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsLogTestIntegration;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SyncSmsTest extends AndroidTestCase {

    private SyncLogUtilTest mLogUtilTest;
    private ImapServiceHandler mImapServiceHandler;
    private BasicSyncStrategy mSyncStrategy;
    private CmsLog mCmsLog;
    private CmsLogTestIntegration mCmsLogTestIntegration;
    private XmsLog mXmsLog;
    private LocalContentResolver mLocalContentResolver;
    private ImapCmsUtilTest mImapCmsUtilTest;

    protected void setUp() throws Exception {
        super.setUp();
        RcsSettings settings = RcsSettingsMock.getMockSettings(mContext);
        AndroidFactory.setApplicationContext(mContext, settings);
        mCmsLog = CmsLog.getInstance(mContext);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(mContext);
        mLocalContentResolver = new LocalContentResolver(mContext);
        mXmsLog = XmsLog.getInstance(mContext, settings, mLocalContentResolver);
        MessagingLog messagingLog = MessagingLog.getInstance(new LocalContentResolver(mContext),
                settings);
        mLogUtilTest = SyncLogUtilTest.getInstance(mContext);
        CmsSessionController cmsSessionCtrl = new CmsSessionController(mContext, null, null,
                settings, mLocalContentResolver, mXmsLog, messagingLog, mCmsLog);
        InstantMessagingService imService = new InstantMessagingService(null, settings, null,
                messagingLog, null, mLocalContentResolver, mContext, null, cmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, messagingLog, null, settings,
                null, cmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, messagingLog, settings, null, mContext, cmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, mContext.getContentResolver());

        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, cmsSessionCtrl, chatService,
                fileTransferService, imService, mXmsLog, settings, xmsManager,
                mLocalContentResolver);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, mLocalContentResolver,
                mCmsLog, mXmsLog, messagingLog, chatService, fileTransferService, cmsServiceImpl,
                imService, settings);
        LocalStorage localStorage = new LocalStorage(settings, mCmsLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(settings);
        BasicImapService imapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(mContext, settings, imapService, localStorage,
                mXmsLog, mCmsLog);
        imapService.init();
        mImapCmsUtilTest = new ImapCmsUtilTest(mContext, settings, imapService, mCmsLog);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapCmsUtilTest.deleteRemoteStorage();
        mImapServiceHandler.closeService();
        mCmsLog.removeFolders(true);
        mLocalContentResolver.delete(XmsData.CONTENT_URI, null, null);
        RcsSettingsMock.restoreSettings();
    }

    /**
     * Test1
     * <ul>
     * <li>step 0 : purge local storage and CMS server folders</li>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : start a sync</li>
     * </ul>
     */
    public void test1() throws FileAccessException, NetworkException, PayloadException {
        Map<Integer, CmsObject> imapData;
        // create messages on CMS
        mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test1.conversation);
        mSyncStrategy.execute();
        // check that messages are created in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        int nbFolders = mCmsLog.getFolders().size();
        assertTrue(nbFolders > 0);
        imapData = mCmsLogTestIntegration.getMessages(SmsIntegrationUtils.Test1.folderName);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, imapData.size());
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, messages.size());
        for (XmsDataObject message : messages) {
            Assert.assertEquals(Test1.readStatus, message.getReadStatus());
        }
        // start synchronization : all is up to date -> nothing happened
        mSyncStrategy.execute();
        // check that folders are present in local storage
        assertEquals(nbFolders, mCmsLog.getFolders().size());
        // check that messages are present in local storage
        imapData = mCmsLogTestIntegration.getMessages(SmsIntegrationUtils.Test1.folderName);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, imapData.size());
    }

    /**
     * Test2 Test 1 + Step 0 : mark conversation as seen on CMS
     * <ul>
     * <li>step 1 : start a sync : messages are marked as seen in local storage</li>
     * <li>Step 2 : mark conversation as deleted on CMS</li>
     * <li>step 3 : start a sync : messages are marked as deleted in local storage</li>
     * </ul>
     */
    public void test2() throws FileAccessException, NetworkException, PayloadException {
        test1();
        // update messages with 'seen' flag on CMS
        mImapCmsUtilTest.updateRemoteFlags(Test1.folderName, Test2.cmsObjectReadRequested);
        mSyncStrategy.execute();
        // check that messages are marked as 'Seen' in local storage
        int nbFolders = mCmsLog.getFolders().size();
        assertTrue(nbFolders > 0);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName).size());
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, messages.size());
        for (XmsDataObject message : messages) {
            Assert.assertEquals(ReadStatus.READ, message.getReadStatus());
        }
        // update messages with 'deleted' flag on CMS
        mImapCmsUtilTest.updateRemoteFlags(Test1.folderName, Test2.cmsObjectDeletedRequested);
        mSyncStrategy.execute();
        // check that messages are marked as 'Deleted' in local storage
        assertEquals(nbFolders, mCmsLog.getFolders().size());
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName).size());
        messages = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(0, messages.size());
    }

    /**
     * Test3 Test 1 + Step 0 : mark conversation as read_report_requested in local storage
     * <ul>
     * <li>Step 1 : start sync : message are marked as seen in CMS</li>
     * <li>step 2 : start sync : messages are marked as seen in local storage</li>
     * <li>Step 3 : mark conversation as deleted_requested in local storage</li>
     * <li>Step 4 : start sync : message are marked as deleted in CMS</li>
     * <li>step 5 : start sync : messages are marked as deleted in local storage</li>
     * </ul>
     */
    public void test3() throws FileAccessException, NetworkException, PayloadException {
        test1();
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateXmsReadStatus(SmsIntegrationUtils.Test1.contact, sms.getMessageId(),
                    CmsData.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        // sync with CMS : during this first sync, messages are marked as 'Seen' on CMS
        mSyncStrategy.execute();
        String folder = CmsUtils.contactToCmsFolder(SmsIntegrationUtils.Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsData.ReadStatus.READ, cmsObject.getReadStatus());
        }
        messages = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.SMS, sms.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
        }
        // sync with CMS : during this first sync, messages are marked as 'Deleted' on CMS
        mSyncStrategy.execute();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test4 Test 1 + Step 0 : delete mailbox from CMS. In local storage, message are pushed.
     * <ul>
     * <li>Step 1 : mark conversation as seen_requested in local storage</li>
     * <li>Step 2 : start sync</li>
     * <li>step 3 : check that local storage is unchanged since flag synchronization cannot be done
     * if message is not present in remote storage.</li>
     * <li>Step 1 : mark conversation as delete_requested in local storage</li>
     * <li>Step 2 : start sync</li>
     * <li>step 3 : check that local storage is unchanged since flag synchronization cannot be done
     * if message is not present in remote storage.</li>
     * </ul>
     */
    public void test4() throws FileAccessException, NetworkException, PayloadException,
            IOException, ImapException {
        test1();
        String folder = CmsUtils.contactToCmsFolder(SmsIntegrationUtils.Test1.contact);
        // delete mailbox on CMS
        mImapCmsUtilTest.deleteRemoteMailbox(folder);
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateXmsReadStatus(SmsIntegrationUtils.Test1.contact, sms.getMessageId(),
                    CmsData.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        mSyncStrategy.execute();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsData.ReadStatus.READ_REPORT_REQUESTED, cmsObject.getReadStatus());
        }
        for (XmsDataObject sms : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.SMS, sms.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
        }
        mSyncStrategy.execute();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test5
     * <ul>
     * <li>Step 1 : mark conversation as seen_requested in local storage</li>
     * <li>Step 2 : start sync</li>
     * <li>step 3 : check that conversation is marked as seen</li>
     * </ul>
     */
    public void test5() throws FileAccessException, NetworkException, PayloadException {
        test1();
        String folder = CmsUtils.contactToCmsFolder(SmsIntegrationUtils.Test1.contact);
        // mark messages as deleted on server and expunge them.
        mImapCmsUtilTest.updateRemoteFlags(Test1.folderName, Test5.cmsObjectDeletedRequested);
        mImapCmsUtilTest.purgeDeleteRemoteMessages(folder);
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateXmsReadStatus(SmsIntegrationUtils.Test1.contact, sms.getMessageId(),
                    CmsData.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        mSyncStrategy.execute();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsData.ReadStatus.READ, cmsObject.getReadStatus());
        }
        for (XmsDataObject sms : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.SMS, sms.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
        }
        mSyncStrategy.execute();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test6 : check correlation algorithm (messages having different content)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test6() throws FileAccessException, NetworkException, PayloadException {
        // create messages on CMS
        mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test1.conversation);
        // create messages in local storage
        for (SmsDataObject sms : SmsIntegrationUtils.Test1.conversation) {
            mXmsLog.addSms(sms);
            String folder = CmsUtils.contactToCmsFolder(sms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.SMS, folder, sms.getMessageId(),
                    PushStatus.PUSHED, CmsData.ReadStatus.READ, DeleteStatus.NOT_DELETED, null));
        }
        mSyncStrategy.execute();
        List<XmsDataObject> smss = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, smss.size());
        Map<Integer, CmsObject> cmss = mCmsLogTestIntegration.getMessages();
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, cmss.size());
        for (CmsObject cmsObject : cmss.values()) {
            assertNotNull(cmsObject.getUid());
        }
    }

    /**
     * Test6 : check correlation algorithm (messages having the same content)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test7() throws FileAccessException, NetworkException, PayloadException {
        // create messages on CMS
        mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test7.conversation);
        // create messages in local storage
        for (SmsDataObject sms : SmsIntegrationUtils.Test7.conversation) {
            mXmsLog.addSms(sms);
            String messageId = sms.getMessageId();
            String folder = CmsUtils.contactToCmsFolder(sms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.SMS, folder, messageId,
                    PushStatus.PUSHED, Test7.imapReadStatus, Test7.imapDeleteStatus, null));
        }
        mSyncStrategy.execute();
        List<XmsDataObject> sms = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(SmsIntegrationUtils.Test7.conversation.length, sms.size());
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName);
        assertEquals(SmsIntegrationUtils.Test7.conversation.length, imapData.size());
        Assert.assertEquals(imapData.get(4).getMessageId(), sms.get(0).getMessageId());
        Assert.assertEquals(imapData.get(3).getMessageId(), sms.get(1).getMessageId());
        Assert.assertEquals(imapData.get(2).getMessageId(), sms.get(2).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), sms.get(3).getMessageId());
    }

    /**
     * Test8 : check correlation algorithm (messages having the same content)<br>
     * The local storage has 3 messages (with the same content) whereas the CMS has only 2 messages<br>
     * --> The first local message will not be mapped with a message from CMS<br>
     * --> No download of message from CMS (imap network trace)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test8() throws FileAccessException, NetworkException, PayloadException {
        // create messages on CMS
        mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test8.conversation_remote);
        // create messages in local storage
        for (SmsDataObject sms : SmsIntegrationUtils.Test8.conversation_local) {
            mXmsLog.addSms(sms);
            String folder = CmsUtils.contactToCmsFolder(sms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.SMS, folder, sms.getMessageId(),
                    PushStatus.PUSHED, Test8.imapReadStatus, Test8.imapDeleteStatus, null));
        }
        mSyncStrategy.execute();
        List<XmsDataObject> sms = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName);
        Assert.assertEquals(Test8.conversation_local.length, sms.size());
        Assert.assertEquals(Test8.conversation_local.length, imapData.size());
        Assert.assertEquals(imapData.get(2).getMessageId(), sms.get(0).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), sms.get(1).getMessageId());
    }

    /**
     * Test9 : check correlation algorithm (messages having the same content) <br>
     * The local storage has 2 messages (with the same content) whereas the CMS has 2 messages with
     * different content<br>
     * --> The first local message will not be mapped with a message from CMS<br>
     * --> One message should be downloaded from CMS (imap network trace)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local</li>
     * <li>storage step 3 : start a sync</li>
     * </ul>
     */
    public void test9() throws FileAccessException, NetworkException, PayloadException {
        // create messages on CMS
        mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test9.conversation_remote);
        // create messages in local storage
        for (SmsDataObject sms : SmsIntegrationUtils.Test9.conversation_local) {
            mXmsLog.addSms(sms);
            String messageId = sms.getMessageId();
            String folder = CmsUtils.contactToCmsFolder(sms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.SMS, folder, messageId,
                    PushStatus.PUSHED, Test9.imapReadStatus, Test9.imapDeleteStatus, null));
        }
        mSyncStrategy.execute();
        List<XmsDataObject> sms = mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName);
        Assert.assertEquals(2 + 1, sms.size());
        Assert.assertEquals(2 + 1, imapData.size());
        Assert.assertEquals(imapData.get(2).getMessageId(), sms.get(0).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), sms.get(1).getMessageId());
        Assert.assertEquals(imapData.get(null).getMessageId(), sms.get(2).getMessageId());
    }

    /**
     * Test10 : multi contact
     */
    public void test10() throws FileAccessException, NetworkException, PayloadException {
        mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test10.conversation_1);
        mSyncStrategy.execute();
        mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test10.conversation_2);
        for (SmsDataObject sms : SmsIntegrationUtils.Test10.conversation_2) {
            mXmsLog.addSms(sms);
            String folder = CmsUtils.contactToCmsFolder(sms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.SMS, folder, sms.getMessageId(),
                    PushStatus.PUSHED, Test9.imapReadStatus, Test9.imapDeleteStatus, null));
        }
        mSyncStrategy.execute();
        mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test10.conversation_3);
        mSyncStrategy.execute();
        assertEquals(SmsIntegrationUtils.Test10.conversation_1.length,
                mLogUtilTest
                        .getMessages(MimeType.TEXT_MESSAGE, SmsIntegrationUtils.Test10.contact1)
                        .size());
        assertEquals(SmsIntegrationUtils.Test10.conversation_2.length,
                mLogUtilTest
                        .getMessages(MimeType.TEXT_MESSAGE, SmsIntegrationUtils.Test10.contact2)
                        .size());
        assertEquals(SmsIntegrationUtils.Test10.conversation_3.length,
                mLogUtilTest
                        .getMessages(MimeType.TEXT_MESSAGE, SmsIntegrationUtils.Test10.contact3)
                        .size());

        assertEquals(SmsIntegrationUtils.Test10.conversation_1.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test10.folder1).size());

        Map<Integer, CmsObject> messages = mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test10.folder2);
        assertEquals(SmsIntegrationUtils.Test10.conversation_2.length, messages.size());
        assertEquals(SmsIntegrationUtils.Test10.conversation_3.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test10.folder3).size());
    }

    /**
     * Check correlation of SMS message:<br>
     * <ul>
     * <li>the message has a trailing space and a quote in it.
     * <li>the message is unseen on CMS but read report requested on local
     * </ul>
     */
    public void test11() throws FileAccessException, NetworkException, PayloadException {
        ContactId contact = SmsIntegrationUtils.Test11.contact;
        String folder = SmsIntegrationUtils.Test11.folder;
        SmsDataObject sms = SmsIntegrationUtils.Test11.smsDataObject;
        mImapCmsUtilTest.createRemoteXmsMessages(new XmsDataObject[] {
            sms
        });
        SmsDataObject localSms = new SmsDataObject(sms.getMessageId(), contact, sms.getBody(),
                sms.getDirection(), RcsService.ReadStatus.READ, sms.getTimestamp(),
                sms.getTimestampSent(), sms.getTimestampDelivered());
        mXmsLog.addSms(localSms);
        mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.SMS, folder, localSms.getMessageId(),
                PushStatus.PUSH_REQUESTED, CmsData.ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.NOT_DELETED, null));
        mSyncStrategy.execute();
        /*
         * Checks that message is correctly correlated. Checks that the local flags take precedence
         * to the remote ones.
         */
        assertEquals(1,
                mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE, SmsIntegrationUtils.Test11.contact)
                        .size());
        CmsObject cmsObject = mCmsLogTestIntegration.getMessage(folder, localSms.getMessageId());
        // Check that message is seen as pushed and read (correlated)
        assertNotNull(cmsObject.getUid());
        assertEquals(CmsData.ReadStatus.READ, cmsObject.getReadStatus());
        assertEquals(DeleteStatus.NOT_DELETED, cmsObject.getDeleteStatus());
        assertEquals(PushStatus.PUSHED, cmsObject.getPushStatus());
    }

    public void testLoad() throws FileAccessException, NetworkException, PayloadException {
        for (int i = 0; i < TestLoad.iteration; i++) {
            mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.TestLoad.conversation_1);
            mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test10.conversation_2);
            for (SmsDataObject sms : SmsIntegrationUtils.Test10.conversation_2) {
                String msgId = IdGenerator.generateMessageID();
                mXmsLog.addSms(new SmsDataObject(msgId, sms.getContact(), sms.getBody(), sms
                        .getDirection(), sms.getReadStatus(), sms.getTimestamp(), sms
                        .getNativeProviderId(), sms.getNativeThreadId()));
                String folder = CmsUtils.contactToCmsFolder(sms.getContact());
                mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.SMS, folder, msgId,
                        PushStatus.PUSHED,
                        sms.getReadStatus() == RcsService.ReadStatus.READ ? CmsData.ReadStatus.READ
                                : CmsData.ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, sms
                                .getNativeThreadId()));
            }
            mImapCmsUtilTest.createRemoteXmsMessages(SmsIntegrationUtils.Test10.conversation_3);
        }
        mSyncStrategy.execute();
        Assert.assertEquals(
                TestLoad.iteration * SmsIntegrationUtils.TestLoad.conversation_1.length,
                mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                        SmsIntegrationUtils.TestLoad.contact1).size());
        Assert.assertEquals(
                TestLoad.iteration * SmsIntegrationUtils.TestLoad.conversation_2.length,
                mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                        SmsIntegrationUtils.TestLoad.contact2).size());
        Assert.assertEquals(
                TestLoad.iteration * SmsIntegrationUtils.TestLoad.conversation_3.length,
                mLogUtilTest.getMessages(MimeType.TEXT_MESSAGE,
                        SmsIntegrationUtils.TestLoad.contact3).size());
    }

}

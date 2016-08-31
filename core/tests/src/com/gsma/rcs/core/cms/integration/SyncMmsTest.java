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
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test1;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test2;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test5;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test7;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test8;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test9;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.TestLoad;
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
import com.gsma.rcs.provider.smsmms.SmsMmsLog;
import com.gsma.rcs.provider.xms.PartData;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SyncMmsTest extends AndroidTestCase {

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
        RcsSettings settings = RcsSettingsMock.getMockSettings(getContext());
        AndroidFactory.setApplicationContext(mContext, settings);
        mCmsLog = CmsLog.getInstance(mContext);
        SmsMmsLog smsMmsLog = SmsMmsLog.getInstance(mContext, mContext.getContentResolver());
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(mContext);
        mLocalContentResolver = new LocalContentResolver(mContext);
        mXmsLog = XmsLog.getInstance(mContext, settings, mLocalContentResolver);
        MessagingLog messagingLog = MessagingLog.getInstance(new LocalContentResolver(mContext),
                settings);
        mLogUtilTest = SyncLogUtilTest.getInstance(mContext);
        CmsSessionController cmsSessionCtrl = new CmsSessionController(mContext, null, settings,
                mLocalContentResolver, mXmsLog, messagingLog, mCmsLog, null);
        InstantMessagingService imService = new InstantMessagingService(null, settings, null,
                messagingLog, null, mLocalContentResolver, mContext, null, cmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, messagingLog, null, settings,
                null, cmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, messagingLog, settings, null, mContext, cmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, mXmsLog, smsMmsLog);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, cmsSessionCtrl, chatService,
                fileTransferService, imService, mXmsLog, xmsManager, mLocalContentResolver,
                smsMmsLog);
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
        mLocalContentResolver.delete(PartData.CONTENT_URI, null, null);
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
        mImapCmsUtilTest.createRemoteXmsMessages(Test1.conversation);
        mSyncStrategy.execute();
        // check that messages are present in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        int nbFolders = mCmsLog.getFolders().size();
        assertTrue(nbFolders > 0);
        imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        assertEquals(Test1.conversation.length, imapData.size());
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        assertEquals(Test1.conversation.length, messages.size());
        for (XmsDataObject message : messages) {
            Assert.assertEquals(Test1.readStatus, message.getReadStatus());
        }
        mSyncStrategy.execute();
        // check that messages are present in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        assertEquals(nbFolders, mCmsLog.getFolders().size());
        imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        assertEquals(Test1.conversation.length, imapData.size());
    }

    /**
     * Test2 Test 1 + Step 0 : mark conversation as seen on CMS
     * <ul>
     * <li>step 1 : start a sync : messages are marked as seen in local storage</li>
     * <li>Step 2 : mark conversation as deleted on CMS</li>
     * <li>step 3 : start a sync : messages are marked as deleted in local storage</li>
     * </ul>
     */
    public void test2() throws NetworkException, PayloadException, FileAccessException {
        test1();
        // update messages with 'seen' flag on CMS
        mImapCmsUtilTest.updateRemoteFlags(Test1.folderName, Test2.cmsObjectReadRequested);
        // sync with CMS
        mSyncStrategy.execute();
        // check that messages are marked as 'Seen' in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        int nbFolders = mCmsLog.getFolders().size();
        assertTrue(nbFolders > 0);
        assertEquals(Test1.conversation.length, mCmsLogTestIntegration
                .getMessages(Test1.folderName).size());
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        assertEquals(Test1.conversation.length, messages.size());
        for (XmsDataObject message : messages) {
            Assert.assertEquals(ReadStatus.READ, message.getReadStatus());
        }
        // update messages with 'deleted' flag on CMS
        mImapCmsUtilTest.updateRemoteFlags(Test1.folderName, Test2.cmsObjectDeletedRequested);
        // sync with CMS
        mSyncStrategy.execute();
        // check that messages are marked as 'Deleted' in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        assertEquals(nbFolders, mCmsLog.getFolders().size());
        assertEquals(Test1.conversation.length, mCmsLogTestIntegration
                .getMessages(Test1.folderName).size());
        messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
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
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        for (XmsDataObject msg : messages) {
            mCmsLog.updateXmsReadStatus(Test1.contact, msg.getMessageId(),
                    CmsData.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        // sync with CMS : during this first sync, messages are marked as 'Seen' on CMS
        mSyncStrategy.execute();
        String folder = CmsUtils.contactToCmsFolder(Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsData.ReadStatus.READ, cmsObject.getReadStatus());
        }
        messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (XmsDataObject msg : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.MMS, msg.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
        }
        // sync with CMS : during this first sync, messages are marked as 'Deleted' on CMS
        mSyncStrategy.execute();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test4 Test 1 + Step 0 : delete mailbox from CMS
     * <ul>
     * <li>Step 1 : mark conversation as seen_requested in local storage</li>
     * <li>Step 2 : start sync</li>
     * <li>step 3 : check that conversation is marked as seen</li>
     * </ul>
     */
    public void test4() throws NetworkException, PayloadException, FileAccessException,
            IOException, ImapException {
        test1();
        // delete mailbox on CMS
        mImapCmsUtilTest.deleteRemoteMailbox(CmsUtils.contactToCmsFolder(Test1.contact));
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateXmsReadStatus(Test1.contact, sms.getMessageId(),
                    CmsData.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        mSyncStrategy.execute();
        String folder = CmsUtils.contactToCmsFolder(Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsData.ReadStatus.READ_REPORT_REQUESTED, cmsObject.getReadStatus());
        }
        for (XmsDataObject sms : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.MMS, sms.getMessageId(),
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
    public void test5() throws NetworkException, FileAccessException, PayloadException {
        test1();
        // mark messages as deleted on server and expunge them.
        mImapCmsUtilTest.updateRemoteFlags(Test1.folderName, Test5.cmsObjectDeletedRequested);
        mImapCmsUtilTest.purgeDeleteRemoteMessages(CmsUtils.contactToCmsFolder(Test1.contact));
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateXmsReadStatus(Test1.contact, sms.getMessageId(),
                    CmsData.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        mSyncStrategy.execute();
        String folder = CmsUtils.contactToCmsFolder(Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsData.ReadStatus.READ, cmsObject.getReadStatus());
        }
        for (XmsDataObject sms : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.MMS, sms.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
        }
        mSyncStrategy.execute();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test6 : check correlation algorithm (messages having the same mmsId)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test6() throws FileAccessException, NetworkException, PayloadException {
        // create messages on CMS
        mImapCmsUtilTest.createRemoteXmsMessages(Test1.conversation);
        // create messages in local storage
        for (MmsDataObject mms : Test1.conversation) {
            if (RcsService.Direction.OUTGOING == mms.getDirection()) {
                mXmsLog.addOutgoingMms(mms);
            } else {
                mXmsLog.addIncomingMms(mms);
            }
            String folder = CmsUtils.contactToCmsFolder(mms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.MMS, folder, mms.getMessageId(),
                    PushStatus.PUSHED, CmsData.ReadStatus.READ, DeleteStatus.NOT_DELETED, mms
                            .getNativeId()));
        }
        mSyncStrategy.execute();
        assertEquals(Test1.conversation.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE, Test1.contact).size());
    }

    /**
     * Test6 : check correlation algorithm (messages having the same mmsId)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test7() throws FileAccessException, NetworkException, PayloadException {
        // create messages on CMS
        mImapCmsUtilTest.createRemoteXmsMessages(Test7.conversation);
        // create messages in local storage
        for (MmsDataObject mms : Test7.conversation) {
            if (RcsService.Direction.OUTGOING == mms.getDirection()) {
                mXmsLog.addOutgoingMms(mms);
            } else {
                mXmsLog.addIncomingMms(mms);
            }
            String messageId = mms.getMessageId();
            String folder = CmsUtils.contactToCmsFolder(mms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.MMS, folder, messageId,
                    PushStatus.PUSHED, Test7.imapReadStatus, Test7.imapDeleteStatus, mms
                            .getNativeId()));
        }
        mSyncStrategy.execute();
        List<XmsDataObject> mms = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Assert.assertEquals(Test7.conversation.length, mms.size());
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        Assert.assertEquals(Test7.conversation.length, imapData.size());
        Assert.assertEquals(imapData.get(4).getMessageId(), mms.get(0).getMessageId());
        Assert.assertEquals(imapData.get(3).getMessageId(), mms.get(1).getMessageId());
        Assert.assertEquals(imapData.get(2).getMessageId(), mms.get(2).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), mms.get(3).getMessageId());
    }

    /**
     * Test8 : check correlation algorithm (messages having the same content)<br>
     * The local storage has 3 messages (with the same content) whereas the CMS has only 2 messages.<br>
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
        mImapCmsUtilTest.createRemoteXmsMessages(Test8.conversation_remote);
        // create messages in local storage
        for (MmsDataObject mms : Test8.conversation_local) {
            if (RcsService.Direction.OUTGOING == mms.getDirection()) {
                mXmsLog.addOutgoingMms(mms);
            } else {
                mXmsLog.addIncomingMms(mms);
            }
            String folder = CmsUtils.contactToCmsFolder(mms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.MMS, folder, mms.getMessageId(),
                    PushStatus.PUSHED, Test8.imapReadStatus, Test8.imapDeleteStatus, mms
                            .getNativeId()));
        }
        mSyncStrategy.execute();
        List<XmsDataObject> mms = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        Assert.assertEquals(Test8.conversation_local.length, mms.size());
        Assert.assertEquals(Test8.conversation_local.length, imapData.size());
        Assert.assertEquals(imapData.get(null).getMessageId(), "messageId3");
        Assert.assertEquals(imapData.get(2).getMessageId(), mms.get(1).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), mms.get(2).getMessageId());
    }

    /**
     * Test9 : check correlation algorithm (messages having the mmsId)<br>
     * The local storage has 2 messages whereas the CMS has 2 messages (one is new)<br>
     * --> One local message will be mapped with a message from CMS<br>
     * --> One message should be downloaded from CMS (imap network trace)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test9() throws FileAccessException, NetworkException, PayloadException {
        // create messages on CMS
        mImapCmsUtilTest.createRemoteXmsMessages(Test9.conversation_remote);
        // create messages in local storage
        for (MmsDataObject mms : Test9.conversation_local) {
            if (RcsService.Direction.OUTGOING == mms.getDirection()) {
                mXmsLog.addOutgoingMms(mms);
            } else {
                mXmsLog.addIncomingMms(mms);
            }
            String messageId = mms.getMessageId();
            String folder = CmsUtils.contactToCmsFolder(mms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.MMS, folder, messageId,
                    PushStatus.PUSHED, Test9.imapReadStatus, Test9.imapDeleteStatus, mms
                            .getNativeId()));
        }
        mSyncStrategy.execute();
        List<XmsDataObject> mms = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        Assert.assertEquals(2 + 1, mms.size());
        Assert.assertEquals(2 + 1, imapData.size());
        Assert.assertEquals(imapData.get(1).getMessageId(), mms.get(2).getMessageId());
        Assert.assertEquals(imapData.get(null).getMessageId(), "messageId2");
    }

    /**
     * Test10 : multi contacts
     */
    public void test10() throws NetworkException, FileAccessException, PayloadException {
        mImapCmsUtilTest.createRemoteXmsMessages(MmsIntegrationUtils.Test10.conversation_1);
        mSyncStrategy.execute();
        mImapCmsUtilTest.createRemoteXmsMessages(MmsIntegrationUtils.Test10.conversation_2);
        for (MmsDataObject mms : MmsIntegrationUtils.Test10.conversation_2) {
            mXmsLog.addOutgoingMms(mms);
            String folder = CmsUtils.contactToCmsFolder(mms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.MMS, folder, mms.getMessageId(),
                    PushStatus.PUSHED, Test9.imapReadStatus, Test9.imapDeleteStatus, mms
                            .getNativeId()));
        }
        mSyncStrategy.execute();
        mImapCmsUtilTest.createRemoteXmsMessages(MmsIntegrationUtils.Test10.conversation_3);
        mSyncStrategy.execute();
        assertEquals(
                MmsIntegrationUtils.Test10.conversation_1.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                        MmsIntegrationUtils.Test10.contact1).size());
        assertEquals(
                MmsIntegrationUtils.Test10.conversation_2.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                        MmsIntegrationUtils.Test10.contact2).size());
        assertEquals(
                MmsIntegrationUtils.Test10.conversation_3.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                        MmsIntegrationUtils.Test10.contact3).size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_1.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test10.folder1).size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_2.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test10.folder2).size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_3.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test10.folder3).size());
    }

    /**
     * Test11 : multi contacts : mms with multiple recipients
     */
    public void test11() throws NetworkException, FileAccessException, PayloadException {
        String mmsId = MmsIntegrationUtils.Test11.conversation_1[0].getMessageId();
        mImapCmsUtilTest.createRemoteXmsMessages(MmsIntegrationUtils.Test11.conversation_1);
        mImapCmsUtilTest.createRemoteXmsMessages(MmsIntegrationUtils.Test11.conversation_2);
        mImapCmsUtilTest.createRemoteXmsMessages(MmsIntegrationUtils.Test11.conversation_3);
        mSyncStrategy.execute();
        assertEquals(
                MmsIntegrationUtils.Test11.conversation_1.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                        MmsIntegrationUtils.Test11.contact1).size());
        assertEquals(
                MmsIntegrationUtils.Test11.conversation_2.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                        MmsIntegrationUtils.Test11.contact2).size());
        assertEquals(
                MmsIntegrationUtils.Test11.conversation_3.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                        MmsIntegrationUtils.Test11.contact3).size());
        Assert.assertEquals(MmsIntegrationUtils.Test11.conversation_1.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test11.folder1).size());
        Assert.assertEquals(MmsIntegrationUtils.Test11.conversation_2.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test11.folder2).size());
        Assert.assertEquals(MmsIntegrationUtils.Test11.conversation_3.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test11.folder3).size());
        // mark messages as deleted on server and expunge them.
        mImapCmsUtilTest.updateRemoteFlags(MmsIntegrationUtils.Test11.folder1,
                MmsIntegrationUtils.Test11.cmsObjectDeletedRequested);
        mSyncStrategy.execute();
        // check that conversation_1 message is marked as 'Deleted' in local storage
        assertEquals(3, mCmsLog.getFolders().size());
        Map<Integer, CmsObject> cmsMessages = mCmsLogTestIntegration
                .getMessages(MmsIntegrationUtils.Test11.folder1);
        assertEquals(MmsIntegrationUtils.Test11.conversation_1.length, cmsMessages.size());
        CmsObject cmsObject1 = cmsMessages.get(1);
        assertEquals(mmsId, cmsObject1.getMessageId());
        assertEquals(DeleteStatus.DELETED, cmsObject1.getDeleteStatus());
        // check that conversation_2 message is marked as 'Not Deleted' in local storage
        cmsMessages = mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test11.folder2);
        assertEquals(MmsIntegrationUtils.Test11.conversation_2.length, cmsMessages.size());
        CmsObject cmsObject2 = cmsMessages.get(1);
        assertEquals(mmsId, cmsObject2.getMessageId());
        assertEquals(DeleteStatus.NOT_DELETED, cmsObject2.getDeleteStatus());
        // Check that message is deleted from xms
        List<XmsDataObject> messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                MmsIntegrationUtils.Test11.contact1);
        assertTrue(messages.isEmpty());
        messages = mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                MmsIntegrationUtils.Test11.contact2);
        assertEquals(1, messages.size());
        // check that parts are noy deleted
        assertTrue(mXmsLog.isPartPersisted(mmsId));
        // mark messages as deleted on server and expunge them.
        mImapCmsUtilTest.updateRemoteFlags(MmsIntegrationUtils.Test11.folder2,
                MmsIntegrationUtils.Test11.cmsObjectDeletedRequested);
        mImapCmsUtilTest.updateRemoteFlags(MmsIntegrationUtils.Test11.folder3,
                MmsIntegrationUtils.Test11.cmsObjectDeletedRequested);
        mSyncStrategy.execute();
        // check that parts are deleted
        assertFalse(mXmsLog.isPartPersisted(mmsId));
    }

    public void testLoad() throws FileAccessException, NetworkException, PayloadException {
        mImapCmsUtilTest.createRemoteXmsMessages(TestLoad.conversation_1);
        mImapCmsUtilTest.createRemoteXmsMessages(TestLoad.conversation_2);
        for (MmsDataObject mms : TestLoad.conversation_2) {
            mXmsLog.addOutgoingMms(new MmsDataObject(mms.getMessageId(), mms.getContact(), mms
                    .getSubject(), mms.getDirection(), ReadStatus.UNREAD, mms.getTimestamp(), mms
                    .getNativeId(), mms.getMmsParts()));
            String folder = CmsUtils.contactToCmsFolder(mms.getContact());
            mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.MMS, folder, mms.getMessageId(),
                    PushStatus.PUSHED, CmsData.ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, mms
                            .getNativeId()));
        }
        mImapCmsUtilTest.createRemoteXmsMessages(TestLoad.conversation_3);
        mSyncStrategy.execute();
        assertEquals(TestLoad.conversation_1.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact1).size());
        assertEquals(TestLoad.conversation_2.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact2).size());
        assertEquals(TestLoad.conversation_3.length,
                mLogUtilTest.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact3).size());
    }

}

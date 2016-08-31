/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.im.chat;

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.core.cms.event.XmsEventHandler;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.messaging.OneToOneChatMessageDeleteTask;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.smsmms.SmsMmsLog;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.InstrumentationTestCase;

/**
 * @author Philippe LEMORDANT
 */
public class OneToOneChatMessageDeleteTaskTest extends InstrumentationTestCase {

    private LocalContentResolver mLocalContentResolver;
    private CmsSessionController mCmsSessionCtrl;
    private CmsLog mCmsLog;
    private CmsObject mCmsObjectMsg;
    private ChatMessage mChatMsg;
    private MessagingLog mMessagingLog;
    private ChatServiceImpl mChatService;
    private InstantMessagingService mImService;
    private ChatMessage mChatMsg1;
    private ChatMessage mChatMsg2;
    private CmsObject mCmsObjectMsg1;
    private CmsObject mCmsObjectMsg2;

    protected void setUp() throws Exception {
        super.setUp();
        Context mContext = getInstrumentation().getContext();
        RcsSettings settings = RcsSettingsMock.getMockSettings(mContext);
        RcsSettingsMock.setInvalidCmsServer();
        assertNull(settings.getMessageStoreUri());
        mLocalContentResolver = new LocalContentResolver(mContext.getContentResolver());
        XmsLog mXmsLog = XmsLog.getInstance(mContext, settings, mLocalContentResolver);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));

        ContactId mContact1 = contactUtils.formatContact("+33000000001");
        String mFolder1 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact1);
        ContactId mContact2 = contactUtils.formatContact("+33000000002");
        String mFolder2 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact2);

        long timestamp = 1;
        mChatMsg = new ChatMessage("msg_id-mChatMsg", mContact1, "body-mChatMsg",
                ChatLog.Message.MimeType.TEXT_MESSAGE, timestamp++, timestamp++, "display");
        mCmsObjectMsg = new CmsRcsObject(MessageType.CHAT_MESSAGE, mFolder1,
                mChatMsg.getMessageId(), 1, PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.NOT_DELETED, null);
        mChatMsg1 = new ChatMessage("msg_id-mChatMsg1", mContact2, "body-mChatMsg1",
                ChatLog.Message.MimeType.TEXT_MESSAGE, timestamp++, timestamp++, "display");
        mCmsObjectMsg1 = new CmsRcsObject(MessageType.CHAT_MESSAGE, mFolder2,
                mChatMsg1.getMessageId(), 2, PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.NOT_DELETED, null);
        mChatMsg2 = new ChatMessage("msg_id-mChatMsg2", mContact2, "body-mChatMsg2",
                ChatLog.Message.MimeType.TEXT_MESSAGE, timestamp++, timestamp, "display");
        mCmsObjectMsg2 = new CmsRcsObject(MessageType.CHAT_MESSAGE, mFolder2,
                mChatMsg2.getMessageId(), 3, PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.NOT_DELETED, null);
        mCmsLog = CmsLog.getInstance(mContext);
        SmsMmsLog smsMmsLog = SmsMmsLog.getInstance(mContext, mContext.getContentResolver());
        mMessagingLog = MessagingLog.getInstance(mLocalContentResolver, settings);
        mCmsSessionCtrl = new CmsSessionController(mContext, null, settings, mLocalContentResolver,
                mXmsLog, mMessagingLog, mCmsLog, null);
        mImService = new InstantMessagingService(null, settings, null, mMessagingLog, null,
                mLocalContentResolver, mContext, null, mCmsSessionCtrl);
        mChatService = new ChatServiceImpl(mImService, mMessagingLog, null, settings, null,
                mCmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, mXmsLog, smsMmsLog);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(mImService,
                mChatService, mMessagingLog, settings, null, mContext, mCmsSessionCtrl);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, mCmsSessionCtrl, mChatService,
                fileTransferService, mImService, mXmsLog, xmsManager, mLocalContentResolver,
                smsMmsLog);
        mCmsSessionCtrl.register(cmsServiceImpl, mChatService, fileTransferService, mImService);
        XmsEventHandler xmsEventHandler = new XmsEventHandler(mCmsLog, mXmsLog, settings,
                cmsServiceImpl);
        mCmsSessionCtrl.initialize(xmsEventHandler);
        mCmsSessionCtrl.start();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mMessagingLog.deleteAllEntries();
        mCmsLog.removeMessages();
        mCmsSessionCtrl.stop(ImsServiceSession.TerminationReason.TERMINATION_BY_SYSTEM);
        RcsSettingsMock.restoreSettings();
    }

    private void addMessageToCmsLog(CmsObject cmsObject) {
        if (CmsObject.isXmsData(cmsObject.getMessageType())) {
            mCmsLog.addXmsMessage((CmsXmsObject) cmsObject);
        } else {
            mCmsLog.addRcsMessage((CmsRcsObject) cmsObject);
        }
    }

    public void testOneToOneChatMessageDeleteTaskOneSpecific_action() {
        mMessagingLog.addIncomingOneToOneChatMessage(mChatMsg, null, false);
        addMessageToCmsLog(mCmsObjectMsg);
        assertTrue(mMessagingLog.isMessagePersisted(mChatMsg.getMessageId()));
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(mChatMsg.getMessageId());
        assertEquals(mCmsObjectMsg, cmsObjectFromDb);

        OneToOneChatMessageDeleteTask task = new OneToOneChatMessageDeleteTask(mChatService,
                mImService, mLocalContentResolver, mChatMsg.getMessageId(), mCmsSessionCtrl);
        task.run();

        assertFalse(mMessagingLog.isMessagePersisted(mChatMsg.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
    }

    public void testOneToOneChatMessageDeleteTaskOneSpecific_event() {
        mMessagingLog.addIncomingOneToOneChatMessage(mChatMsg, null, false);
        addMessageToCmsLog(mCmsObjectMsg);
        assertTrue(mMessagingLog.isMessagePersisted(mChatMsg.getMessageId()));
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(mChatMsg.getMessageId());
        assertEquals(mCmsObjectMsg, cmsObjectFromDb);

        OneToOneChatMessageDeleteTask task = new OneToOneChatMessageDeleteTask(mChatService,
                mImService, mLocalContentResolver, mChatMsg.getMessageId(), mCmsLog);
        task.run();

        assertFalse(mMessagingLog.isMessagePersisted(mChatMsg.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg.getMessageId());
        assertEquals(DeleteStatus.DELETED, cmsObjectFromDb.getDeleteStatus());
    }

    public void testOneToOneChatMessageDeleteTaskConversation() {
        mMessagingLog.addIncomingOneToOneChatMessage(mChatMsg1, null, false);
        addMessageToCmsLog(mCmsObjectMsg1);
        assertTrue(mMessagingLog.isMessagePersisted(mChatMsg1.getMessageId()));
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(mChatMsg1.getMessageId());
        assertEquals(mCmsObjectMsg1, cmsObjectFromDb);

        mMessagingLog.addIncomingOneToOneChatMessage(mChatMsg2, null, false);
        addMessageToCmsLog(mCmsObjectMsg2);
        assertTrue(mMessagingLog.isMessagePersisted(mChatMsg2.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg2.getMessageId());
        assertEquals(mCmsObjectMsg2, cmsObjectFromDb);

        OneToOneChatMessageDeleteTask task = new OneToOneChatMessageDeleteTask(mChatService,
                mImService, mLocalContentResolver, mChatMsg1.getRemoteContact(), mCmsSessionCtrl);
        task.run();

        assertFalse(mMessagingLog.isMessagePersisted(mChatMsg1.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg1.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mMessagingLog.isMessagePersisted(mChatMsg2.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg2.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
    }

    public void testOneToOneChatMessageDeleteTaskAll() {
        mMessagingLog.addIncomingOneToOneChatMessage(mChatMsg, null, false);
        assertTrue(mMessagingLog.isMessagePersisted(mChatMsg.getMessageId()));
        addMessageToCmsLog(mCmsObjectMsg);
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatData(mChatMsg.getMessageId());
        assertEquals(mCmsObjectMsg, cmsObjectFromDb);

        mMessagingLog.addIncomingOneToOneChatMessage(mChatMsg1, null, false);
        addMessageToCmsLog(mCmsObjectMsg1);
        assertTrue(mMessagingLog.isMessagePersisted(mChatMsg1.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg1.getMessageId());
        assertEquals(mCmsObjectMsg1, cmsObjectFromDb);

        mMessagingLog.addIncomingOneToOneChatMessage(mChatMsg2, null, false);
        addMessageToCmsLog(mCmsObjectMsg2);
        assertTrue(mMessagingLog.isMessagePersisted(mChatMsg2.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg2.getMessageId());
        assertEquals(mCmsObjectMsg2, cmsObjectFromDb);

        OneToOneChatMessageDeleteTask task = new OneToOneChatMessageDeleteTask(mChatService,
                mImService, mLocalContentResolver, mCmsSessionCtrl);
        task.run();

        assertFalse(mMessagingLog.isMessagePersisted(mChatMsg.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mMessagingLog.isMessagePersisted(mChatMsg1.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg1.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mMessagingLog.isMessagePersisted(mChatMsg2.getMessageId()));
        cmsObjectFromDb = mCmsLog.getChatData(mChatMsg2.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
    }

}

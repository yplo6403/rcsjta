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

package com.gsma.rcs.provider.cms;

import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.ContentProviderResult;
import android.content.Context;
import android.test.AndroidTestCase;

import java.util.Map;

public class CmsLogTest extends AndroidTestCase {

    private CmsLog mCmsLog;
    private CmsLogTestIntegration mCmsLogTestIntegration;

    private CmsFolder[] mFolders;
    private CmsObject[] mMessages;
    private ContactId mContact1;
    private ContactId mContact2;
    private String mFolder1;
    private String mFolder2;
    private String mFolder3;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        mCmsLog = CmsLog.getInstance(context);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(context);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(context));
        mContact1 = contactUtils.formatContact("+330000000001");
        mContact2 = contactUtils.formatContact("+330000000002");
        ContactId mContact3 = contactUtils.formatContact("+330000000003");
        String chatId4 = "chatId4";
        mFolder1 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact1);
        mFolder2 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact2);
        mFolder3 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact3);
        String mFolder4 = com.gsma.rcs.core.cms.utils.CmsUtils.groupChatToCmsFolder(chatId4,
                chatId4);
        mFolders = new CmsFolder[] {
                new CmsFolder(mFolder1, 1, 123, 1), new CmsFolder(mFolder2, 1, 1234, 1),
                new CmsFolder(mFolder3, 1, 12345, 1), new CmsFolder(mFolder4, 1, 123456, 1)
        };
        mMessages = new CmsObject[] {
                new CmsObject(mFolder1, 1, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.SMS, "messageId1", null),
                new CmsObject(mFolder2, 1, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.SMS, "messageId1", null),
                new CmsObject(mFolder2, 2, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.SMS, "messageId2", null),
                new CmsObject(mFolder3, 1, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.SMS, "messageId1", null),
                new CmsObject(mFolder3, 2, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.SMS, "messageId2", null),
                new CmsObject(mFolder3, 3, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.SMS, "messageId3", null),
                new CmsObject(mFolder4, 3, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.CHAT_MESSAGE, "messageId1", 1L)
        };
        mCmsLog.removeFolders(false);
        mCmsLog.removeMessages();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mCmsLog.removeFolders(false);
        mCmsLog.removeMessages();
    }

    public void testAddFolder() {
        CmsFolder folder = mFolders[0];
        mCmsLog.addFolder(folder);
        CmsFolder folderFromLog = mCmsLogTestIntegration.getFolder(folder.getName());
        assertEquals(folder, folderFromLog);
        folderFromLog = mCmsLogTestIntegration.getFolder("dummy");
        assertNull(folderFromLog);
    }

    public void testIsFolderPersisted() {
        CmsFolder folder = mFolders[0];
        mCmsLog.addFolder(folder);
        assertTrue(mCmsLog.isFolderPersisted(folder.getName()));
    }

    public void testAddMessage() {
        CmsObject message = mMessages[0];
        assertEquals((Integer) 0, mCmsLog.getMaxUidForMessages(message.getFolder()));
        mCmsLog.addMessage(message);
        assertEquals((Integer) 1, mCmsLog.getMaxUidForMessages(message.getFolder()));
        CmsObject msgFromLog = mCmsLog.getMessage(mFolder1, message.getUid());
        assertEquals(message, msgFromLog);
        msgFromLog = mCmsLog.getMessage("dummy", 0);
        assertNull(msgFromLog);
    }

    public void testGetFolders() {
        Map<String, CmsFolder> folders = mCmsLog.getFolders();
        assertEquals(0, folders.size());

        mCmsLog.addFolder(mFolders[0]);
        folders = mCmsLog.getFolders();
        assertEquals(1, folders.size());
        assertTrue(folders.containsKey(mFolder1));

        mCmsLog.addFolder(mFolders[0]);
        folders = mCmsLog.getFolders();
        assertEquals(1, folders.size());
        assertTrue(folders.containsKey(mFolder1));

        mCmsLog.addFolder(mFolders[1]);
        folders = mCmsLog.getFolders();
        assertEquals(2, folders.size());
        assertTrue(folders.containsKey(mFolder1));
        assertTrue(folders.containsKey(mFolder2));

        mCmsLog.addFolder(mFolders[2]);
        folders = mCmsLog.getFolders();
        assertEquals(3, folders.size());
        assertTrue(folders.containsKey(mFolder1));
        assertTrue(folders.containsKey(mFolder2));
        assertTrue(folders.containsKey(mFolder3));
    }

    public void testUpdateFolderSingle() {
        CmsFolder folder = mFolders[0];
        mCmsLog.addFolder(folder);
        assertTrue(mCmsLog.isFolderPersisted(folder.getName()));

        CmsFolder folderFromLog = mCmsLogTestIntegration.getFolder(folder.getName());
        assertEquals(folder, folderFromLog);

        mCmsLog.updateFolder(new CmsFolder(folder.getName(), 0, 0, 0));
        folderFromLog = mCmsLogTestIntegration.getFolder(folder.getName());
        assertEquals(1, mCmsLog.getFolders().size());
        assertEquals((Integer) 0, folderFromLog.getModseq());
        assertEquals((Integer) 0, folderFromLog.getNextUid());
        assertEquals((Integer) 0, folderFromLog.getUidValidity());
    }

    public void testUpdateFolderGroup() {
        CmsFolder groupFolder = mFolders[3];
        String folderName = groupFolder.getName();
        mCmsLog.addFolder(groupFolder);
        assertTrue(mCmsLog.isFolderPersisted(folderName));

        CmsFolder folderFromLog = mCmsLogTestIntegration.getFolder(folderName);
        assertEquals(groupFolder, folderFromLog);

        mCmsLog.updateFolder(new CmsFolder(folderName, 0, 0, 0));

        folderFromLog = mCmsLogTestIntegration.getFolder(folderName);
        assertEquals(1, mCmsLog.getFolders().size());
        assertEquals((Integer) 0, folderFromLog.getModseq());
        assertEquals((Integer) 0, folderFromLog.getNextUid());
        assertEquals((Integer) 0, folderFromLog.getUidValidity());
    }

    public void testRemoveFolder() {
        for (CmsFolder folder : mFolders) {
            mCmsLog.addFolder(folder);
        }
        assertEquals(mFolders.length, mCmsLog.getFolders().size());
        for (int i = 0; i < mFolders.length; i++) {
            mCmsLog.removeFolder(mFolders[i].getName(), false);
            assertEquals(mFolders.length - (i + 1), mCmsLog.getFolders().size());
        }
    }

    public void testRemoveFolders() {
        for (CmsFolder folder : mFolders) {
            mCmsLog.addFolder(folder);
        }
        assertEquals(mFolders.length, mCmsLog.getFolders().size());
        mCmsLog.removeFolders(false);
        assertEquals(0, mCmsLog.getFolders().size());
    }

    public void testGetMessages() {
        Map<Integer, CmsObject> messages = mCmsLogTestIntegration.getMessages();
        assertEquals(0, messages.size());
        for (int i = 0; i < mMessages.length; i++) {
            mCmsLog.addMessage(mMessages[i]);
            assertEquals(i + 1, mCmsLogTestIntegration.getMessages().size());
        }
        messages = mCmsLogTestIntegration.getMessages(mFolder1);
        assertEquals(1, messages.size());
        assertEquals(mMessages[0], messages.get(mMessages[0].getUid()));

        messages = mCmsLogTestIntegration.getMessages(mFolder2);
        assertEquals(2, messages.size());
        assertEquals(mMessages[1], messages.get(mMessages[1].getUid()));
        assertEquals(mMessages[2], messages.get(mMessages[2].getUid()));

        messages = mCmsLogTestIntegration.getMessages(mFolder3);
        assertEquals(3, messages.size());
        assertEquals(mMessages[3], messages.get(mMessages[3].getUid()));
        assertEquals(mMessages[4], messages.get(mMessages[4].getUid()));
        assertEquals(mMessages[5], messages.get(mMessages[5].getUid()));

        for (CmsObject mMessage : mMessages) {
            assertEquals(mMessage, mCmsLog.getMessage(mMessage.getFolder(), mMessage.getUid()));
        }
        for (CmsObject mMessage : mMessages) {
            assertEquals(mMessage, mCmsLogTestIntegration.getMessage(mMessage.getFolder(),
                    mMessage.getMessageId()));
        }
        for (CmsObject mMessage : mMessages) {
            assertTrue(mCmsLog.getMessageId(mMessage.getFolder(), mMessage.getUid()) != CmsLog.INVALID_ID);
        }
    }

    public void testUpdateMessage() {
        CmsObject message = mMessages[0];
        mCmsLog.addMessage(message);
        CmsObject msgFromLog = mCmsLog.getMessage(message.getFolder(), message.getUid());
        assertEquals(message, msgFromLog);
        mCmsLog.addMessage(new CmsObject(message.getFolder(), 1, ReadStatus.READ,
                DeleteStatus.DELETED, PushStatus.PUSHED, MessageType.MMS, "messageId1", null));
        msgFromLog = mCmsLog.getMessage(message.getFolder(), message.getUid());
        assertEquals(1, mCmsLogTestIntegration.getMessages().size());
        assertEquals(DeleteStatus.DELETED, msgFromLog.getDeleteStatus());
        assertEquals(ReadStatus.READ, msgFromLog.getReadStatus());
        assertEquals(MessageType.MMS, msgFromLog.getMessageType());
        assertNull(msgFromLog.getNativeProviderId());
    }

    public void testRemoveMessage() {
        for (CmsObject message : mMessages) {
            mCmsLog.addMessage(message);
        }
        assertEquals(mMessages.length, mCmsLogTestIntegration.getMessages().size());
        for (int i = 0; i < mMessages.length; i++) {
            mCmsLogTestIntegration.removeMessage(mMessages[i].getFolder(), mMessages[i].getUid());
            assertEquals(mMessages.length - (i + 1), mCmsLogTestIntegration.getMessages().size());
        }
    }

    public void testRemoveMessages() {
        for (CmsObject message : mMessages) {
            mCmsLog.addMessage(message);
        }
        assertEquals(1, mCmsLog.removeMessages(mFolder1));
        assertEquals(2, mCmsLog.removeMessages(mFolder2));
        assertEquals(3, mCmsLog.removeMessages(mFolder3));
    }

    public void testPurgeMessages() {
        for (CmsObject message : mMessages) {
            mCmsLog.addMessage(message);
        }
        assertEquals(1, mCmsLog.updateDeleteStatus(mFolder1, DeleteStatus.DELETED));
        assertEquals(1,
                mCmsLog.updateXmsDeleteStatus(mContact2, "messageId1", DeleteStatus.DELETED, null));
        assertEquals(1, mCmsLog.updateDeleteStatus(mFolder3, 1, DeleteStatus.DELETED));
        assertEquals(1, mCmsLog.updateDeleteStatus(mFolder3, 3, DeleteStatus.DELETED));
        assertEquals(4, mCmsLog.purgeDeletedMessages());
    }

    public void testRemoveAllMessages() {
        for (CmsObject message : mMessages) {
            mCmsLog.addMessage(message);
        }
        assertEquals(mMessages.length, mCmsLogTestIntegration.getMessages().size());
        assertEquals(mMessages.length, mCmsLog.removeMessages());
        assertEquals(0, mCmsLogTestIntegration.getMessages().size());
    }

    public void testUpdateRcsDeleteStatus() {
        CmsObject message = mMessages[6];
        mCmsLog.addMessage(message);
        CmsObject msgFromLog = mCmsLog.getMessage(message.getFolder(), message.getUid());
        assertEquals(DeleteStatus.NOT_DELETED, msgFromLog.getDeleteStatus());
        assertEquals(1, mCmsLog.updateRcsDeleteStatus(message.getMessageType(),
                message.getMessageId(), DeleteStatus.DELETED_REPORT_REQUESTED, null));
        msgFromLog = mCmsLog.getMessage(message.getFolder(), msgFromLog.getUid());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, msgFromLog.getDeleteStatus());
    }

    public void testUpdateReadStatus() {
        CmsObject message = mMessages[0];
        mCmsLog.addMessage(message);
        CmsObject msgFromLog = mCmsLog.getMessage(message.getFolder(), message.getUid());
        assertEquals(ReadStatus.UNREAD, msgFromLog.getReadStatus());
        assertEquals(message.getFolder(), msgFromLog.getFolder());
        assertEquals(1,
                mCmsLog.updateReadStatus(message.getFolder(), message.getUid(), ReadStatus.READ));
        message = mCmsLog.getMessage(message.getFolder(), msgFromLog.getUid());
        assertEquals(ReadStatus.READ, message.getReadStatus());
    }

    public void testUpdateXmsReadStatus() {
        CmsObject message = mMessages[0];
        mCmsLog.addMessage(message);
        CmsObject msgFromLog = mCmsLog.getMessage(message.getFolder(), message.getUid());
        assertEquals(ReadStatus.UNREAD, msgFromLog.getReadStatus());
        assertEquals(1, mCmsLog.updateXmsReadStatus(mContact1, message.getMessageId(),
                ReadStatus.READ_REPORT_REQUESTED, null));
        msgFromLog = mCmsLog.getMessage(message.getFolder(), msgFromLog.getUid());
        assertEquals(ReadStatus.READ_REPORT_REQUESTED, msgFromLog.getReadStatus());
    }

    public void testUpdateRcsReadStatus() {
        CmsObject message = mMessages[6];
        mCmsLog.addMessage(message);
        CmsObject msgFromLog = mCmsLog.getMessage(message.getFolder(), message.getUid());
        assertEquals(ReadStatus.UNREAD, msgFromLog.getReadStatus());
        assertEquals(1, mCmsLog.updateRcsReadStatus(MessageType.CHAT_MESSAGE,
                message.getMessageId(), ReadStatus.READ_REPORT_REQUESTED, null));
        msgFromLog = mCmsLog.getMessage(msgFromLog.getFolder(), msgFromLog.getUid());
        assertEquals(ReadStatus.READ_REPORT_REQUESTED, msgFromLog.getReadStatus());
    }

    public void testResetReportedStatus() {
        CmsObject obj0 = new CmsObject("folder0", 0, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                PushStatus.PUSHED, MessageType.CHAT_MESSAGE, "messageId0", null);
        CmsObject obj1 = new CmsObject("folder1", 1, ReadStatus.READ_REPORTED,
                DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.CHAT_MESSAGE,
                "messageId1", null);
        CmsObject obj2 = new CmsObject("folder2", 2, ReadStatus.UNREAD,
                DeleteStatus.DELETED_REPORTED, PushStatus.PUSHED, MessageType.CHAT_MESSAGE,
                "messageId2", null);
        CmsObject obj3 = new CmsObject("folder3", 3, ReadStatus.READ_REPORTED,
                DeleteStatus.DELETED_REPORTED, PushStatus.PUSHED, MessageType.CHAT_MESSAGE,
                "messageId3", null);

        mCmsLog.addMessage(obj0);
        mCmsLog.addMessage(obj1);
        mCmsLog.addMessage(obj2);
        mCmsLog.addMessage(obj3);

        ContentProviderResult[] result = mCmsLog.resetReportedStatus();
        assertEquals((Integer) 2, result[0].count);
        assertEquals((Integer) 2, result[1].count);

        CmsObject message = mCmsLog.getMessage(obj0.getFolder(), obj0.getUid());
        assertEquals(ReadStatus.UNREAD, message.getReadStatus());
        assertEquals(DeleteStatus.NOT_DELETED, message.getDeleteStatus());

        message = mCmsLog.getMessage(obj1.getFolder(), obj1.getUid());
        assertEquals(ReadStatus.READ_REPORT_REQUESTED, message.getReadStatus());
        assertEquals(DeleteStatus.NOT_DELETED, message.getDeleteStatus());

        message = mCmsLog.getMessage(obj2.getFolder(), obj2.getUid());
        assertEquals(ReadStatus.UNREAD, message.getReadStatus());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, message.getDeleteStatus());

        message = mCmsLog.getMessage(obj3.getFolder(), obj3.getUid());
        assertEquals(ReadStatus.READ_REPORT_REQUESTED, message.getReadStatus());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, message.getDeleteStatus());
    }

    public void testUpdateStatusesWhereReported() {
        CmsObject obj0 = new CmsObject("folder0", 0, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                PushStatus.PUSHED, MessageType.CHAT_MESSAGE, "messageId0", null);
        CmsObject obj1 = new CmsObject("folder1", 1, ReadStatus.READ_REPORTED,
                DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.CHAT_MESSAGE,
                "messageId1", null);
        CmsObject obj2 = new CmsObject("folder2", 2, ReadStatus.UNREAD,
                DeleteStatus.DELETED_REPORTED, PushStatus.PUSHED, MessageType.CHAT_MESSAGE,
                "messageId2", null);
        CmsObject obj3 = new CmsObject("folder3", 3, ReadStatus.READ_REPORTED,
                DeleteStatus.DELETED_REPORTED, PushStatus.PUSHED, MessageType.CHAT_MESSAGE,
                "messageId3", null);

        mCmsLog.addMessage(obj0);
        mCmsLog.addMessage(obj1);
        mCmsLog.addMessage(obj2);
        mCmsLog.addMessage(obj3);

        ContentProviderResult[] result = mCmsLog.updateStatusesWhereReported(obj0.getMessageId());
        assertEquals((Integer) 0, result[0].count);
        assertEquals((Integer) 0, result[1].count);

        result = mCmsLog.updateStatusesWhereReported(obj1.getMessageId());
        assertEquals((Integer) 1, result[0].count);
        assertEquals((Integer) 0, result[1].count);

        result = mCmsLog.updateStatusesWhereReported(obj2.getMessageId());
        assertEquals((Integer) 0, result[0].count);
        assertEquals((Integer) 1, result[1].count);

        result = mCmsLog.updateStatusesWhereReported(obj3.getMessageId());
        assertEquals((Integer) 1, result[0].count);
        assertEquals((Integer) 1, result[1].count);

        CmsObject message = mCmsLog.getMessage(obj0.getFolder(), obj0.getUid());
        assertEquals(ReadStatus.UNREAD, message.getReadStatus());
        assertEquals(DeleteStatus.NOT_DELETED, message.getDeleteStatus());

        message = mCmsLog.getMessage(obj1.getFolder(), obj1.getUid());
        assertEquals(ReadStatus.READ, message.getReadStatus());
        assertEquals(DeleteStatus.NOT_DELETED, message.getDeleteStatus());

        message = mCmsLog.getMessage(obj2.getFolder(), obj2.getUid());
        assertEquals(ReadStatus.UNREAD, message.getReadStatus());
        assertEquals(DeleteStatus.DELETED, message.getDeleteStatus());

        message = mCmsLog.getMessage(obj3.getFolder(), obj3.getUid());
        assertEquals(ReadStatus.READ, message.getReadStatus());
        assertEquals(DeleteStatus.DELETED, message.getDeleteStatus());
    }

    public void testGetNativeMessages() {
        for (CmsObject message : mMessages) {
            mCmsLog.addMessage(message);
        }
        Map<Long, CmsObject> cmsObjets = mCmsLog.getNativeMessages(MessageType.SMS);
        assertTrue(cmsObjets.isEmpty());
        cmsObjets = mCmsLog.getNativeMessages(MessageType.CHAT_MESSAGE);
        assertEquals(1, cmsObjets.size());
        Map.Entry<Long, CmsObject> entry = cmsObjets.entrySet().iterator().next();
        assertEquals((Long) 1L, entry.getValue().getNativeProviderId());
    }

    public void testUpdateXmsPushStatus() {
        CmsObject message = new CmsObject(mFolder1, null, ReadStatus.UNREAD,
                DeleteStatus.NOT_DELETED, PushStatus.PUSH_REQUESTED, MessageType.SMS, "messageId1",
                null);
        mCmsLog.addMessage(message);
        assertEquals(1, mCmsLog.updateXmsPushStatus(1, mContact1, message.getMessageId(),
                PushStatus.PUSHED));
        assertNull(mCmsLog.getMmsData(mContact1, message.getMessageId()));
        CmsObject msgFromLog = mCmsLog.getSmsData(mContact1, message.getMessageId());
        assertEquals(PushStatus.PUSHED, msgFromLog.getPushStatus());
    }

    public void testUpdateSmsMessageId() {
        CmsObject message = mMessages[0];
        mCmsLog.addMessage(message);
        assertTrue(mCmsLog.updateSmsMessageId(mContact1, message.getMessageId(), "new-message-id"));
        CmsObject msgFromLog = mCmsLog.getSmsData(mContact1, "new-message-id");
        assertEquals("new-message-id", msgFromLog.getMessageId());
        assertEquals(message.getUid(), msgFromLog.getUid());
    }
}

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

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        mCmsLog = CmsLog.getInstance(context);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(context);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(context));
        ContactId contact1 = contactUtils.formatContact("+330000000001");
        ContactId contact2 = contactUtils.formatContact("+330000000002");
        ContactId contact3 = contactUtils.formatContact("+330000000003");
        String chatId4 = "chatId4";
        mFolders = new CmsFolder[] {
                new CmsFolder(com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(contact1), 1,
                        123, 1),
                new CmsFolder(com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(contact2), 1,
                        1234, 1),
                new CmsFolder(com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(contact3), 1,
                        12345, 1),
                new CmsFolder(com.gsma.rcs.core.cms.utils.CmsUtils.groupChatToCmsFolder(chatId4,
                        chatId4), 1, 123456, 1)
        };
        mMessages = new CmsObject[] {
                new CmsObject(mFolders[0].getName(), 1, ReadStatus.UNREAD,
                        DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS, "messageId1",
                        null),
                new CmsObject(mFolders[1].getName(), 1, ReadStatus.UNREAD,
                        DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS, "messageId1",
                        null),
                new CmsObject(mFolders[1].getName(), 2, ReadStatus.UNREAD,
                        DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS, "messageId2",
                        null),
                new CmsObject(mFolders[2].getName(), 1, ReadStatus.UNREAD,
                        DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS, "messageId1",
                        null),
                new CmsObject(mFolders[2].getName(), 2, ReadStatus.UNREAD,
                        DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS, "messageId2",
                        null),
                new CmsObject(mFolders[2].getName(), 3, ReadStatus.UNREAD,
                        DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS, "messageId3",
                        null)
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
        CmsFolder folder;
        mCmsLog.addFolder(mFolders[0]);
        folder = mCmsLogTestIntegration.getFolder(mFolders[0].getName());
        assertEquals(mFolders[0], folder);

        folder = mCmsLogTestIntegration.getFolder("dummy");
        assertNull(folder);
    }

    public void testAddMessage() {
        CmsObject message;
        assertEquals(Integer.valueOf(0), mCmsLog.getMaxUidForMessages(mMessages[0].getFolder()));
        mCmsLog.addMessage(mMessages[0]);
        assertEquals(Integer.valueOf(1), mCmsLog.getMaxUidForMessages(mMessages[0].getFolder()));
        message = mCmsLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
        assertEquals(mMessages[0], message);

        message = mCmsLog.getMessage("dummy", 0);
        assertNull(message);
    }

    public void testGetFolders() {
        Map<String, CmsFolder> folders = mCmsLog.getFolders();
        assertEquals(0, folders.size());

        mCmsLog.addFolder(mFolders[0]);
        folders = mCmsLog.getFolders();
        assertEquals(1, folders.size());
        assertTrue(folders.containsKey(mFolders[0].getName()));

        mCmsLog.addFolder(mFolders[0]);
        folders = mCmsLog.getFolders();
        assertEquals(1, folders.size());
        assertTrue(folders.containsKey(mFolders[0].getName()));

        mCmsLog.addFolder(mFolders[1]);
        folders = mCmsLog.getFolders();
        assertEquals(2, folders.size());
        assertTrue(folders.containsKey(mFolders[0].getName()));
        assertTrue(folders.containsKey(mFolders[1].getName()));

        mCmsLog.addFolder(mFolders[2]);
        folders = mCmsLog.getFolders();
        assertEquals(3, folders.size());
        assertTrue(folders.containsKey(mFolders[0].getName()));
        assertTrue(folders.containsKey(mFolders[1].getName()));
        assertTrue(folders.containsKey(mFolders[2].getName()));
    }

    public void testUpdateFolderSingle() {
        CmsFolder folder;
        mCmsLog.addFolder(mFolders[0]);
        assertTrue(mCmsLog.isFolderPersisted(mFolders[0].getName()));

        folder = mCmsLogTestIntegration.getFolder(mFolders[0].getName());
        assertEquals(mFolders[0], folder);

        mCmsLog.updateFolder(new CmsFolder(mFolders[0].getName(), 0, 0, 0));

        folder = mCmsLogTestIntegration.getFolder(mFolders[0].getName());
        assertEquals(1, mCmsLog.getFolders().size());
        assertEquals(Integer.valueOf(0), folder.getModseq());
        assertEquals(Integer.valueOf(0), folder.getNextUid());
        assertEquals(Integer.valueOf(0), folder.getUidValidity());
    }

    public void testUpdateFolderGroup() {
        CmsFolder folder;
        CmsFolder groupFolder = mFolders[3];
        String folderName = groupFolder.getName();
        mCmsLog.addFolder(groupFolder);
        assertTrue(mCmsLog.isFolderPersisted(folderName));

        folder = mCmsLogTestIntegration.getFolder(folderName);
        assertEquals(groupFolder, folder);

        mCmsLog.updateFolder(new CmsFolder(folderName, 0, 0, 0));

        folder = mCmsLogTestIntegration.getFolder(folderName);
        assertEquals(1, mCmsLog.getFolders().size());
        assertEquals(Integer.valueOf(0), folder.getModseq());
        assertEquals(Integer.valueOf(0), folder.getNextUid());
        assertEquals(Integer.valueOf(0), folder.getUidValidity());
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
        messages = mCmsLogTestIntegration.getMessages(mFolders[0].getName());
        assertEquals(1, messages.size());
        assertEquals(mMessages[0], messages.get(mMessages[0].getUid()));

        messages = mCmsLogTestIntegration.getMessages(mFolders[1].getName());
        assertEquals(2, messages.size());
        assertEquals(mMessages[1], messages.get(mMessages[1].getUid()));
        assertEquals(mMessages[2], messages.get(mMessages[2].getUid()));

        messages = mCmsLogTestIntegration.getMessages(mFolders[2].getName());
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
        for (CmsObject mMessage : mMessages) {
            assertEquals(mMessage.getUid(),
                    mCmsLogTestIntegration.getUidForXmsMessage(mMessage.getMessageId()));
        }
    }

    public void testUpdateMessage() {
        CmsObject message;
        mCmsLog.addMessage(mMessages[0]);

        message = mCmsLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
        assertEquals(mMessages[0], message);

        mCmsLog.addMessage(new CmsObject(mMessages[0].getFolder(), 1, ReadStatus.READ,
                DeleteStatus.DELETED, PushStatus.PUSHED, MessageType.MMS, "messageId1", null));

        message = mCmsLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
        assertEquals(1, mCmsLogTestIntegration.getMessages().size());
        assertEquals(DeleteStatus.DELETED, message.getDeleteStatus());
        assertEquals(ReadStatus.READ, message.getReadStatus());
        assertEquals(MessageType.MMS, message.getMessageType());
    }

    public void testRemoveMessages() {
        for (CmsObject message : mMessages) {
            mCmsLog.addMessage(message);
        }
        assertEquals(mMessages.length, mCmsLogTestIntegration.getMessages().size());
        for (int i = 0; i < mMessages.length; i++) {
            mCmsLogTestIntegration.removeMessage(mMessages[i].getFolder(), mMessages[i].getUid());
            assertEquals(mMessages.length - (i + 1), mCmsLogTestIntegration.getMessages().size());
        }
    }

    public void testRemoveAllMessages() {
        for (CmsObject message : mMessages) {
            mCmsLog.addMessage(message);
        }
        assertEquals(mMessages.length, mCmsLogTestIntegration.getMessages().size());
        mCmsLog.removeMessages();
        assertEquals(0, mCmsLogTestIntegration.getMessages().size());
    }

    public void testUpdateDeleteStatus() {
        CmsObject message;
        mCmsLog.addMessage(mMessages[0]);
        message = mCmsLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
        assertEquals(DeleteStatus.NOT_DELETED, message.getDeleteStatus());

        mCmsLog.updateDeleteStatus(mMessages[0].getMessageType(), mMessages[0].getMessageId(),
                DeleteStatus.DELETED_REPORT_REQUESTED);
        message = mCmsLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, message.getDeleteStatus());
    }

    public void testUpdateReadStatus() {
        CmsObject message;
        mCmsLog.addMessage(mMessages[0]);
        message = mCmsLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
        assertEquals(ReadStatus.UNREAD, message.getReadStatus());

        mCmsLog.updateReadStatus(mMessages[0].getMessageType(), mMessages[0].getMessageId(),
                ReadStatus.READ_REPORT_REQUESTED);
        message = mCmsLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
        assertEquals(ReadStatus.READ_REPORT_REQUESTED, message.getReadStatus());
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
        assertEquals(Integer.valueOf(2), result[0].count);
        assertEquals(Integer.valueOf(2), result[1].count);

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
        assertEquals(Integer.valueOf(0), result[0].count);
        assertEquals(Integer.valueOf(0), result[1].count);

        result = mCmsLog.updateStatusesWhereReported(obj1.getMessageId());
        assertEquals(Integer.valueOf(1), result[0].count);
        assertEquals(Integer.valueOf(0), result[1].count);

        result = mCmsLog.updateStatusesWhereReported(obj2.getMessageId());
        assertEquals(Integer.valueOf(0), result[0].count);
        assertEquals(Integer.valueOf(1), result[1].count);

        result = mCmsLog.updateStatusesWhereReported(obj3.getMessageId());
        assertEquals(Integer.valueOf(1), result[0].count);
        assertEquals(Integer.valueOf(1), result[1].count);

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
}

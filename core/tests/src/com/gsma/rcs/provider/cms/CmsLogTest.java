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

import android.content.Context;
import android.test.AndroidTestCase;

import java.util.Map;

public class CmsLogTest extends AndroidTestCase {

    private CmsLog mCmsLog;
    private CmsLogTestIntegration mCmsLogTestIntegration;
    private Context mContext;

    private CmsFolder[] mFolders;
    private CmsObject[] mMessages;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mCmsLog = CmsLog.getInstance(mContext);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(mContext);

        mFolders = new CmsFolder[] {
                new CmsFolder("folder1", 1, 123, 1), new CmsFolder("folder2", 1, 1234, 1),
                new CmsFolder("folder3", 1, 12345, 1)
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
        assertEquals(new Integer(0), mCmsLog.getMaxUidForMessages(mMessages[0].getFolder()));
        mCmsLog.addMessage(mMessages[0]);
        assertEquals(new Integer(1), mCmsLog.getMaxUidForMessages(mMessages[0].getFolder()));
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
        assertTrue(folders.containsKey("folder1"));

        mCmsLog.addFolder(mFolders[0]);
        folders = mCmsLog.getFolders();
        assertEquals(1, folders.size());
        assertTrue(folders.containsKey("folder1"));

        mCmsLog.addFolder(mFolders[1]);
        folders = mCmsLog.getFolders();
        assertEquals(2, folders.size());
        assertTrue(folders.containsKey("folder1"));
        assertTrue(folders.containsKey("folder2"));

        mCmsLog.addFolder(mFolders[2]);
        folders = mCmsLog.getFolders();
        assertEquals(3, folders.size());
        assertTrue(folders.containsKey("folder1"));
        assertTrue(folders.containsKey("folder2"));
        assertTrue(folders.containsKey("folder3"));
    }

    public void testUpdateFolder() {
        CmsFolder folder;
        mCmsLog.addFolder(mFolders[0]);

        folder = mCmsLogTestIntegration.getFolder(mFolders[0].getName());
        assertEquals(mFolders[0], folder);

        mCmsLog.addFolder(new CmsFolder(mFolders[0].getName(), 0, 0, 0));

        folder = mCmsLogTestIntegration.getFolder(mFolders[0].getName());
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

        for (int i = 0; i < mMessages.length; i++) {
            assertEquals(mMessages[i],
                    mCmsLog.getMessage(mMessages[i].getFolder(), mMessages[i].getUid()));
        }

        for (int i = 0; i < mMessages.length; i++) {
            assertEquals(
                    mMessages[i],
                    mCmsLogTestIntegration.getMessage(mMessages[i].getFolder(),
                            mMessages[i].getMessageId()));
        }

        for (int i = 0; i < mMessages.length; i++) {
            assertTrue(mCmsLog.getMessageId(mMessages[i].getFolder(), mMessages[i].getUid()) != CmsLog.INVALID_ID);
        }

        for (int i = 0; i < mMessages.length; i++) {
            assertEquals(mMessages[i].getUid(),
                    mCmsLog.getUidForXmsMessage(mMessages[i].getMessageId()));
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

}

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

package com.gsma.rcs.core.cms.event.framework;

import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class EventReportingFrameworkDocumentTest extends AndroidTestCase {

    private ContactId mContact1;
    private ContactId mContact2;

    protected void setUp() throws Exception {
        super.setUp();
        RcsSettings rcsSettings = RcsSettingsMock.getMockSettings(mContext);
        AndroidFactory.setApplicationContext(mContext, rcsSettings);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mContact1 = contactUtils.formatContact("+33601020304");
        mContact2 = contactUtils.formatContact("+33699999999");
    }

    public void testReportSeenObjectWithUid() throws RcsPermissionDeniedException {
        EventFrameworkDocument sipEventReportingFrameworkDocument;
        List<CmsObject> seenObjects = new ArrayList<>();
        List<CmsObject> deletedObjects = new ArrayList<>();
        Integer uid1 = 1;
        String messageId1 = IdGenerator.generateMessageID();
        String folder = CmsUtils.contactToCmsFolder(mContact1);
        CmsXmsObject cmsObject = new CmsXmsObject(MessageType.SMS, folder, messageId1, uid1,
                PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null);
        seenObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, null);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Seen\">" + "<object uid=\"" + uid1
                + "\" folder-path=\"Default/tel:" + mContact1 + "/\">" + "<message-id>"
                + messageId1 + "</message-id>" + "</object>" + "</operation>" + "</event-cpm-imap>"
                + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
        String messageId2 = IdGenerator.generateMessageID();
        Integer uid2 = 2;
        folder = CmsUtils.contactToCmsFolder(mContact2);
        cmsObject = new CmsXmsObject(MessageType.SMS, folder, messageId2, uid2, PushStatus.PUSHED,
                ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null);
        seenObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, null);
        expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Seen\">" + "<object uid=\"" + uid1
                + "\" folder-path=\"Default/tel:" + mContact1 + "/\">" + "<message-id>"
                + messageId1 + "</message-id>" + "</object>" + "<object uid=\"" + uid2
                + "\" folder-path=\"Default/tel:" + mContact2 + "/\">" + "<message-id>"
                + messageId2 + "</message-id>" + "</object>" + "</operation>" + "</event-cpm-imap>"
                + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
    }

    public void testReportDeletedObjectWithUid() {
        EventFrameworkDocument sipEventReportingFrameworkDocument;
        List<CmsObject> seenObjects = new ArrayList<>();
        List<CmsObject> deletedObjects = new ArrayList<>();
        Integer uid1 = 1;
        String messageId1 = IdGenerator.generateMessageID();
        String folder = CmsUtils.contactToCmsFolder(mContact1);
        CmsXmsObject cmsObject = new CmsXmsObject(MessageType.SMS, folder, messageId1, uid1,
                PushStatus.PUSHED, ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null);
        deletedObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, null);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Deleted\">" + "<object uid=\"" + uid1
                + "\" folder-path=\"Default/tel:" + mContact1 + "/\">" + "<message-id>"
                + messageId1 + "</message-id>" + "</object>" + "</operation>" + "</event-cpm-imap>"
                + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
        String messageId2 = IdGenerator.generateMessageID();
        Integer uid2 = 2;
        folder = CmsUtils.contactToCmsFolder(mContact2);
        cmsObject = new CmsXmsObject(MessageType.SMS, folder, messageId2, uid2, PushStatus.PUSHED,
                ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null);
        deletedObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, null);
        expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Deleted\">" + "<object uid=\"" + uid1
                + "\" folder-path=\"Default/tel:" + mContact1 + "/\">" + "<message-id>"
                + messageId1 + "</message-id>" + "</object>" + "<object uid=\"" + uid2
                + "\" folder-path=\"Default/tel:" + mContact2 + "/\">" + "<message-id>"
                + messageId2 + "</message-id>" + "</object>" + "</operation>" + "</event-cpm-imap>"
                + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
    }

    public void testReportSeenAndDeletedObjectWithUid() {
        EventFrameworkDocument sipEventReportingFrameworkDocument;
        List<CmsObject> seenObjects = new ArrayList<>();
        List<CmsObject> deletedObjects = new ArrayList<>();
        Integer uid1 = 1;
        String messageId1 = IdGenerator.generateMessageID();
        String folder = CmsUtils.contactToCmsFolder(mContact1);
        CmsXmsObject cmsObject = new CmsXmsObject(MessageType.SMS, folder, messageId1, uid1,
                PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null);
        seenObjects.add(cmsObject);
        String messageId2 = IdGenerator.generateMessageID();
        Integer uid2 = 2;
        folder = CmsUtils.contactToCmsFolder(mContact2);
        cmsObject = new CmsXmsObject(MessageType.SMS, folder, messageId2, uid2, PushStatus.PUSHED,
                ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null);
        deletedObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, null);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Seen\">" + "<object uid=\""
                + uid1
                + "\" folder-path=\"Default/tel:"
                + mContact1
                + "/\">"
                + "<message-id>"
                + messageId1
                + "</message-id>"
                + "</object>"
                + "</operation>"
                + "<operation name=\"add\" flag=\"\\Deleted\">"
                + "<object uid=\""
                + uid2
                + "\" folder-path=\"Default/tel:"
                + mContact2
                + "/\">"
                + "<message-id>"
                + messageId2
                + "</message-id>"
                + "</object>"
                + "</operation>"
                + "</event-cpm-imap>"
                + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
    }

    public void testReportSeenGroupChatMessageWithoutUid() {
        EventFrameworkDocument sipEventReportingFrameworkDocument;
        List<CmsObject> seenObjects = new ArrayList<>();
        List<CmsObject> deletedObjects = new ArrayList<>();
        String chatId1 = IdGenerator.generateMessageID();
        String messageId1 = IdGenerator.generateMessageID();
        String folder = CmsUtils.groupChatToCmsFolder(chatId1, chatId1);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, messageId1,
                PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED,
                chatId1);
        seenObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, chatId1);
        String expectedXml;
        expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Seen\">" + "<object>" + "<conversation-id>"
                + chatId1 + "</conversation-id>" + "<contribution-id>" + chatId1
                + "</contribution-id>" + "<message-id>" + messageId1 + "</message-id>"
                + "</object>" + "</operation>" + "</event-cpm-imap>" + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
        String messageId2 = IdGenerator.generateMessageID();
        cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, messageId2,
                PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED,
                chatId1);
        seenObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, chatId1);
        expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Seen\">" + "<object>" + "<conversation-id>"
                + chatId1
                + "</conversation-id>"
                + "<contribution-id>"
                + chatId1
                + "</contribution-id>"
                + "<message-id>"
                + messageId1
                + "</message-id>"
                + "</object>"
                + "<object>"
                + "<conversation-id>"
                + chatId1
                + "</conversation-id>"
                + "<contribution-id>"
                + chatId1
                + "</contribution-id>"
                + "<message-id>"
                + messageId2
                + "</message-id>"
                + "</object>"
                + "</operation>"
                + "</event-cpm-imap>"
                + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
    }

    public void testReportDeletedGroupChatMessageWithoutUid() {
        EventFrameworkDocument sipEventReportingFrameworkDocument;
        List<CmsObject> seenObjects = new ArrayList<>();
        List<CmsObject> deletedObjects = new ArrayList<>();
        String chatId1 = IdGenerator.generateMessageID();
        String messageId1 = IdGenerator.generateMessageID();
        String folder = CmsUtils.groupChatToCmsFolder(chatId1, chatId1);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, messageId1,
                PushStatus.PUSHED, ReadStatus.UNREAD, DeleteStatus.DELETED_REPORT_REQUESTED,
                chatId1);
        deletedObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, chatId1);
        String expectedXml;
        expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Deleted\">" + "<object>" + "<conversation-id>"
                + chatId1 + "</conversation-id>" + "<contribution-id>" + chatId1
                + "</contribution-id>" + "<message-id>" + messageId1 + "</message-id>"
                + "</object>" + "</operation>" + "</event-cpm-imap>" + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
        String messageId2 = IdGenerator.generateMessageID();
        cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, messageId2,
                PushStatus.PUSHED, ReadStatus.UNREAD, DeleteStatus.DELETED_REPORT_REQUESTED,
                chatId1);
        deletedObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, chatId1);
        expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Deleted\">" + "<object>" + "<conversation-id>"
                + chatId1
                + "</conversation-id>"
                + "<contribution-id>"
                + chatId1
                + "</contribution-id>"
                + "<message-id>"
                + messageId1
                + "</message-id>"
                + "</object>"
                + "<object>"
                + "<conversation-id>"
                + chatId1
                + "</conversation-id>"
                + "<contribution-id>"
                + chatId1
                + "</contribution-id>"
                + "<message-id>"
                + messageId2
                + "</message-id>"
                + "</object>"
                + "</operation>"
                + "</event-cpm-imap>"
                + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
    }

    public void testReportSeenAndDeletedGroupChatMessageWithoutUid() {
        EventFrameworkDocument sipEventReportingFrameworkDocument;
        List<CmsObject> seenObjects = new ArrayList<>();
        List<CmsObject> deletedObjects = new ArrayList<>();
        String chatId1 = IdGenerator.generateMessageID();
        String messageId1 = IdGenerator.generateMessageID();
        String folder = CmsUtils.groupChatToCmsFolder(chatId1, chatId1);
        CmsRcsObject cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, messageId1,
                PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED,
                chatId1);
        seenObjects.add(cmsObject);
        String messageId2 = IdGenerator.generateMessageID();
        cmsObject = new CmsRcsObject(MessageType.CHAT_MESSAGE, folder, messageId2,
                PushStatus.PUSHED, ReadStatus.UNREAD, DeleteStatus.DELETED_REPORT_REQUESTED,
                chatId1);
        deletedObjects.add(cmsObject);
        sipEventReportingFrameworkDocument = new EventFrameworkDocument(seenObjects,
                deletedObjects, chatId1);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<event-cpm-imap type=\"flags\" index=\"0\">"
                + "<operation name=\"add\" flag=\"\\Seen\">" + "<object>" + "<conversation-id>"
                + chatId1
                + "</conversation-id>"
                + "<contribution-id>"
                + chatId1
                + "</contribution-id>"
                + "<message-id>"
                + messageId1
                + "</message-id>"
                + "</object>"
                + "</operation>"
                + "<operation name=\"add\" flag=\"\\Deleted\">"
                + "<object>"
                + "<conversation-id>"
                + chatId1
                + "</conversation-id>"
                + "<contribution-id>"
                + chatId1
                + "</contribution-id>"
                + "<message-id>"
                + messageId2
                + "</message-id>"
                + "</object>"
                + "</operation>"
                + "</event-cpm-imap>"
                + "</cpm-evfw>";
        Assert.assertEquals(expectedXml, sipEventReportingFrameworkDocument.toXml());
    }

    // TODO FGI : unit test for 1-1 message
}

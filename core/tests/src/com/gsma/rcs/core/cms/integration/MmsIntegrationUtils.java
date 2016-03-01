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

import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.core.cms.sync.process.FlagChange;
import com.gsma.rcs.core.cms.sync.process.FlagChange.Operation;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

import com.gsma.rcs.imaplib.imap.Flag;

import java.util.ArrayList;
import java.util.List;

public class MmsIntegrationUtils {

    public static class Test1 {

        public static ContactId contact = ContactUtil
                .createContactIdFromTrustedData("+33640332858");
        public static String folderName = "Default/tel:+33640332858";
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation = new MmsDataObject[] {
                new MmsDataObject("mmsId1", "messageId1", contact, "Hello!", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() - 4000, 1L, 1L, parts),
                new MmsDataObject("mmsId2", "messageId2", contact, "How are you?",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() - 3000, 2L, 1L,
                        parts),
                new MmsDataObject("mmsId3", "messageId3", contact, "Fine, and you?",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() - 2000, 3L, 1L,
                        parts),
                new MmsDataObject("mmsId4", "messageId4", contact, "Fine, thanks",
                        Direction.INCOMING, readStatus, System.currentTimeMillis(), 4L, 1L, parts),
        };
    }

    public static class Test2 {

        public static CmsObject[] cmsObjectReadRequested = new CmsObject[] {
                new CmsObject(Test1.folderName, 1, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.MMS, "1", null),
                new CmsObject(Test1.folderName, 2, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.MMS, "2", null),
                new CmsObject(Test1.folderName, 3, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.MMS, "3", null),
                new CmsObject(Test1.folderName, 4, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.MMS, "4", null),
        };

        public static CmsObject[] cmsObjectDeletedRequested = new CmsObject[] {
                new CmsObject(Test1.folderName, 1, ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, PushStatus.PUSHED, MessageType.MMS, "1", null),
                new CmsObject(Test1.folderName, 2, ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, PushStatus.PUSHED, MessageType.MMS, "2", null),
                new CmsObject(Test1.folderName, 3, ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, PushStatus.PUSHED, MessageType.MMS, "3", null),
                new CmsObject(Test1.folderName, 4, ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, PushStatus.PUSHED, MessageType.MMS, "4", null),
        };
    }

    public static class Test5 {

        public static ContactId contactId = ContactUtil
                .createContactIdFromTrustedData("+33640332858");
        public static CmsObject[] cmsObjectDeletedRequested = new CmsObject[] {
                new CmsObject(Test1.folderName, 1, ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, PushStatus.PUSHED, MessageType.MMS, "1", null),
                new CmsObject(Test1.folderName, 2, ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, PushStatus.PUSHED, MessageType.MMS, "2", null),
        };

        public static List<MmsPart> parts = new ArrayList<>();
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.READ;

        public static MmsDataObject[] conversation = new MmsDataObject[] {
                new MmsDataObject("mmsId1", "messageId1", contactId, "Hello!", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() - 4000, 1L, 1L, parts),
                new MmsDataObject("mmsId2", "messageId2", contactId, "How are you?",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() - 3000, 2L, 1L,
                        parts),
                new MmsDataObject("mmsId3", "messageId3", contactId, "Fine, and you?",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() - 2000, 3L, 1L,
                        parts),
                new MmsDataObject("mmsId4", "messageId4", contactId, "Fine, thanks",
                        Direction.INCOMING, readStatus, System.currentTimeMillis(), 4L, 1L, parts),
        };
    }

    public static class Test7 {

        public static ContactId contactId = ContactUtil
                .createContactIdFromTrustedData("+33640332858");

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static ReadStatus imapReadStatus = ReadStatus.UNREAD;
        public static DeleteStatus imapDeleteStatus = DeleteStatus.NOT_DELETED;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation = new MmsDataObject[] {
                new MmsDataObject("mmsId1", "messageId1", contactId, "Hello!", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() - 4000, 1L, 1L, parts),
                new MmsDataObject("mmsId2", "messageId2", contactId, "yes", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() - 3000, 2L, 1L, parts),
                new MmsDataObject("mmsId3", "messageId3", contactId, "yes", Direction.OUTGOING,
                        readStatus, System.currentTimeMillis() - 2000, 3L, 1L, parts),
                new MmsDataObject("mmsId4", "messageId4", contactId, "yes", Direction.INCOMING,
                        readStatus, System.currentTimeMillis(), 4L, 1L, parts),
        };
    }

    public static class Test8 {

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static ReadStatus imapReadStatus = ReadStatus.UNREAD;
        public static DeleteStatus imapDeleteStatus = DeleteStatus.NOT_DELETED;

        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_remote = new MmsDataObject[] {
                new MmsDataObject("mmsId2", "messageId2", Test1.contact, "Hello!",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 2000, 2L, 1L,
                        parts),
                new MmsDataObject("mmsId1", "messageId1", Test1.contact, "Hello!",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 1000, 1L, 1L,
                        parts),
        };

        public static MmsDataObject[] conversation_local = new MmsDataObject[] {
                new MmsDataObject("mmsId1", "messageId1", Test1.contact, "Hello!",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 1000, 1L, 1L,
                        parts),
                new MmsDataObject("mmsId2", "messageId2", Test1.contact, "Hello!",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 2000, 2L, 1L,
                        parts),
                new MmsDataObject("mmsId3", "messageId3", Test1.contact, "Hello!",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 3000, 3L, 1L,
                        parts),

        };
    }

    public static class Test9 {

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static ReadStatus imapReadStatus = ReadStatus.UNREAD;
        public static DeleteStatus imapDeleteStatus = DeleteStatus.NOT_DELETED;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_remote = new MmsDataObject[] {
                new MmsDataObject("mmsId3", "messageId3", Test1.contact, "Hello",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 3000, 2L, 1L,
                        parts),
                new MmsDataObject("mmsId1", "messageId1", Test1.contact, "Hello!",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 1000, 1L, 1L,
                        parts),
        };

        public static MmsDataObject[] conversation_local = new MmsDataObject[] {
                new MmsDataObject("mmsId1", "messageId1", Test1.contact, "Hello!",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 1000, 1L, 1L,
                        parts),
                new MmsDataObject("mmsId2", "messageId2", Test1.contact, "Hello!",
                        Direction.INCOMING, readStatus, System.currentTimeMillis() + 2000, 2L, 1L,
                        parts),
        };

    }

    public static class Test10 {

        public static ContactId contact1 = ContactUtil
                .createContactIdFromTrustedData("+33600000001");
        public static ContactId contact2 = ContactUtil
                .createContactIdFromTrustedData("+33600000002");
        public static ContactId contact3 = ContactUtil
                .createContactIdFromTrustedData("+33600000003");

        public static String folder1 = "Default/tel:+33600000001";
        public static String folder2 = "Default/tel:+33600000002";
        public static String folder3 = "Default/tel:+33600000003";

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_1 = new MmsDataObject[] {
                new MmsDataObject("mmsId1", "messageId1", contact1, "Hello 1!", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() + 1000, 2L, 1L, parts),
                new MmsDataObject("mmsId2", "messageId2", contact1, "Hi 1", Direction.INCOMING,
                        RcsService.ReadStatus.READ, System.currentTimeMillis() + 2000, 2L, 1L,
                        parts),
                new MmsDataObject("mmsId3", "messageId3", contact1, "Ciao 1", Direction.OUTGOING,
                        RcsService.ReadStatus.READ, System.currentTimeMillis() + 2000, 2L, 1L,
                        parts),
                new MmsDataObject("mmsId4", "messageId4", contact1, "Bye 1", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() + 2000, 2L, 1L, parts),
        };

        public static MmsDataObject[] conversation_2 = new MmsDataObject[] {
                new MmsDataObject("mmsId5", "messageId5", contact2, "Hi 2!", Direction.OUTGOING,
                        RcsService.ReadStatus.READ, System.currentTimeMillis() + 2000, 2L, 1L,
                        parts),
                new MmsDataObject("mmsId6", "messageId6", contact2, "Ciao 2", Direction.OUTGOING,
                        RcsService.ReadStatus.READ, System.currentTimeMillis() + 2000, 2L, 1L,
                        parts),
        };

        public static MmsDataObject[] conversation_3 = new MmsDataObject[] {
                new MmsDataObject("mmsId7", "messageId7", contact3, "Hello 3!", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() + 1000, 1L, 1L, parts),
                new MmsDataObject("mmsId8", "messageId8", contact3, "Bye 3", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() + 2000, 2L, 1L, parts),
        };
    }

    public static class TestLoad {

        public static int iteration = 3;
        public static ContactId contact1 = ContactUtil
                .createContactIdFromTrustedData("+33600000001");
        public static ContactId contact2 = ContactUtil
                .createContactIdFromTrustedData("+33600000002");
        public static ContactId contact3 = ContactUtil
                .createContactIdFromTrustedData("+33600000003");
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_1 = new MmsDataObject[] {
            new MmsDataObject("mmsId1", "messageId1", contact1, "Hello !", Direction.INCOMING,
                    readStatus, System.currentTimeMillis() + 1000, 1L, 1L, parts),
        };

        public static MmsDataObject[] conversation_2 = new MmsDataObject[] {
                new MmsDataObject("mmsId2", "messageId2", contact2, "Hi", Direction.INCOMING,
                        RcsService.ReadStatus.READ, System.currentTimeMillis() + 1000, 1L, 2L,
                        parts),
                new MmsDataObject("mmsId3", "messageId3", contact2, "Ciao", Direction.INCOMING,
                        RcsService.ReadStatus.READ, System.currentTimeMillis() + 1000, 2L, 2L,
                        parts),
        };

        public static MmsDataObject[] conversation_3 = new MmsDataObject[] {
                new MmsDataObject("mmsId4", "messageId4", contact3, "Hello", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() + 1000, 1L, 3L, parts),
                new MmsDataObject("mmsId5", "messageId5", contact3, "Bye", Direction.INCOMING,
                        readStatus, System.currentTimeMillis() + 1000, 2L, 3L, parts),
        };
    }
}

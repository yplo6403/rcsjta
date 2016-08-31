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

import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;
import java.util.List;

public class MmsIntegrationUtils {

    public static class Test1 {

        public static ContactId contact = ContactUtil
                .createContactIdFromTrustedData("+33640332858");
        public static String folderName = CmsUtils.contactToCmsFolder(contact);
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation = new MmsDataObject[] {
                new MmsDataObject("messageId1", contact, "Hello!", Direction.INCOMING, readStatus,
                        NtpTrustedTime.currentTimeMillis() - 4000, 1L, parts),
                new MmsDataObject("messageId2", contact, "How are you?", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() - 3000, 2L, parts),
                new MmsDataObject("messageId3", contact, "Fine, and you?", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() - 2000, 3L, parts),
                new MmsDataObject("messageId4", contact, "Fine, thanks", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis(), 4L, parts),
        };
    }

    public static class Test2 {

        public static CmsXmsObject[] cmsObjectReadRequested = new CmsXmsObject[] {
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "1", 1, PushStatus.PUSHED,
                        ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null),
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "2", 2, PushStatus.PUSHED,
                        ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null),
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "3", 3, PushStatus.PUSHED,
                        ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null),
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "4", 4, PushStatus.PUSHED,
                        ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null),
        };

        public static CmsXmsObject[] cmsObjectDeletedRequested = new CmsXmsObject[] {
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "1", 1, PushStatus.PUSHED,
                        ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null),
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "2", 2, PushStatus.PUSHED,
                        ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null),
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "3", 3, PushStatus.PUSHED,
                        ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null),
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "4", 4, PushStatus.PUSHED,
                        ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null),
        };
    }

    public static class Test5 {

        public static ContactId contactId = ContactUtil
                .createContactIdFromTrustedData("+33640332858");
        public static CmsXmsObject[] cmsObjectDeletedRequested = new CmsXmsObject[] {
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "1", 1, PushStatus.PUSHED,
                        ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null),
                new CmsXmsObject(MessageType.MMS, Test1.folderName, "2", 2, PushStatus.PUSHED,
                        ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null),
        };

        public static List<MmsPart> parts = new ArrayList<>();
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.READ;

        public static MmsDataObject[] conversation = new MmsDataObject[] {
                new MmsDataObject("messageId1", contactId, "Hello!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() - 4000, 1L, parts),
                new MmsDataObject("messageId2", contactId, "How are you?", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() - 3000, 2L, parts),
                new MmsDataObject("messageId3", contactId, "Fine, and you?", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() - 2000, 3L, parts),
                new MmsDataObject("messageId4", contactId, "Fine, thanks", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis(), 4L, parts),
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
                new MmsDataObject("messageId1", contactId, "Hello!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() - 4000, 1L, parts),
                new MmsDataObject("messageId2", contactId, "yes", Direction.INCOMING, readStatus,
                        NtpTrustedTime.currentTimeMillis() - 3000, 2L, parts),
                new MmsDataObject("messageId3", contactId, "yes", Direction.OUTGOING, readStatus,
                        NtpTrustedTime.currentTimeMillis() - 2000, 3L, parts),
                new MmsDataObject("messageId4", contactId, "yes", Direction.INCOMING, readStatus,
                        NtpTrustedTime.currentTimeMillis(), 4L, parts),
        };
    }

    public static class Test8 {

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static ReadStatus imapReadStatus = ReadStatus.UNREAD;
        public static DeleteStatus imapDeleteStatus = DeleteStatus.NOT_DELETED;

        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_remote = new MmsDataObject[] {
                new MmsDataObject("messageId2", Test1.contact, "Hello!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 2000, 2L, parts),
                new MmsDataObject("messageId1", Test1.contact, "Hello!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 1000, 1L, parts),
        };

        public static MmsDataObject[] conversation_local = new MmsDataObject[] {
                new MmsDataObject("messageId1", Test1.contact, "Hello!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 1000, 1L, parts),
                new MmsDataObject("messageId2", Test1.contact, "Hello!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 2000, 2L, parts),
                new MmsDataObject("messageId3", Test1.contact, "Salut!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 3000, 3L, parts),

        };
    }

    public static class Test9 {

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static ReadStatus imapReadStatus = ReadStatus.UNREAD;
        public static DeleteStatus imapDeleteStatus = DeleteStatus.NOT_DELETED;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_remote = new MmsDataObject[] {
                new MmsDataObject("messageId3", Test1.contact, "Salut", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 3000, 2L, parts),
                new MmsDataObject("messageId1", Test1.contact, "Hello!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 1000, 1L, parts),
        };

        public static MmsDataObject[] conversation_local = new MmsDataObject[] {
                new MmsDataObject("messageId1", Test1.contact, "Hello!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 1000, 1L, parts),
                new MmsDataObject("messageId2", Test1.contact, "Hallo!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 2000, 2L, parts),
        };

    }

    public static class Test10 {

        public static ContactId contact1 = ContactUtil
                .createContactIdFromTrustedData("+33600000001");
        public static ContactId contact2 = ContactUtil
                .createContactIdFromTrustedData("+33600000002");
        public static ContactId contact3 = ContactUtil
                .createContactIdFromTrustedData("+33600000003");

        public static String folder1 = CmsUtils.contactToCmsFolder(contact1);
        public static String folder2 = CmsUtils.contactToCmsFolder(contact2);
        public static String folder3 = CmsUtils.contactToCmsFolder(contact3);

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_1 = new MmsDataObject[] {
                new MmsDataObject("messageId1", contact1, "Hello 1!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 1000, 2L, parts),
                new MmsDataObject("messageId2", contact1, "Hi 1", Direction.INCOMING,
                        RcsService.ReadStatus.READ, NtpTrustedTime.currentTimeMillis() + 2000, 2L,
                        parts),
                new MmsDataObject("messageId3", contact1, "Ciao 1", Direction.OUTGOING,
                        RcsService.ReadStatus.READ, NtpTrustedTime.currentTimeMillis() + 2000, 2L,
                        parts),
                new MmsDataObject("messageId4", contact1, "Bye 1", Direction.INCOMING, readStatus,
                        NtpTrustedTime.currentTimeMillis() + 2000, 2L, parts),
        };

        public static MmsDataObject[] conversation_2 = new MmsDataObject[] {
                new MmsDataObject("messageId5", contact2, "Hi 2!", Direction.OUTGOING,
                        RcsService.ReadStatus.READ, NtpTrustedTime.currentTimeMillis() + 2000, 2L,
                        parts),
                new MmsDataObject("messageId6", contact2, "Ciao 2", Direction.OUTGOING,
                        RcsService.ReadStatus.READ, NtpTrustedTime.currentTimeMillis() + 2000, 2L,
                        parts),
        };

        public static MmsDataObject[] conversation_3 = new MmsDataObject[] {
                new MmsDataObject("messageId7", contact3, "Hello 3!", Direction.INCOMING,
                        readStatus, NtpTrustedTime.currentTimeMillis() + 1000, 1L, parts),
                new MmsDataObject("messageId8", contact3, "Bye 3", Direction.INCOMING, readStatus,
                        NtpTrustedTime.currentTimeMillis() + 2000, 2L, parts),
        };
    }

    public static class Test11 {

        public static ContactId contact1 = ContactUtil
                .createContactIdFromTrustedData("+33600000001");
        public static ContactId contact2 = ContactUtil
                .createContactIdFromTrustedData("+33600000002");
        public static ContactId contact3 = ContactUtil
                .createContactIdFromTrustedData("+33600000003");

        public static String folder1 = CmsUtils.contactToCmsFolder(contact1);
        public static String folder2 = CmsUtils.contactToCmsFolder(contact2);
        public static String folder3 = CmsUtils.contactToCmsFolder(contact3);

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<MmsPart>() {
            {
                add(new MmsDataObject.MmsPart("messageId1", XmsMessageLog.MimeType.TEXT_MESSAGE,
                        "body"));
            }
        };

        public static MmsDataObject[] conversation_1 = new MmsDataObject[] {
            new MmsDataObject("messageId1", contact1, "Hello 1!", Direction.OUTGOING, readStatus,
                    NtpTrustedTime.currentTimeMillis() + 1000, 2L, parts),
        };

        public static MmsDataObject[] conversation_2 = new MmsDataObject[] {
            new MmsDataObject("messageId1", contact2, "Hello 1!", Direction.OUTGOING, readStatus,
                    NtpTrustedTime.currentTimeMillis() + 1000, 2L, parts),
        };

        public static MmsDataObject[] conversation_3 = new MmsDataObject[] {
            new MmsDataObject("messageId1", contact3, "Hello 1!", Direction.OUTGOING, readStatus,
                    NtpTrustedTime.currentTimeMillis() + 1000, 2L, parts),
        };

        public static CmsXmsObject[] cmsObjectDeletedRequested = new CmsXmsObject[] {
            new CmsXmsObject(MessageType.MMS, folder1, "messageId1", 1, PushStatus.PUSHED,
                    ReadStatus.READ, DeleteStatus.DELETED_REPORT_REQUESTED, null)
        };
    }

    public static class TestLoad {

        public static ContactId contact1 = ContactUtil
                .createContactIdFromTrustedData("+33600000001");
        public static ContactId contact2 = ContactUtil
                .createContactIdFromTrustedData("+33600000002");
        public static ContactId contact3 = ContactUtil
                .createContactIdFromTrustedData("+33600000003");
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_1 = new MmsDataObject[] {
            new MmsDataObject("messageId1", contact1, "Hello !", Direction.INCOMING, readStatus,
                    NtpTrustedTime.currentTimeMillis() + 1000, 1L, parts),
        };

        public static MmsDataObject[] conversation_2 = new MmsDataObject[] {
                new MmsDataObject("messageId2", contact2, "Hi", Direction.INCOMING,
                        RcsService.ReadStatus.READ, NtpTrustedTime.currentTimeMillis() + 1000, 1L,
                        parts),
                new MmsDataObject("messageId3", contact2, "Ciao", Direction.INCOMING,
                        RcsService.ReadStatus.READ, NtpTrustedTime.currentTimeMillis() + 1000, 2L,
                        parts),
        };

        public static MmsDataObject[] conversation_3 = new MmsDataObject[] {
                new MmsDataObject("messageId4", contact3, "Hello", Direction.INCOMING, readStatus,
                        NtpTrustedTime.currentTimeMillis() + 1000, 1L, parts),
                new MmsDataObject("messageId5", contact3, "Bye", Direction.INCOMING, readStatus,
                        NtpTrustedTime.currentTimeMillis() + 1000, 2L, parts),
        };
    }
}

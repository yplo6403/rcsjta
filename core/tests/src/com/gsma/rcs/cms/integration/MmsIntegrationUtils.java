package com.gsma.rcs.cms.integration;

import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.cms.sync.strategy.FlagChange.Operation;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.sonymobile.rcs.imap.Flag;

import java.util.ArrayList;
import java.util.List;

public class MmsIntegrationUtils {
    
    public static class Test1{

        public static ContactId contact  = ContactUtil.createContactIdFromTrustedData("+33640332858");
        public static String folderName = "Default/tel:+33640332858";
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1", contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()-4000,1l , 1l, parts),
                new MmsDataObject("mmsId2", "messageId2", contact, "How are you?", Direction.INCOMING,  readStatus, System.currentTimeMillis()-3000,2l , 1l, parts),
                new MmsDataObject("mmsId3", "messageId3", contact, "Fine, and you?", Direction.INCOMING,  readStatus, System.currentTimeMillis()-2000,3l , 1l, parts),
                new MmsDataObject("mmsId4", "messageId4", contact, "Fine, thanks", Direction.INCOMING, readStatus,  System.currentTimeMillis(),4l , 1l, parts),
        };
    }
    
    public static class Test2{

        public static FlagChange[] flagChangesSeen = new FlagChange[]{
                new FlagChange(Test1.folderName,1,Flag.Seen, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,2,Flag.Seen, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,3,Flag.Seen, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,4,Flag.Seen, Operation.ADD_FLAG)
        };
        
        public static FlagChange[] flagChangesDeleted = new FlagChange[]{
                new FlagChange(Test1.folderName,1,Flag.Deleted, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,2,Flag.Deleted, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,3,Flag.Deleted, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,4,Flag.Deleted, Operation.ADD_FLAG)

        };         

    }
    

    public static class Test5{

        public static ContactId contactId  = ContactUtil.createContactIdFromTrustedData("+33640332858");
        public static FlagChange[] flagChangesDeleted = new FlagChange[]{
                new FlagChange(Test1.folderName,1,Flag.Deleted, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,2,Flag.Deleted, Operation.ADD_FLAG),
        };

        public static List<MmsPart> parts = new ArrayList<>();
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.READ;

        public static MmsDataObject[] conversation = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1", contactId, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()-4000,1l , 1l, parts),
                new MmsDataObject("mmsId2", "messageId2", contactId, "How are you?", Direction.INCOMING, readStatus, System.currentTimeMillis()-3000,2l , 1l, parts),
                new MmsDataObject("mmsId3", "messageId3", contactId, "Fine, and you?", Direction.INCOMING, readStatus, System.currentTimeMillis()-2000,3l , 1l, parts),
                new MmsDataObject("mmsId4", "messageId4", contactId, "Fine, thanks", Direction.INCOMING, readStatus, System.currentTimeMillis(),4l , 1l, parts),
        };
    }
    
    public static class Test7{

        public static ContactId contactId  = ContactUtil.createContactIdFromTrustedData("+33640332858");

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static ReadStatus imapReadStatus = ReadStatus.UNREAD;
        public static DeleteStatus imapDeleteStatus = DeleteStatus.NOT_DELETED;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1", contactId, "Hello!", Direction.INCOMING, readStatus,  System.currentTimeMillis()-4000,1l , 1l, parts),
                new MmsDataObject("mmsId2", "messageId2", contactId, "yes", Direction.INCOMING,readStatus,  System.currentTimeMillis()-3000,2l , 1l, parts),
                new MmsDataObject("mmsId3", "messageId3", contactId, "yes", Direction.OUTGOING, readStatus, System.currentTimeMillis()-2000,3l , 1l, parts),
                new MmsDataObject("mmsId4", "messageId4", contactId, "yes", Direction.INCOMING, readStatus, System.currentTimeMillis(),4l , 1l, parts),
        };
    }
    
    public static class Test8{

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static ReadStatus imapReadStatus = ReadStatus.UNREAD;
        public static DeleteStatus imapDeleteStatus = DeleteStatus.NOT_DELETED;

        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_remote = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1", Test1.contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000,1l , 1l, parts),
                new MmsDataObject("mmsId2", "messageId2", Test1.contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()+2000,2l , 1l, parts ),
        };

        public static MmsDataObject[] conversation_local = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1", Test1.contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000,1l , 1l, parts),
                new MmsDataObject("mmsId2", "messageId2", Test1.contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()+2000,2l , 1l, parts),
                new MmsDataObject("mmsId3", "messageId3", Test1.contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()+3000,3l , 1l, parts),

        };
    }
    
    public static class Test9{

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static ReadStatus imapReadStatus = ReadStatus.UNREAD;
        public static DeleteStatus imapDeleteStatus = DeleteStatus.NOT_DELETED;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_remote = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1", Test1.contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000,1l, 1l, parts),
                new MmsDataObject("mmsId3", "messageId3", Test1.contact, "Hello", Direction.INCOMING, readStatus, System.currentTimeMillis()+3000,2l, 1l, parts),
        };

        public static MmsDataObject[] conversation_local = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1", Test1.contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000,1l, 1l, parts),
                new MmsDataObject("mmsId2", "messageId2", Test1.contact, "Hello!", Direction.INCOMING, readStatus, System.currentTimeMillis()+2000,2l, 1l, parts),
        };

    }
    
    public static class Test10 {
        
        public static ContactId contact1 = ContactUtil.createContactIdFromTrustedData("+33600000001");
        public static ContactId contact2 =  ContactUtil.createContactIdFromTrustedData("+33600000002");
        public static ContactId contact3 =  ContactUtil.createContactIdFromTrustedData("+33600000003");
        
        public static String folder1 = "Default/tel:+33600000001";
        public static String folder2 = "Default/tel:+33600000002";
        public static String folder3 = "Default/tel:+33600000003";

        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_1 = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1", contact1, "Hello 1!", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000,2l, 1l, parts),
                new MmsDataObject("mmsId2", "messageId2", contact1, "Hi 1", Direction.INCOMING, RcsService.ReadStatus.READ, System.currentTimeMillis()+2000, 2l, 1l, parts),
                new MmsDataObject("mmsId3", "messageId3", contact1, "Ciao 1", Direction.OUTGOING, RcsService.ReadStatus.READ, System.currentTimeMillis()+2000, 2l, 1l, parts),
                new MmsDataObject("mmsId4", "messageId4", contact1, "Bye 1", Direction.INCOMING, readStatus, System.currentTimeMillis()+2000,2l, 1l, parts),
        };


        public static MmsDataObject[] conversation_2 = new MmsDataObject[]{
                new MmsDataObject("mmsId5", "messageId5",contact2, "Hi 2!", Direction.OUTGOING, RcsService.ReadStatus.READ, System.currentTimeMillis()+2000, 2l ,1L, parts),
                new MmsDataObject("mmsId6", "messageId6",contact2, "Ciao 2", Direction.OUTGOING, RcsService.ReadStatus.READ, System.currentTimeMillis()+2000, 2l ,1l, parts),
        };

        public static MmsDataObject[] conversation_3 = new MmsDataObject[]{
                new MmsDataObject("mmsId7", "messageId7",contact3, "Hello 3!", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000, 1l, 1l, parts),
                new MmsDataObject("mmsId8", "messageId8",contact3, "Bye 3", Direction.INCOMING, readStatus, System.currentTimeMillis()+2000, 2l, 1l, parts),
        };
    }
    
    public static class TestLoad {
        
        public static int iteration = 3;
        public static ContactId contact1 = ContactUtil.createContactIdFromTrustedData("+33600000001");
        public static ContactId contact2 =  ContactUtil.createContactIdFromTrustedData("+33600000002");
        public static ContactId contact3 =  ContactUtil.createContactIdFromTrustedData("+33600000003");
        public static RcsService.ReadStatus readStatus = RcsService.ReadStatus.UNREAD;
        public static List<MmsPart> parts = new ArrayList<>();

        public static MmsDataObject[] conversation_1 = new MmsDataObject[]{
                new MmsDataObject("mmsId1", "messageId1",contact1, "Hello !", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000, 1l, 1l, parts),
        };

        public static MmsDataObject[] conversation_2 = new MmsDataObject[]{
                new MmsDataObject("mmsId2", "messageId2" ,contact2, "Hi", Direction.INCOMING, RcsService.ReadStatus.READ, System.currentTimeMillis()+1000,1l , 2l, parts),
                new MmsDataObject("mmsId3", "messageId3",contact2, "Ciao", Direction.INCOMING, RcsService.ReadStatus.READ, System.currentTimeMillis()+1000,2l , 2l, parts),
        };


        public static MmsDataObject[] conversation_3 = new MmsDataObject[]{
                new MmsDataObject("mmsId4", "messageId4",contact3, "Hello", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000,1l , 3l, parts),
                new MmsDataObject("mmsId5", "messageId5",contact3, "Bye", Direction.INCOMING, readStatus, System.currentTimeMillis()+1000,2l , 3l, parts),
        };
    }
}

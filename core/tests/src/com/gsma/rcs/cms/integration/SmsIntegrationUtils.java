package com.gsma.rcs.cms.integration;

import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.model.XmsData.DeleteStatus;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.cms.sync.strategy.FlagChange.Operation;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.services.rcs.RcsService.Direction;

import com.sonymobile.rcs.imap.Flag;

public class SmsIntegrationUtils {
    
    public static class Test1{
        
        public static String contact = "+33640332858";
        public static String folderName = CmsUtils.contactToCmsFolder(CmsSettings.getInstance(), contact);
        public static ReadStatus readStatus = ReadStatus.UNREAD;
        public static DeleteStatus deleteStatus = DeleteStatus.NOT_DELETED; 
        
        public static SmsData[] conversation = new SmsData[]{
                new SmsData(1l,1l, contact, "Hello!", System.currentTimeMillis()-4000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, contact, "How are you?", System.currentTimeMillis()-3000, Direction.INCOMING, readStatus),
                new SmsData(3l,1l, contact, "Fine, and you?", System.currentTimeMillis()-2000, Direction.OUTGOING, readStatus),
                new SmsData(4l,1l, contact, "Fine, thanks", System.currentTimeMillis(), Direction.INCOMING, readStatus),
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
        
        public static FlagChange[] flagChangesDeleted = new FlagChange[]{
                new FlagChange(Test1.folderName,1,Flag.Deleted, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,2,Flag.Deleted, Operation.ADD_FLAG),
        };         

        public static ReadStatus readStatus = ReadStatus.READ_REQUESTED;
        public static DeleteStatus deleteStatus = DeleteStatus.NOT_DELETED; 
        
        public static SmsData[] conversation = new SmsData[]{
                new SmsData(1l,1l, Test1.contact, "Hello!", System.currentTimeMillis()-4000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, Test1.contact, "How are you?", System.currentTimeMillis()-3000, Direction.INCOMING, readStatus),
                new SmsData(3l,1l, Test1.contact, "Fine, and you?", System.currentTimeMillis()-2000, Direction.OUTGOING, readStatus),
                new SmsData(4l,1l, Test1.contact, "Fine, thanks", System.currentTimeMillis(), Direction.INCOMING, readStatus),
        };       
    }
    
    public static class Test7{
        
        public static FlagChange[] flagChangesDeleted = new FlagChange[]{
                new FlagChange(Test1.folderName,1,Flag.Deleted, Operation.ADD_FLAG),
                new FlagChange(Test1.folderName,2,Flag.Deleted, Operation.ADD_FLAG),
        };         

        public static ReadStatus readStatus = ReadStatus.UNREAD;
        public static DeleteStatus deleteStatus = DeleteStatus.NOT_DELETED; 
        
        public static SmsData[] conversation = new SmsData[]{
                new SmsData(1l,1l, Test1.contact, "Hello!", System.currentTimeMillis()-4000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, Test1.contact, "yes", System.currentTimeMillis()-3000, Direction.INCOMING, readStatus),
                new SmsData(3l,1l, Test1.contact, "yes", System.currentTimeMillis()-2000, Direction.OUTGOING, readStatus),
                new SmsData(4l,1l, Test1.contact, "yes", System.currentTimeMillis(), Direction.INCOMING, readStatus),
        };
    }
    
    public static class Test8{
        
        public static ReadStatus readStatus = ReadStatus.UNREAD;
        
        public static SmsData[] conversation_remote = new SmsData[]{
                new SmsData(1l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+2000, Direction.INCOMING, readStatus),
        };
        
        public static SmsData[] conversation_local = new SmsData[]{
                new SmsData(1l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+2000, Direction.INCOMING, readStatus),
                new SmsData(3l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+3000, Direction.INCOMING, readStatus),
        };        

    }
    
    public static class Test9{
        
        public static ReadStatus readStatus = ReadStatus.UNREAD;
        
        public static SmsData[] conversation_local = new SmsData[]{
                new SmsData(1l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+2000, Direction.INCOMING, readStatus),
        };
        
        public static SmsData[] conversation_remote = new SmsData[]{
                new SmsData(1l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+2000, Direction.INCOMING, readStatus),
                new SmsData(3l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+3000, Direction.INCOMING, readStatus),
        };        

    }
    
    public static class Test10 {
        
        public static String contact1 = "+33600000001";
        public static String contact2 = "+33600000002";
        public static String contact3 = "+33600000003";
        
        public static String folder1 = CmsUtils.contactToCmsFolder(CmsSettings.getInstance(), contact1);
        public static String folder2 = CmsUtils.contactToCmsFolder(CmsSettings.getInstance(), contact2);
        public static String folder3 = CmsUtils.contactToCmsFolder(CmsSettings.getInstance(), contact3);

        public static ReadStatus readStatus = ReadStatus.UNREAD;
        
        public static SmsData[] conversation_1 = new SmsData[]{
                new SmsData(1l,1l, contact1, "Hello 1!", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, contact1, "Hi 1", System.currentTimeMillis()+2000, Direction.OUTGOING, ReadStatus.READ),
                new SmsData(2l,1l, contact1, "Ciao 1", System.currentTimeMillis()+2000, Direction.OUTGOING, ReadStatus.READ),
                new SmsData(2l,1l, contact1, "Bye 1", System.currentTimeMillis()+2000, Direction.INCOMING, readStatus),
        };

        public static SmsData[] conversation_2 = new SmsData[]{
                new SmsData(2l,1l, contact2, "Hi 2", System.currentTimeMillis()+2000, Direction.OUTGOING, ReadStatus.READ),
                new SmsData(2l,1l, contact2, "Ciao 2", System.currentTimeMillis()+2000, Direction.OUTGOING, ReadStatus.READ),
        };

        public static SmsData[] conversation_3 = new SmsData[]{
                new SmsData(1l,1l, contact3, "Hello 3!", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, contact3, "Bye 3", System.currentTimeMillis()+2000, Direction.INCOMING, readStatus),
        };

        public static SmsData[] conversation_remote = new SmsData[]{
                new SmsData(1l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
                new SmsData(2l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+2000, Direction.INCOMING, readStatus),
                new SmsData(3l,1l, Test1.contact, "Hello!", System.currentTimeMillis()+3000, Direction.INCOMING, readStatus),
        };        

    }
    
    public static class TestLoad {
        
        public static int iteration = 3;
        public static String contact1 = "+33600000001";
        public static String contact2 = "+33600000002";
        public static String contact3 = "+33600000003";
        public static ReadStatus readStatus = ReadStatus.UNREAD;
        
        public static SmsData[] conversation_1 = new SmsData[]{
                new SmsData(1l,1l, contact1, "Hello !", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
        };

        public static SmsData[] conversation_2 = new SmsData[]{
                new SmsData(2l,2l, contact2, "Hi ", System.currentTimeMillis()+2000, Direction.OUTGOING, ReadStatus.READ),
                new SmsData(2l,2l, contact2, "Ciao ", System.currentTimeMillis()+2000, Direction.INCOMING, ReadStatus.READ),
        };

        public static SmsData[] conversation_3 = new SmsData[]{
                new SmsData(1l,3l, contact3, "Hello !", System.currentTimeMillis()+1000, Direction.INCOMING, readStatus),
                new SmsData(2l,3l, contact3, "Bye ", System.currentTimeMillis()+2000, Direction.INCOMING, readStatus),
        };
    }
}

package com.gsma.rcs.cms.integration;

import android.content.Context;
import android.test.AndroidTestCase;

import com.gsma.rcs.cms.event.XmsEventListener;
import com.gsma.rcs.cms.fordemo.ImapContext;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.DeleteTask;
import com.gsma.rcs.cms.imap.task.DeleteTask.Operation;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.integration.SmsIntegrationUtils.Test1;
import com.gsma.rcs.cms.integration.SmsIntegrationUtils.Test2;
import com.gsma.rcs.cms.integration.SmsIntegrationUtils.Test7;
import com.gsma.rcs.cms.integration.SmsIntegrationUtils.Test8;
import com.gsma.rcs.cms.integration.SmsIntegrationUtils.Test9;
import com.gsma.rcs.cms.integration.SmsIntegrationUtils.TestLoad;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.XmsLogEnvIntegration;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.BasicSyncStrategy;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.cms.toolkit.operations.Message;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import junit.framework.Assert;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class SmsTest extends AndroidTestCase{

    private final static Logger sLogger = Logger.getLogger(BasicSyncStrategy.class.getSimpleName());

    private XmsLogEnvIntegration mXmsLogEnvIntegration;
    private RcsSettings mSettings;
    private LocalStorage mLocalStorage;
    private BasicImapService mBasicImapService;
    private BasicSyncStrategy mSyncStrategy;
    private ImapLog mImapLog;
    private XmsLog mXmsLog;
    private ImapContext mImapContext;

    
    protected void setUp() throws Exception {
        super.setUp();                        
        Context context = getContext();                
        mSettings = RcsSettingsMock.getRcsSettings(context);
        mImapLog = ImapLog.createInstance(context);
        mXmsLog = XmsLog.createInstance(context.getContentResolver(), new LocalContentResolver(context));
        mXmsLogEnvIntegration = XmsLogEnvIntegration.getInstance(context);
        XmsEventListener smsEventHandler = new XmsEventListener(context, mImapLog, mXmsLog, mSettings);
        mLocalStorage = new LocalStorage(mImapLog);
        mLocalStorage.registerRemoteEventHandler(MessageType.SMS, smsEventHandler);
        mBasicImapService = ImapServiceManager.getService(mSettings);        
        mSyncStrategy = new BasicSyncStrategy(context,mSettings,mBasicImapService, mLocalStorage);
        mBasicImapService.init();
        mImapContext = new ImapContext();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        ImapServiceManager.releaseService(mBasicImapService);
        mLocalStorage.unregisterRemoteEventHandler(MessageType.SMS);
    }
    
     /**
      * Test1
      * step 0 : purge local storage and CMS server folders
      * step 1 : create a conversation on CMS server
      * step 2 : start a sync
      */
    public void test1()  {

        try {
        Map<Integer, MessageData> imapData;
                
        deleteLocalStorage(true, true);
        deleteRemoteStorage();
                
        // create messages on CMS        
        createRemoteMessages(SmsIntegrationUtils.Test1.conversation);
        
        // start synchro
        startSynchro();

        // check that messages are present in local storage
        assertFalse(mImapLog.getFolders().isEmpty());
        int nbFolders = mImapLog.getFolders().size();
        assertTrue(nbFolders>0);

        imapData = mImapLog.getMessages(SmsIntegrationUtils.Test1.folderName);        
        assertEquals(SmsIntegrationUtils.Test1.conversation.length,imapData.size());
        
        List<SmsDataObject> messages = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, messages.size());
        for(SmsDataObject message : messages){
            Assert.assertEquals(Test1.readStatus, message.getReadStatus());
        }
        
        // start synchro : all is up to date
        startSynchro();        

        // check that messages are present in local storage   
        assertFalse(mImapLog.getFolders().isEmpty());
        assertEquals(nbFolders,mImapLog.getFolders().size());
        
        imapData = mImapLog.getMessages(SmsIntegrationUtils.Test1.folderName);        
        assertEquals(SmsIntegrationUtils.Test1.conversation.length,imapData.size());
                
        } catch (ImapServiceNotAvailableException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }
   
    /**
     * Test2 
     * Test 1 +
     * Step 0 : mark conversation as seen on CMS  
     * step 1 : start a sync :  messages are marked as seen in local storage
     * Step 2 : mark conversation as deleted on CMS  
     * step 3 : start a sync :  messages are marked as deleted in local storage
 
     */
   public void test2()  {

       try {
           
           test1();
           
           // update messages with 'seen' flag on CMS 
           updateRemoteFlags(Arrays.asList(Test2.flagChangesSeen));
           
           // sync with CMS
           startSynchro();
           
           //check that messages are marked as 'Seen' in local storage   
           assertFalse(mImapLog.getFolders().isEmpty());
           int nbFolders = mImapLog.getFolders().size();
           assertTrue(nbFolders>0);
           assertEquals(SmsIntegrationUtils.Test1.conversation.length,mImapLog.getMessages(SmsIntegrationUtils.Test1.folderName).size());           
           List<SmsDataObject> messages = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
           assertEquals(SmsIntegrationUtils.Test1.conversation.length, messages.size());
           for(SmsDataObject message : messages){
               Assert.assertEquals(ReadStatus.READ, message.getReadStatus());
           }
           
           // update messages with 'deleted' flag on CMS           
           updateRemoteFlags(Arrays.asList(Test2.flagChangesDeleted));

           // sync with CMS
           startSynchro();
           
           //check that messages are marked as 'Deleted' in local storage   
           assertFalse(mImapLog.getFolders().isEmpty());
           assertEquals(nbFolders,mImapLog.getFolders().size());
           assertEquals(SmsIntegrationUtils.Test1.conversation.length,mImapLog.getMessages(SmsIntegrationUtils.Test1.folderName).size());           
           messages = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
           assertEquals(0, messages.size());                      
        } catch (ImapServiceNotAvailableException e) {
            Assert.fail();
        }
       
   }
   
   /**
    * Test3 
    * Test 1 +
    * Step 0 : mark conversation as read_report_requested in local storage
    * Step 1 : start sync : message are marked as seen in CMS
    * step 2 : start sync :  messages are marked as seen in local storage
    * Step 3 : mark conversation as deleted_requested in local storage  
    * Step 4 : start sync : message are marked as deleted in CMS
    * step 5 : start sync : messages are marked as deleted in local storage
    */
      public void test3()  {
    
              test1();
              
              List<SmsDataObject> messages = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
              for(SmsDataObject sms:messages){
                  mImapLog.updateReadStatus(MessageType.SMS, sms.getMessageId(), MessageData.ReadStatus.READ_REPORT_REQUESTED);
              }

              // sync with CMS : during this first sync, messages are marked as 'Seen' on CMS
              startSynchro();

              String folder = CmsUtils.contactToCmsFolder(mSettings, SmsIntegrationUtils.Test1.contact);
              for(MessageData messageData : mImapLog.getMessages(folder).values()){
                  Assert.assertEquals(ReadStatus.READ, messageData.getReadStatus());
              }

               messages = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
              for(SmsDataObject sms:messages){
                  mImapLog.updateDeleteStatus(MessageType.SMS, sms.getMessageId(), MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
              }

              // sync with CMS : during this first sync, messages are marked as 'Deleted' on CMS
              startSynchro();

                for(MessageData messageData : mImapLog.getMessages(folder).values()){
                    Assert.assertEquals(DeleteStatus.DELETED ,messageData.getDeleteStatus());
                }
      }

      /**
       * Test4 
       * Test 1 +
       * Step 0 : delete mailbox from CMS  
       * Step 1 : mark conversation as seen_requested in local storage  
       * Step 2 : start sync
       * step 3 : check that conversation is marked as seen
       */
     public void test4()  {
   
            test1();
             // delete mailbox on CMS
             try {
                 deleteRemoteMailbox(CmsUtils.contactToCmsFolder(mSettings, SmsIntegrationUtils.Test1.contact));
            } catch (ImapServiceNotAvailableException e) {
                e.printStackTrace();
                Assert.fail();
            }

             List<SmsDataObject> messages = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
             for(SmsDataObject sms:messages){
                 mImapLog.updateReadStatus(MessageType.SMS, sms.getMessageId(), MessageData.ReadStatus.READ_REPORT_REQUESTED);
             }

             startSynchro();

             String folder = CmsUtils.contactToCmsFolder(mSettings, SmsIntegrationUtils.Test1.contact);
             for(MessageData messageData : mImapLog.getMessages(folder).values()){
                 Assert.assertEquals(ReadStatus.READ_REPORT_REQUESTED, messageData.getReadStatus());
             }

             for(SmsDataObject sms:messages){
                 mImapLog.updateDeleteStatus(MessageType.SMS, sms.getMessageId(), MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
             }

             startSynchro();
             for(MessageData messageData : mImapLog.getMessages(folder).values()){
                 Assert.assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, messageData.getDeleteStatus());
             }
     }
  
     /**
      * Test5 
      * Step 1 : mark conversation as seen_requested in local storage  
      * Step 2 : start sync
      * step 3 : check that conversation is marked as seen
      */
        public void test5()  {
                           
            test1();
            
            // mark messages as deleted on server and expunge them.
            try {
                updateRemoteFlags(Arrays.asList(SmsIntegrationUtils.Test5.flagChangesDeleted));
                deleteRemoteMessages(CmsUtils.contactToCmsFolder(mSettings, SmsIntegrationUtils.Test1.contact));
            } catch (Exception e) {
                Assert.fail();
            }

            List<SmsDataObject> messages = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
            for(SmsDataObject sms:messages){
                mImapLog.updateReadStatus(MessageType.SMS, sms.getMessageId(), MessageData.ReadStatus.READ_REPORT_REQUESTED);
            }

            startSynchro();

            String folder = CmsUtils.contactToCmsFolder(mSettings, SmsIntegrationUtils.Test1.contact);
            for(MessageData messageData : mImapLog.getMessages(folder).values()){
                Assert.assertEquals(ReadStatus.READ, messageData.getReadStatus());
            }

            for(SmsDataObject sms:messages){
                mImapLog.updateDeleteStatus(MessageType.SMS, sms.getMessageId(), MessageData.DeleteStatus.DELETED_REPORT_REQUESTED);
            }

            startSynchro();

            for(MessageData messageData : mImapLog.getMessages(folder).values()){
                Assert.assertEquals(DeleteStatus.DELETED, messageData.getDeleteStatus());
            }

        }
    
        /**
         * Test6 : check correlation algorithm (messages having different content) 
         * step 1 : create a conversation on CMS server  
         * step 2 : create a conversation in local storage
         * step 3 : start a sync
         */
           public void test6()  {

               try {
                   
               deleteLocalStorage(true, true);
               deleteRemoteStorage();

               // create messages on CMS    
               createRemoteMessages(SmsIntegrationUtils.Test1.conversation);
               
               // create messages in local storage
               for(SmsDataObject sms : SmsIntegrationUtils.Test1.conversation){
                   mXmsLog.addSms(sms);
               }
                           
               startSynchro();

               Assert.assertEquals(SmsIntegrationUtils.Test1.conversation.length,mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact).size());
               } catch (Exception e) {
                   Assert.fail();
               }
           }
           
           /**
            * Test6 : check correlation algorithm (messages having the same content) 
            * step 1 : create a conversation on CMS server  
            * step 2 : create a conversation in local storage
            * step 3 : start a sync
            */
              public void test7()  {

                  try {
                      
                  deleteLocalStorage(true, true);
                  deleteRemoteStorage();

                  // create messages on CMS    
                  createRemoteMessages(SmsIntegrationUtils.Test7.conversation);
                  
                  // create messages in local storage
                  for(SmsDataObject sms : SmsIntegrationUtils.Test7.conversation){
                      mXmsLog.addSms(sms);
                      String messageId = sms.getMessageId();
                      mImapLog.addMessage(
                              new MessageData(
                                      CmsUtils.contactToCmsFolder(mSettings, sms.getContact()),
                                      Test7.imapReadStatus,
                                      Test7.imapDeleteStatus,
                                      PushStatus.PUSHED,
                                      MessageType.SMS,
                                      messageId,null)
                      );
                  }
                              
                  startSynchro();

                  List<SmsDataObject> sms = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
                  Assert.assertEquals(SmsIntegrationUtils.Test7.conversation.length,sms.size());
                  Map<Integer, MessageData>  imapData = mImapLog.getMessages(SmsIntegrationUtils.Test1.folderName);
                  Assert.assertEquals(SmsIntegrationUtils.Test7.conversation.length,imapData.size());
                        
                  Integer uid=1;
                  for(SmsDataObject message : sms ){
                      Assert.assertEquals(imapData.get(uid).getMessageId(), message.getMessageId());
                      uid++;
                  }
                  
                  
                  } catch (Exception e) {
                      e.printStackTrace();
                      Assert.fail();
                  }

              }    
                   
              /**
               * Test8 : check correlation algorithm (messages having the same content)
               * The local storage has 3 messages (with the same content)
               * whereas the CMS has only 2 messages
               * --> The first local message will not be mapped with a message from CMS  
               * --> No download of message from CMS (imap network trace)    
               * step 1 : create a conversation on CMS server  
               * step 2 : create a conversation in local storage
               * step 3 : start a sync
               */
                 public void test8()  {

                     try {
                                
                         deleteLocalStorage(true, true);                         
                         deleteRemoteStorage();
                         
                     // create messages on CMS    
                     createRemoteMessages(SmsIntegrationUtils.Test8.conversation_remote);
                     
                     // create messages in local storage
                     for(SmsDataObject sms : SmsIntegrationUtils.Test8.conversation_local){
                         mXmsLog.addSms(sms);
                         mImapLog.addMessage(
                                 new MessageData(
                                         CmsUtils.contactToCmsFolder(mSettings, sms.getContact()),
                                         Test8.imapReadStatus,
                                         Test8.imapDeleteStatus,
                                         PushStatus.PUSHED,
                                         MessageType.SMS,
                                         sms.getMessageId(),null)
                         );

                     }
                                 
                     startSynchro();

                     List<SmsDataObject> sms = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
                     Map<Integer, MessageData>  imapData = mImapLog.getMessages(SmsIntegrationUtils.Test1.folderName);



                     Assert.assertTrue(sms.size() > imapData.size());
                     
                     // start at 1, we do not consider the first local message which is not mapped with CMS
                     for(int i=1; i< sms.size();i++){
                         Assert.assertEquals(imapData.get(i).getMessageId(), sms.get(i).getMessageId());
                     }
                     
                     } catch (Exception e) {
                         Assert.fail();
                     }

                 }
                 
                 /**
                  * Test9 : check correlation algorithm (messages having the same content)
                  * The local storage has 2 messages (with the same content)
                  * whereas the CMS has 3 messages  
                  * --> One message should be downloaded from CMS (imap network trace)    
                  * step 1 : create a conversation on CMS server  
                  * step 2 : create a conversation in local storage
                  * step 3 : start a sync
                  */
                    public void test9()  {

                        try {
                                   
                            deleteLocalStorage(true, true);                         
                            deleteRemoteStorage();
                            
                        // create messages on CMS    
                        createRemoteMessages(SmsIntegrationUtils.Test9.conversation_remote);
                        
                        // create messages in local storage
                        for(SmsDataObject sms : SmsIntegrationUtils.Test9.conversation_local){
                            mXmsLog.addSms(sms);
                            String messageId = sms.getMessageId();
                            mImapLog.addMessage(
                                    new MessageData(
                                            CmsUtils.contactToCmsFolder(mSettings, sms.getContact()),
                                            Test9.imapReadStatus,
                                            Test9.imapDeleteStatus,
                                            PushStatus.PUSHED,
                                            MessageType.SMS,
                                            messageId,null)
                            );
                        }
                                    
                        startSynchro();

                        List<SmsDataObject> sms = mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test1.contact);
                        Map<Integer, MessageData>  imapData = mImapLog.getMessages(SmsIntegrationUtils.Test1.folderName);
                        
                        Assert.assertTrue(sms.size() == imapData.size());
                                                
                        } catch (Exception e) {
                            Assert.fail();
                        }

                    }
              
                    /**
                     * Test10 : multi contact 
                     */
                       public void test10()  {

                           try {
                                      
                               deleteLocalStorage(true, true);                         
                               deleteRemoteStorage();
                                   
                           createRemoteMessages(SmsIntegrationUtils.Test10.conversation_1);                           
                           startSynchro();
                           
                           createRemoteMessages(SmsIntegrationUtils.Test10.conversation_2);
                           for(SmsDataObject sms : SmsIntegrationUtils.Test10.conversation_2){
                               mXmsLog.addSms(sms);
                               mImapLog.addMessage(
                                       new MessageData(
                                               CmsUtils.contactToCmsFolder(mSettings, sms.getContact()),
                                               Test9.imapReadStatus,
                                               Test9.imapDeleteStatus,
                                               PushStatus.PUSHED,
                                               MessageType.SMS,
                                               sms.getMessageId(),null)
                               );
                           }

                           startSynchro();

                           createRemoteMessages(SmsIntegrationUtils.Test10.conversation_3);                           
                           startSynchro();

                           
                           Assert.assertEquals(SmsIntegrationUtils.Test10.conversation_1.length, mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test10.contact1).size());
                           Assert.assertEquals(SmsIntegrationUtils.Test10.conversation_2.length, mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test10.contact2).size());
                           Assert.assertEquals(SmsIntegrationUtils.Test10.conversation_3.length, mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.Test10.contact3).size());
                           
                           Assert.assertEquals(SmsIntegrationUtils.Test10.conversation_1.length,mImapLog.getMessages(SmsIntegrationUtils.Test10.folder1).size());
                           Assert.assertEquals(SmsIntegrationUtils.Test10.conversation_2.length,mImapLog.getMessages(SmsIntegrationUtils.Test10.folder2).size());
                           Assert.assertEquals(SmsIntegrationUtils.Test10.conversation_3.length,mImapLog.getMessages(SmsIntegrationUtils.Test10.folder3).size());
                                                                              
                           } catch (Exception e) {
                               Assert.fail();
                           }

                       }
                       
   public void testLoad(){
       
       try {
                  
           deleteLocalStorage(true, true);                         
           deleteRemoteStorage();
               
           for(int i=0;i<TestLoad.iteration;i++ ){
               createRemoteMessages(SmsIntegrationUtils.TestLoad.conversation_1);
               createRemoteMessages(SmsIntegrationUtils.Test10.conversation_2);
               for(SmsDataObject sms : SmsIntegrationUtils.Test10.conversation_2){
                   mXmsLog.addSms(sms);
               }        
               createRemoteMessages(SmsIntegrationUtils.Test10.conversation_3);
           }
                                                         
       startSynchro();
                        
       Assert.assertEquals(TestLoad.iteration*SmsIntegrationUtils.TestLoad.conversation_1.length, mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.TestLoad.contact1).size());
       Assert.assertEquals(TestLoad.iteration*SmsIntegrationUtils.TestLoad.conversation_2.length, mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.TestLoad.contact2).size());
       Assert.assertEquals(TestLoad.iteration*SmsIntegrationUtils.TestLoad.conversation_3.length, mXmsLogEnvIntegration.getMessages(SmsIntegrationUtils.TestLoad.contact3).size());
       
       } catch (Exception e) {
           Assert.fail();
       }

   
       
   }                    
   private void deleteLocalStorage(boolean deleteImapData, boolean deleteMessages){
       if(deleteImapData){
           mImapLog.removeFolders(true);
           assertTrue(mImapLog.getFolders().isEmpty());
           assertTrue(mImapLog.getMessages().isEmpty());
       }
       if(deleteMessages){
           mXmsLog.deleteAllEntries();
       }   
       
   }
   
   private void createRemoteMessages(XmsDataObject[] messages) throws ImapServiceNotAvailableException {
       PushMessageTask task = new PushMessageTask(mContext, mSettings, mBasicImapService, mXmsLog, mImapLog, null);
       task.pushMessages(Arrays.asList(messages));
   }
   
   private void deleteRemoteStorage() throws ImapServiceNotAvailableException{       
       DeleteTask deleteTask = new DeleteTask(mBasicImapService, Operation.DELETE_ALL, null);
       deleteTask.delete();
   }

   private void deleteRemoteMailbox(String mailbox) throws ImapServiceNotAvailableException{       
       DeleteTask deleteTask = new DeleteTask(mBasicImapService, Operation.DELETE_MAILBOX, null);
       deleteTask.delete(mailbox);
   }

   private void deleteRemoteMessages(String mailbox) throws ImapServiceNotAvailableException{       
       DeleteTask deleteTask = new DeleteTask(mBasicImapService, Operation.DELETE_MESSAGES, null);
       deleteTask.delete(mailbox);
   }

   private void updateRemoteFlags(List<FlagChange> changes) throws ImapServiceNotAvailableException {       
       UpdateFlagTask task = new UpdateFlagTask(mBasicImapService, changes, null);
       task.updateFlags();
   }
   
   private void startSynchro(){
       mSyncStrategy.execute();
   }
   
   private ImapMessage buildImapMessage(int uid, Message message){
       
       ImapMessageMetadata metadata = new ImapMessageMetadata(uid);       
       metadata.getFlags().addAll(message.getFlag());
       
       Part part = new Part();
       Iterator<Entry<String, String>> iter = message.getHeaders().entrySet().iterator();
       while(iter.hasNext()){
           Entry<String, String> entry = iter.next();
           part.setHeader(entry.getKey(), entry.getValue());
       }
       part.setContent(message.getContent());
       
       ImapMessage imapMessage = new ImapMessage(uid, metadata, part); 
       imapMessage.setFolderPath(message.getFolder());
       return imapMessage;
   }
   
}

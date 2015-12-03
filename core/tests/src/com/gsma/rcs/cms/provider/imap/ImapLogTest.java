package com.gsma.rcs.cms.provider.imap;

import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;

import android.content.Context;
import android.test.AndroidTestCase;

import java.util.Map;

public class ImapLogTest extends AndroidTestCase {

    private ImapLog mImapLog;    
    private Context mContext;
    
    private FolderData[] mFolders;
    private MessageData[] mMessages;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mImapLog = ImapLog.createInstance(mContext);
        
        mFolders = new FolderData[] {
        		new FolderData("folder1",1,123,1),
        		new FolderData("folder2",1,1234,1),
        		new FolderData("folder3",1,12345,1)
        };
        
        mMessages = new MessageData[]{
        		new MessageData(mFolders[0].getName(), 1, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS,"messageId1",null),
        		new MessageData(mFolders[1].getName(), 1, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS,"messageId1",null),
        		new MessageData(mFolders[1].getName(), 2, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS,"messageId2",null),
        		new MessageData(mFolders[2].getName(), 1, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS,"messageId1",null),
        		new MessageData(mFolders[2].getName(), 2, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS,"messageId2",null),
        		new MessageData(mFolders[2].getName(), 3, ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, PushStatus.PUSHED, MessageType.SMS,"messageId3",null)
        };
        
        mImapLog.removeFolders(false);
        mImapLog.removeMessages();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapLog.removeFolders(false);
        mImapLog.removeMessages();
    }

    public void testAddFolder() {    	
    	FolderData folder;    	
    	mImapLog.addFolder(mFolders[0]);    	
    	folder = mImapLog.getFolder(mFolders[0].getName());
    	assertEquals(mFolders[0], folder);
    	
    	folder = mImapLog.getFolder("dummy");
    	assertNull(folder);
    }
    
    public void testAddMessage() {      
        MessageData message;
        assertEquals(new Integer(0),mImapLog.getMaxUidForMessages(mMessages[0].getFolder()));
        mImapLog.addMessage(mMessages[0]);
        assertEquals(new Integer(1),mImapLog.getMaxUidForMessages(mMessages[0].getFolder()));
        message = mImapLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
        assertEquals(mMessages[0], message);
        
        message = mImapLog.getMessage("dummy", 0);
        assertNull(message);
    }
        
    public void testGetFolders() {
        
        Map<String, FolderData> folders = mImapLog.getFolders();
        assertEquals(0, folders.size());

        mImapLog.addFolder(mFolders[0]);
        folders = mImapLog.getFolders();
        assertEquals(1, folders.size());
        assertTrue(folders.containsKey("folder1"));
        
        mImapLog.addFolder(mFolders[0]);
        folders = mImapLog.getFolders();
        assertEquals(1, folders.size());
        assertTrue(folders.containsKey("folder1"));
        
        mImapLog.addFolder(mFolders[1]);
        folders = mImapLog.getFolders();
        assertEquals(2, folders.size());
        assertTrue(folders.containsKey("folder1"));
        assertTrue(folders.containsKey("folder2"));
        
        mImapLog.addFolder(mFolders[2]);
        folders = mImapLog.getFolders();
        assertEquals(3, folders.size());
        assertTrue(folders.containsKey("folder1"));
        assertTrue(folders.containsKey("folder2"));
        assertTrue(folders.containsKey("folder3"));
    }
    
    public void testUpdateFolder() {    	
    	FolderData folder;
    	mImapLog.addFolder(mFolders[0]);
    	
    	folder = mImapLog.getFolder(mFolders[0].getName());
    	assertEquals(mFolders[0], folder);
    	
    	mImapLog.addFolder(new FolderData(
    			mFolders[0].getName(),
    			0,
    			0,
    			0
    			));
    	
    	folder = mImapLog.getFolder(mFolders[0].getName());
    	assertEquals(1,mImapLog.getFolders().size());
    	assertEquals(Integer.valueOf(0), folder.getModseq());
    	assertEquals(Integer.valueOf(0), folder.getNextUid());
    	assertEquals(Integer.valueOf(0), folder.getUidValidity());    	    	
    }
    
    public void testRemoveFolder() {
    	
    	for(FolderData folder : mFolders){
    		mImapLog.addFolder(folder);	
    	}    	
    	assertEquals(mFolders.length, mImapLog.getFolders().size());

    	for(int i=0; i<mFolders.length; i++){
    		mImapLog.removeFolder(mFolders[i].getName(), false);
    		assertEquals(mFolders.length-(i+1), mImapLog.getFolders().size());
    	}    	

    }

    public void testRemoveFolders() {
    	
    	for(FolderData folder : mFolders){
    		mImapLog.addFolder(folder);	
    	}
    	assertEquals(mFolders.length, mImapLog.getFolders().size());
    	
    	mImapLog.removeFolders(false);
    	assertEquals(0, mImapLog.getFolders().size());
    }
    
    public void testGetMessages() {
    	
    	Map<Integer, MessageData> messages = mImapLog.getMessages();    	
        assertEquals(0, messages.size());

    	for(int i=0; i< mMessages.length; i++){
    		mImapLog.addMessage(mMessages[i]);
    		assertEquals(i+1, mImapLog.getMessages().size());
    	} 
        
    	messages = mImapLog.getMessages(mFolders[0].getName()); 
    	assertEquals(1,messages.size());
    	assertEquals(mMessages[0],messages.get(mMessages[0].getUid()));

    	messages = mImapLog.getMessages(mFolders[1].getName()); 
    	assertEquals(2,messages.size());
    	assertEquals(mMessages[1],messages.get(mMessages[1].getUid()));
    	assertEquals(mMessages[2],messages.get(mMessages[2].getUid()));

    	messages = mImapLog.getMessages(mFolders[2].getName()); 
    	assertEquals(3,messages.size());
    	assertEquals(mMessages[3],messages.get(mMessages[3].getUid()));
    	assertEquals(mMessages[4],messages.get(mMessages[4].getUid()));
    	assertEquals(mMessages[5],messages.get(mMessages[5].getUid()));

    	for(int i=0; i< mMessages.length; i++){
    		assertEquals(mMessages[i],mImapLog.getMessage(mMessages[i].getFolder(),mMessages[i].getUid()));    		
    	} 
    	
        for(int i=0; i< mMessages.length; i++){
            assertEquals(mMessages[i],mImapLog.getMessage(mMessages[i].getFolder(),mMessages[i].getMessageId()));         
        } 

        for(int i=0; i< mMessages.length; i++){
            assertTrue(mImapLog.getMessageId(mMessages[i].getFolder(),mMessages[i].getUid()) != ImapLog.INVALID_ID);         
        } 

        for(int i=0; i< mMessages.length; i++){
            assertEquals(mMessages[i].getUid(),mImapLog.getUidForXmsMessage(mMessages[i].getMessageId()));
        } 
        
    }


    
    public void testUpdateMessage() {    	
    	MessageData message;
    	mImapLog.addMessage(mMessages[0]);
    	
    	message = mImapLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());
    	assertEquals(mMessages[0], message);
    	
    	mImapLog.addMessage(new MessageData(
    			mMessages[0].getFolder(),
    			1,
    			ReadStatus.READ,
    			DeleteStatus.DELETED,
				PushStatus.PUSHED,
    			MessageType.MMS,
    			"messageId1",null));
    	
    	message = mImapLog.getMessage(mMessages[0].getFolder(), mMessages[0].getUid());    	
    	assertEquals(1,mImapLog.getMessages().size());
		assertEquals(DeleteStatus.DELETED,message.getDeleteStatus());
		assertEquals(ReadStatus.READ, message.getReadStatus());
    	assertEquals(MessageType.MMS, message.getMessageType());
    }
    
    public void testRemoveMessages() {
    	
    	for(MessageData message : mMessages){
    		mImapLog.addMessage(message);	
    	}    	
    	assertEquals(mMessages.length, mImapLog.getMessages().size());

    	for(int i=0; i<mMessages.length; i++){
    		mImapLog.removeMessage(mMessages[i].getFolder(), mMessages[i].getUid());
    		assertEquals(mMessages.length-(i+1), mImapLog.getMessages().size());
    	}    	

    }

    public void testRemoveAllMessages() {
    	
    	for(MessageData message : mMessages){
    		mImapLog.addMessage(message);	
    	}    	
    	assertEquals(mMessages.length, mImapLog.getMessages().size());    	
    	mImapLog.removeMessages();;
    	assertEquals(0, mImapLog.getMessages().size());
    }

}

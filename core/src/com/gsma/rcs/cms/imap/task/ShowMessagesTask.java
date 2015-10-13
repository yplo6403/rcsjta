package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;

import com.sonymobile.rcs.imap.ImapMessage;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ShowMessagesTask extends AsyncTask<String, String, List<ImapMessage>> {
       
    private final ShowMessagesTaskListener mListener;
    private final BasicImapService mImapService;
    private String[] mParams;
    
    /**
     * @param rcsSettings
     * @param listener
     * @param imapService 
     * @throws ImapServiceNotAvailableException 
     */
    public ShowMessagesTask(BasicImapService imapService, ShowMessagesTaskListener listener) throws ImapServiceNotAvailableException {
        mImapService = imapService;
        mListener = listener;                
    }

    @Override
    protected List<ImapMessage> doInBackground(String... params) {
        mParams = params;
        Thread currentThread = Thread.currentThread();
        String currentName = currentThread.getName();
        currentThread.setName(BasicSynchronizationTask.class.getSimpleName());

        try {
            mImapService.init();
            List<ImapMessage> msg = getMessages((BasicImapService) mImapService);
            return msg;
        } catch (Exception e) {
            return null;            
        }
        finally {
            Thread.currentThread().setName(currentName);
            ImapServiceManager.releaseService(mImapService);
        }
    }

    private List<ImapMessage> getMessages(BasicImapService imap) {
        List<ImapMessage> messages = new ArrayList<ImapMessage>();
        try {
            for (ImapFolder imapFolder : imap.listStatus()) {
                imap.selectCondstore(imapFolder.getName());
                List<ImapMessage> imapMessages = imap.fetchAllMessages(); 
                for(ImapMessage imapMessage : imapMessages){
                    imapMessage.setFolderPath(imapFolder.getName());
                    messages.add(imapMessage);
                }                    
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }    
    
    @Override
    protected void onPostExecute(List<ImapMessage> result) {
        if(mListener != null){
            mListener.onShowMessagesTaskExecuted(mParams, result);
        }
    }
    
    /**
    *
    */
   public interface ShowMessagesTaskListener {
       
       /**
        * @param params
        * @param result
        */
       public void onShowMessagesTaskExecuted(String[] params, List<ImapMessage> result);
   }
}

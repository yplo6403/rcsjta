package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class DeleteTask extends AsyncTask<String, String, Boolean> {
    
    public enum Operation {
        DELETE_ALL, // delete all content for a user
        DELETE_MAILBOX, // delete one mailbox
        DELETE_MESSAGES // delete messages of a mailbox
    }
    
    private final BasicImapService mImapService;
    private final Operation mOperation;
    private final DeleteTaskListener mListener;      
    private String[] mParams;
    
    /** 
     * @param imapService 
     * @param operation 
     * @param listener 
     * @throws ImapServiceNotAvailableException 
     */
    public DeleteTask(BasicImapService imapService, Operation operation, DeleteTaskListener listener) throws ImapServiceNotAvailableException {
        mImapService = imapService;
        mOperation = operation;
        mListener = listener;                
    }

    @Override
    protected Boolean doInBackground(String... params) {
        mParams = params;
        Thread currentThread = Thread.currentThread();
        String currentName = currentThread.getName();
        currentThread.setName(BasicSynchronizationTask.class.getSimpleName());
        try {            
            mImapService.init();
            return  delete(mParams);  
        } catch (Exception e) {
            return false;
        }
        finally {
            Thread.currentThread().setName(currentName);
            ImapServiceManager.releaseService(mImapService);    
        }
        
    }

    /**
     * @param params 
     * @return boolean
     */
    public boolean delete(String... params) {
       
        try {
        switch (mOperation){
            case DELETE_ALL:
                deleteAll();
                break;
            case DELETE_MAILBOX:
                deleteMailbox(params[0]);
                break;
            case DELETE_MESSAGES:
                deleteMessages(params[0]);
                break;
        }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ImapException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private void deleteAll() throws IOException, ImapException{
        for (ImapFolder imapFolder : mImapService.listStatus()) {
            mImapService.delete(imapFolder.getName());
        }        
    }
    
    private void deleteMailbox(String mailbox) throws IOException, ImapException{
            mImapService.delete(mailbox);
    }
    
    private void deleteMessages(String mailbox) throws IOException, ImapException{
        mImapService.select(mailbox);
        mImapService.expunge();
    } 
        
    @Override
    protected void onPostExecute(Boolean result) {
        if(mListener != null){
            mListener.onDeleteTaskExecuted(mParams, result);
        }
    }
    
    /**
     *
     */
    public interface DeleteTaskListener {
        
        /**
         * @param params
         * @param result
         */
        public void onDeleteTaskExecuted(String[] params, Boolean result);
    }

}

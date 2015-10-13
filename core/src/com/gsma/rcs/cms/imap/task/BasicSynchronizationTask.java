
package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.BasicSyncStrategy;
import com.gsma.rcs.utils.logger.Logger;

import android.os.AsyncTask;

/**
 *
 */
public class BasicSynchronizationTask extends AsyncTask<String,String,Boolean> {
        
    private static final Logger sLogger = Logger.getLogger(BasicSynchronizationTask.class.getSimpleName());
    
    private final BasicSynchronizationTaskListener mListener;
    private final LocalStorage mLocalStorageHandler;        
    private final BasicImapService mImapService;
    private String[] mParams;
        
    /**
     * @param imapService
     * @param localStorageHandler
     * @param listener 
     * @throws ImapServiceNotAvailableException
     */
    public BasicSynchronizationTask(
            BasicImapService imapService,
            LocalStorage localStorageHandler,                         
            BasicSynchronizationTaskListener listener
            ) throws ImapServiceNotAvailableException {
        mImapService = imapService;        
        mLocalStorageHandler = localStorageHandler;
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
                BasicSyncStrategy strategy = new BasicSyncStrategy(mImapService, mLocalStorageHandler);
                if(params.length == 1){ // sync for only a conversation
                    strategy.execute(params[0]);
                }else{ // full sync
                    strategy.execute();                    
                }
                return strategy.getExecutionResult();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            finally {
                Thread.currentThread().setName(currentName);
                ImapServiceManager.releaseService(mImapService);    
            }
    }
        
//    /**
//     * @param listener
//     */
//    public static void registerListener(BasicSynchronizationTaskListener listener){
//        synchronized(mListeners){
//            mListeners.add(listener);    
//        }        
//    }
//
//    /**
//     * @param listener
//     */
//    public static void unregisterListener(BasicSynchronizationTaskListener listener){
//        synchronized(mListeners){
//            mListeners.remove(listener);
//        }        
//    }

//    @Override
//    protected void onPostExecute(Boolean result) {
//        synchronized(mListeners){            
//            if(sLogger.isActivated()){
//                sLogger.debug("listeners : ".concat(String.valueOf(mListeners.size())));
//            }                        
//            for (BasicSynchronizationTaskListener listener : mListeners) {
//                listener.onBasicSynchronizationTaskExecuted(mParams, result);
//            }
//            mListeners.clear();
//        }
//    }

  @Override
  protected void onPostExecute(Boolean result) {
      if(mListener!=null){
          mListener.onBasicSynchronizationTaskExecuted(mParams, result);          
      }
  }

    /**
    *
    */
   public interface BasicSynchronizationTaskListener {
       
       /**
        * @param params
        * @param result
        */
       public void onBasicSynchronizationTaskExecuted(String[] params, Boolean result);
   }
}

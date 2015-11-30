
package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.BasicSyncStrategy;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.os.AsyncTask;

/**
 *
 */
public class BasicSynchronizationTask extends AsyncTask<String,String,Boolean> {
        
    private static final Logger sLogger = Logger.getLogger(BasicSynchronizationTask.class.getSimpleName());

    private final Context mContext;
    private final RcsSettings mRcsSettings;
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
            Context context,
            RcsSettings rcsSettings,
            BasicImapService imapService,
            LocalStorage localStorageHandler,                         
            BasicSynchronizationTaskListener listener
            ) {
        mContext = context;
        mRcsSettings = rcsSettings;
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
                if(params.length == 1){ // sync for only a conversation
                    return syncFolder(params[0]);
                }else{ // full sync
                    return syncAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            finally {
                Thread.currentThread().setName(currentName);
                ImapServiceManager.releaseService(mImapService);    
            }
    }

    public boolean syncFolder(String folder){
        if(sLogger.isActivated()){
            sLogger.info(new StringBuilder("Sync folder : ").append(folder).toString());
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings, mImapService, mLocalStorageHandler);
        strategy.execute(folder);
        return strategy.getExecutionResult();
    }

    public boolean syncAll(){
        if(sLogger.isActivated()){
            sLogger.info("Sync all");
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings, mImapService, mLocalStorageHandler);
        strategy.execute();
        return strategy.getExecutionResult();
    }

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
       void onBasicSynchronizationTaskExecuted(String[] params, Boolean result);
   }
}

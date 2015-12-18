// FG add copyrights + javadoc

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
public class BasicSynchronizationTask implements Runnable {
        

    private static final Logger sLogger = Logger.getLogger(BasicSynchronizationTask.class
            .getSimpleName());

    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final BasicSynchronizationTaskListener mListener;
    private final LocalStorage mLocalStorageHandler;
    private final BasicImapService mImapService;
    private final String mFolderName;

    public BasicSynchronizationTask(
            Context context,
            RcsSettings rcsSettings,
            BasicImapService imapService,
            LocalStorage localStorageHandler,
            String folderName,
            BasicSynchronizationTaskListener listener) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mImapService = imapService;
        mLocalStorageHandler = localStorageHandler;
        mListener = listener;
        mFolderName = folderName;
    }

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
            BasicSynchronizationTaskListener listener) {
        this(context, rcsSettings, imapService, localStorageHandler, null, listener);
    }

    @Override
    public void run() {
        boolean result = false;
            try {
                mImapService.init();
                if(mFolderName == null){
                    result = syncFolder(mFolderName);
                } else{
                    result = syncAll();
                }
            } catch (Exception e) {
                if(sLogger.isActivated()){
                    sLogger.debug(e.getMessage());
                    e.printStackTrace();
                }
            }
            finally {
                ImapServiceManager.releaseService(mImapService);
                if(mListener!=null){
                    mListener.onBasicSynchronizationTaskExecuted(result);
                }
            }
    }

    public boolean syncFolder(String folder) {
        if (sLogger.isActivated()) {
            sLogger.info("Sync folder : " + folder);
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings, mImapService,
                mLocalStorageHandler);
        strategy.execute(folder);
        return strategy.getExecutionResult();
    }

    public boolean syncAll() {
        if (sLogger.isActivated()) {
            sLogger.info("Sync all");
        }
        BasicSyncStrategy strategy = new BasicSyncStrategy(mContext, mRcsSettings, mImapService,
                mLocalStorageHandler);
        strategy.execute();
        return strategy.getExecutionResult();
    }

    /**
    *
    */
   public interface BasicSynchronizationTaskListener {
       
       /**
        * @param result
        */
       void onBasicSynchronizationTaskExecuted(Boolean result);
   }
}

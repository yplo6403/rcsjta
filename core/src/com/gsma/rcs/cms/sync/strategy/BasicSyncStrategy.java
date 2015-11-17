
package com.gsma.rcs.cms.sync.strategy;

import com.gsma.rcs.cms.fordemo.ImapCommandController;
import com.gsma.rcs.cms.fordemo.ImapContext;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.provider.imap.DataUtils;
import com.gsma.rcs.cms.provider.imap.FolderData;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData.PushStatus;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.ISynchronizer;
import com.gsma.rcs.cms.sync.SynchronizerImpl;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.cpm.ms.impl.sync.AbstractSyncStrategy;
import com.sonymobile.rcs.cpm.ms.sync.MutableReport;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasicSyncStrategy extends AbstractSyncStrategy {

    private static final long serialVersionUID = 1L;

    private static Logger sLogger = Logger.getLogger(BasicSyncStrategy.class.getName());

    private BasicImapService mImapService;

    private boolean mExecutionResult;

    private LocalStorage mLocalStorageHandler;
    private ISynchronizer mSynchronizer;

    /**
     * @param imapService
     * @param localStorageHandler
     */
    public BasicSyncStrategy(BasicImapService imapService,
            LocalStorage localStorageHandler) {
        mImapService = imapService;
        mLocalStorageHandler = localStorageHandler;
    }

    /**
     * Execute a full sync 
     */
    public void execute() {
        execute((String)null);
    }
    
    /**
     * Execute a sync for only one folder
     * @param folderName
     */
    public void execute(String folderName) {

        mExecutionResult = false;
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug(">>> BasicSyncStrategy.execute");
        }

        try {
            Map<String, FolderData> localFolders = mLocalStorageHandler.getLocalFolders();
            mSynchronizer = new SynchronizerImpl(mImapService);

            for (ImapFolder remoteFolder : mImapService.listStatus()) {
                String remoteFolderName = remoteFolder.getName();
                if(folderName!=null && !remoteFolderName.equals(folderName)){
                    continue;
                }
                FolderData localFolder = localFolders.get(remoteFolderName);
                if (localFolder == null) {
                    localFolder = new FolderData(remoteFolderName);
                }
                if (shouldStartRemoteSynchronization(localFolder, remoteFolder)) {
                    startRemoteSynchro(localFolder, remoteFolder);
                    mLocalStorageHandler.applyFolderChange(DataUtils.toFolderData(remoteFolder));
                }
                
                // sync CMS with local change
                List<FlagChange> flagChanges = mLocalStorageHandler.getLocalFlagChanges(remoteFolderName);
                mSynchronizer.syncLocalFlags(flagChanges);            
                mLocalStorageHandler.finalizeLocalFlagChanges(flagChanges);                
            }
            
            //TODO FGI : TO BE REMOVED
            // Demo purpose only 
            // push on CMS server, messages that are marked as PUSH_requested in database
            // try to get an instance of XmsLog
            XmsLog xmsLog = XmsLog.getInstance(null);
            PartLog partLog = PartLog.getInstance(null);
            if(xmsLog!=null){
                List<XmsData> messagesToPush = xmsLog.getMessages(PushStatus.PUSH_REQUESTED);
                if(!messagesToPush.isEmpty()){
                    ImapContext localContext = new ImapContext();
                    new PushMessageTask(mImapService, xmsLog, partLog, CmsSettings.getInstance().getMyNumber(), localContext, null).pushMessages(messagesToPush);
                    for(String baseId : localContext.getUids().keySet()){
                        xmsLog.updatePushStatus(baseId, XmsData.PushStatus.PUSHED);
                    }
                    ImapContext globalContext = ImapCommandController.getInstance().getContext();
                    globalContext.importLocalContext(localContext);
                }
            }

            mExecutionResult = true;
        } catch (IOException e) {
            e.printStackTrace();
            sLogger.error(e.getMessage());
        } catch (ImapException e) {
            e.printStackTrace();
            sLogger.error(e.getMessage());
        }

        if (logActivated) {
            sLogger.debug("<<< BasicSyncStrategy.execute ");
        }
    }
    /**
     * @param report
     */
    public void execute(MutableReport report) {

        report.setProgress(15);

        execute();

        report.setProgress(100);

    }

    private void startRemoteSynchro(FolderData localFolder, ImapFolder remoteFolder)
            throws IOException, ImapException {
        String folderName = remoteFolder.getName();

        mSynchronizer.selectFolder(folderName);

        if (localFolder.hasMessages()) {
            List<FlagChange> flagChanges = mSynchronizer.syncRemoteFlags(localFolder, remoteFolder);
            mLocalStorageHandler.applyFlagChange(flagChanges);
        }

        List<ImapMessage> messages = mSynchronizer.syncRemoteHeaders(localFolder, remoteFolder);
        Set<Integer> uids = mLocalStorageHandler.filterNewMessages(messages);
        
        List<ImapMessage> newMessages = mSynchronizer.syncRemoteMessages(remoteFolder.getName(), uids);
        mLocalStorageHandler.createMessages(newMessages);

    }

    private boolean shouldStartRemoteSynchronization(FolderData local, ImapFolder remote) {
        boolean sync = false;
        if (local.isNewFolder()) {
            sync = !remote.isEmpty();
        } else if (!local.getUidValidity().equals(remote.getUidValidity())) {
            mLocalStorageHandler.removeLocalFolder(remote.getName());
            sync = true;
        } else if (!local.getModseq().equals(remote.getHighestModseq())) {
            sync = true;
        }

        if (sLogger.isActivated()) {
            sLogger.debug(">>> shouldStartSynchronization : ".concat(String.valueOf(sync)));
            sLogger.debug("local folder : ".concat(local.toString()));
            sLogger.debug("remote folder : ".concat(remote.toString()));
            sLogger.debug("<<< shouldStartSynchronization");
        }
        return sync;
    }

    /**
     * @return result for execution of sync strategy
     */
    public boolean getExecutionResult() {
        return mExecutionResult;
    }

}


package com.gsma.rcs.cms.imap.task;

import android.os.AsyncTask;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UpdateFlagTask extends AsyncTask<String, String, List<FlagChange>> {

    private final UpdateFlagTaskListener mListener;
    private final BasicImapService mImapService;
    private String[] mParams;
    private RcsSettings mSettings;
    private XmsLog mXmsLog;
    private ImapLog mImapLog;
    private List<FlagChange> mFlagChanges;

    public UpdateFlagTask(BasicImapService imapService, List<FlagChange> flagChanges, UpdateFlagTaskListener listener
    ) throws ImapServiceNotAvailableException {
        mImapService = imapService;
        mListener = listener;
        mFlagChanges = flagChanges;
    }
        /**
         * @param imapService
         * @param listener
         * @throws ImapServiceNotAvailableException
         */
    public UpdateFlagTask(BasicImapService imapService,RcsSettings settings,  XmsLog xmsLog, ImapLog imapLog, UpdateFlagTaskListener listener
            ) throws ImapServiceNotAvailableException {
        mImapService = imapService;
        mListener = listener;
        mXmsLog = xmsLog;
        mImapLog = imapLog;
        mSettings = settings;
    }

    @Override
    protected List<FlagChange> doInBackground(String... params) {
        mParams = params;
        try {
            mImapService.init();
            if(mFlagChanges == null){ // get from db
                mFlagChanges = new ArrayList<>();
                mFlagChanges.addAll(getReadChanges());
                mFlagChanges.addAll(getDeleteChanges());
            }
            return updateFlags();
        } catch (Exception e) {
            return null;
        } finally {
            ImapServiceManager.releaseService(mImapService);
        }
    }

    private List<FlagChange> getReadChanges(){
        Map<String, List<Integer>>  folderUidsMap = new HashMap<>();
        for (MessageData messageData : mImapLog.getMessages(ReadStatus.READ_REPORT_REQUESTED)) {
            Integer uid = messageData.getUid();
            if (uid==MessageData.INVALID_UID) {
                continue;
            }
            String folderName = messageData.getFolder();
            List<Integer> uids = folderUidsMap.get(folderName);
            if(uids==null){
                uids = new ArrayList<>();
                folderUidsMap.put(folderName, uids);
            }
            uids.add(uid);
        }

        List<FlagChange> flagChanges = new ArrayList<>();
        Iterator<Map.Entry<String,List<Integer>>> iterator = folderUidsMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,List<Integer>> entry =  iterator.next();
            String folderName = entry.getKey();
            List<Integer> uids = entry.getValue();
            flagChanges.add(new FlagChange(folderName, uids, Flag.Seen));
        }
        return flagChanges;
    }

    private List<FlagChange> getDeleteChanges(){
        Map<String, List<Integer>>  folderUidsMap = new HashMap<>();
        for (MessageData messageData : mImapLog.getMessages(MessageData.DeleteStatus.DELETED_REPORT_REQUESTED)) {
            String folderName = messageData.getFolder();
            Integer uid = messageData.getUid();
            if (uid==MessageData.INVALID_UID) {
                continue;
            }
            List<Integer> uids = folderUidsMap.get(folderName);
            if(uids==null){
                uids = new ArrayList<>();
                folderUidsMap.put(folderName, uids);
            }
            uids.add(uid);
        }

        List<FlagChange> flagChanges = new ArrayList<>();
        Iterator<Map.Entry<String,List<Integer>>> iterator = folderUidsMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,List<Integer>> entry =  iterator.next();
            String folderName = entry.getKey();
            List<Integer> uids = entry.getValue();
            flagChanges.add(new FlagChange(folderName, uids, Flag.Deleted));
        }
        return flagChanges;
    }

    /**
     * @throws IOException
     * @throws ImapException
     */
    public List<FlagChange> updateFlags() {
        List<FlagChange> successFullFlagChanges = new ArrayList<>();
        if(mFlagChanges==null){
            return successFullFlagChanges;
        }

        Thread currentThread = Thread.currentThread();
        String currentName = currentThread.getName();
        currentThread.setName(BasicSynchronizationTask.class.getSimpleName());

        String previousFolder = null;
        try {
            for (FlagChange flagChange : mFlagChanges) {
                String folder = flagChange.getFolder();
                if (!folder.equals(previousFolder)) {
                    mImapService.select(folder);
                    previousFolder = folder;
                }
                switch (flagChange.getOperation()) {
                    case ADD_FLAG:
                        mImapService.addFlags(flagChange.getJoinedUids(), flagChange.getFlags());
                        break;
                    case REMOVE_FLAG:
                        mImapService.removeFlags(flagChange.getJoinedUids(), flagChange.getFlags());
                        break;
                }
                successFullFlagChanges.add(flagChange);
            }
        } catch (IOException | ImapException e) {
            e.printStackTrace();
            Thread.currentThread().setName(currentName);
        }
        return successFullFlagChanges;
    }

    @Override
    protected void onPostExecute(List<FlagChange> successFullFlagChanges) {
        if (mListener != null) {
            mListener.onUpdateFlagTaskExecuted(successFullFlagChanges);
        }
    }



    /**
    *
    */
    public interface UpdateFlagTaskListener {

        /**
         * @param successFullFlagChanges
         */
        void onUpdateFlagTaskExecuted(List<FlagChange> successFullFlagChanges);
    }

}

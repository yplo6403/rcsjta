
package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceManager;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import com.sonymobile.rcs.imap.ImapException;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateFlagTask extends AsyncTask<String, String, Boolean> {

    private final UpdateFlagTaskListener mListener;
    private final BasicImapService mImapService;
    private String[] mParams;
    private static Set<FlagChange> mFlagChangeQueue = new HashSet<FlagChange>();


    /**
     * @param imapService
     * @param listener
     * @param flagChanges
     * @throws ImapServiceNotAvailableException
     */
    public UpdateFlagTask(BasicImapService imapService, List<FlagChange> flagChanges, UpdateFlagTaskListener listener
            ) throws ImapServiceNotAvailableException {
        mImapService = imapService;
        mFlagChangeQueue.addAll(flagChanges);
        mListener = listener;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        mParams = params;
        try {
            mImapService.init();
            updateFlags();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            ImapServiceManager.releaseService(mImapService);
        }
    }

    /**
     * @throws IOException
     * @throws ImapException
     */
    public void updateFlags() {

        Thread currentThread = Thread.currentThread();
        String currentName = currentThread.getName();
        currentThread.setName(BasicSynchronizationTask.class.getSimpleName());

        String previousFolder = null;
        Set<FlagChange> flagChangeSent = new HashSet<FlagChange>();
        try {
            for (FlagChange flagChange : mFlagChangeQueue) {
                String folder = flagChange.getFolder();
                if (folder != previousFolder) {
                    mImapService.select(folder);
                    previousFolder = folder;
                }
                switch (flagChange.getOperation()) {
                    case ADD_FLAG:
                        mImapService.addFlags(flagChange.getUid(), flagChange.getFlags());
                        break;
                    case REMOVE_FLAG:
                        mImapService.removeFlags(flagChange.getUid(), flagChange.getFlags());
                        break;
                }
                flagChangeSent.add(flagChange);
            }
        } catch (IOException | ImapException e) {
            e.printStackTrace();
        }finally {
            Thread.currentThread().setName(currentName);
            mFlagChangeQueue.removeAll(flagChangeSent);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (mListener != null) {
            mListener.onUpdateFlagTaskExecuted(mParams, result);
        }
    }

    /**
    *
    */
    public interface UpdateFlagTaskListener {

        /**
         * @param params
         * @param result
         */
        public void onUpdateFlagTaskExecuted(String[] params, Boolean result);
    }
}

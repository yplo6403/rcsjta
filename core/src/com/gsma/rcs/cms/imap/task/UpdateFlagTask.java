/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Task used to update flag status on the CMS server.
 */
public class UpdateFlagTask implements Runnable {

    private static final Logger sLogger = Logger.getLogger(UpdateFlagTask.class.getSimpleName());

    private final UpdateFlagTaskListener mListener;
    private final ImapServiceController mImapServiceController;
    private ImapLog mImapLog;
    private List<FlagChange> mFlagChanges;
    private final List<FlagChange> mSuccessFullFlagChanges;

    /**
     * Constructor
     * 
     * @param imapServiceController the IMAP service controller
     * @param flagChanges the list of changed flags
     * @param listener the update flag listener
     */
    public UpdateFlagTask(ImapServiceController imapServiceController,
            List<FlagChange> flagChanges, UpdateFlagTaskListener listener) {
        mImapServiceController = imapServiceController;
        mListener = listener;
        mFlagChanges = flagChanges;
        mSuccessFullFlagChanges = new ArrayList<>();
    }

    /**
     * Constructor
     *
     * @param imapServiceController the IMAP service controller
     * @param imapLog the IMAP log accessor
     * @param listener the update flag listener
     */
    public UpdateFlagTask(ImapServiceController imapServiceController, ImapLog imapLog,
            UpdateFlagTaskListener listener) {
        mImapServiceController = imapServiceController;
        mListener = listener;
        mImapLog = imapLog;
        mSuccessFullFlagChanges = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            mImapServiceController.createService();
            if (mFlagChanges == null) { // get from db
                mFlagChanges = new ArrayList<>();
                mFlagChanges.addAll(getReadChanges());
                mFlagChanges.addAll(getDeleteChanges());
            }
            updateFlags();

        } catch (ImapServiceNotAvailableException e) {
            sLogger.warn("Cannot update flag status on the CMS server: service not available");

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Runtime error while updating flag status on CMS server!", e);

        } catch (ImapException | IOException e) {
            sLogger.error("Failed to update flag status on CMS server!", e);

        } finally {
            try {
                mImapServiceController.closeService();
                if (mListener != null) {
                    mListener.onUpdateFlagTaskExecuted(mSuccessFullFlagChanges);
                }
            } catch (RuntimeException e) {
                /*
                 * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
                 * which should be handled/fixed within the code. However the cases when we are
                 * executing operations on a thread unhandling such exceptions will eventually lead
                 * to exit the system and thus can bring the whole system down, which is not
                 * intended.
                 */
                sLogger.error("Runtime error while updating flag status on CMS server!", e);
            }
        }
    }

    private List<FlagChange> getReadChanges() {
        Map<String, Set<Integer>> folderUidsMap = new HashMap<>();
        for (MessageData messageData : mImapLog.getMessages(ReadStatus.READ_REPORT_REQUESTED)) {
            Integer uid = messageData.getUid();
            if (uid == null) {
                continue;
            }
            String folderName = messageData.getFolder();
            Set<Integer> uids = folderUidsMap.get(folderName);
            if (uids == null) {
                uids = new HashSet<>();
                folderUidsMap.put(folderName, uids);
            }
            uids.add(uid);
        }
        List<FlagChange> flagChanges = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> entry : folderUidsMap.entrySet()) {
            String folderName = entry.getKey();
            Set<Integer> uids = entry.getValue();
            flagChanges.add(new FlagChange(folderName, uids, Flag.Seen));
        }
        return flagChanges;
    }

    private List<FlagChange> getDeleteChanges() {
        Map<String, Set<Integer>> folderUidsMap = new HashMap<>();
        for (MessageData messageData : mImapLog
                .getMessages(MessageData.DeleteStatus.DELETED_REPORT_REQUESTED)) {
            String folderName = messageData.getFolder();
            Integer uid = messageData.getUid();
            if (uid == null) {
                continue;
            }
            Set<Integer> uids = folderUidsMap.get(folderName);
            if (uids == null) {
                uids = new HashSet<>();
                folderUidsMap.put(folderName, uids);
            }
            uids.add(uid);
        }

        List<FlagChange> flagChanges = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> entry : folderUidsMap.entrySet()) {
            String folderName = entry.getKey();
            Set<Integer> uids = entry.getValue();
            flagChanges.add(new FlagChange(folderName, uids, Flag.Deleted));
        }
        return flagChanges;
    }

    /**
     * Update flags
     */
    public void updateFlags() throws ImapServiceNotAvailableException, IOException, ImapException {
        String previousFolder = null;
        BasicImapService imapService = mImapServiceController.getService();
        for (FlagChange flagChange : mFlagChanges) {
            String folder = flagChange.getFolder();
            if (!folder.equals(previousFolder)) {
                imapService.select(folder);
                previousFolder = folder;
            }
            switch (flagChange.getOperation()) {
                case ADD_FLAG:
                    imapService.addFlags(flagChange.getJoinedUids(), flagChange.getFlag());
                    break;
                case REMOVE_FLAG:
                    imapService.removeFlags(flagChange.getJoinedUids(), flagChange.getFlag());
                    break;
            }
            mSuccessFullFlagChanges.add(flagChange);
        }
    }

    /**
     * Interface used to notify listener when flags have been updated on the CMS server
     */
    public interface UpdateFlagTaskListener {
        /**
         * Callback method
         * 
         * @param flags list of changed flags
         */
        void onUpdateFlagTaskExecuted(List<FlagChange> flags);
    }

}

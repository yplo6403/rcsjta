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
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;

import com.gsma.rcs.utils.logger.Logger;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Task used to update flag status on the CMS server.
 */
public class UpdateFlagTask implements Runnable {

    private static final Logger sLogger = Logger.getLogger(UpdateFlagTask.class.getSimpleName());

    private final UpdateFlagTaskListener mListener;
    private final ImapServiceController mImapServiceController;
    private RcsSettings mSettings;
    private XmsLog mXmsLog;
    private ImapLog mImapLog;
    private List<FlagChange> mFlagChanges;
    private List<FlagChange> mSuccessFullFlagChanges = new ArrayList<>();

    /**
     * Constructor
     * @param imapServiceController
     * @param flagChanges
     * @param listener
     */
    public UpdateFlagTask(ImapServiceController imapServiceController, List<FlagChange> flagChanges,
            UpdateFlagTaskListener listener) {
        mImapServiceController = imapServiceController;
        mListener = listener;
        mFlagChanges = flagChanges;
    }

    /**
     * Constructor
     * @param imapServiceController
     * @param listener
     * @throws ImapServiceNotAvailableException
     */
    public UpdateFlagTask(ImapServiceController imapServiceController, RcsSettings settings, XmsLog xmsLog,
            ImapLog imapLog, UpdateFlagTaskListener listener) {
        mImapServiceController = imapServiceController;
        mListener = listener;
        mXmsLog = xmsLog;
        mImapLog = imapLog;
        mSettings = settings;
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
        } catch (Exception e) {

        } finally {
            mImapServiceController.closeService();
            if (mListener != null) {
                mListener.onUpdateFlagTaskExecuted(mSuccessFullFlagChanges);
            }
        }
    }

    private List<FlagChange> getReadChanges() {
        Map<String, List<Integer>> folderUidsMap = new HashMap<>();
        for (MessageData messageData : mImapLog.getMessages(ReadStatus.READ_REPORT_REQUESTED)) {
            Integer uid = messageData.getUid();
            if (uid == null) {
                continue;
            }
            String folderName = messageData.getFolder();
            List<Integer> uids = folderUidsMap.get(folderName);
            if (uids == null) {
                uids = new ArrayList<>();
                folderUidsMap.put(folderName, uids);
            }
            uids.add(uid);
        }

        List<FlagChange> flagChanges = new ArrayList<>();
        Iterator<Map.Entry<String, List<Integer>>> iterator = folderUidsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<Integer>> entry = iterator.next();
            String folderName = entry.getKey();
            List<Integer> uids = entry.getValue();
            flagChanges.add(new FlagChange(folderName, uids, Flag.Seen));
        }
        return flagChanges;
    }

    private List<FlagChange> getDeleteChanges() {
        Map<String, List<Integer>> folderUidsMap = new HashMap<>();
        for (MessageData messageData : mImapLog
                .getMessages(MessageData.DeleteStatus.DELETED_REPORT_REQUESTED)) {
            String folderName = messageData.getFolder();
            Integer uid = messageData.getUid();
            if (uid == null) {
                continue;
            }
            List<Integer> uids = folderUidsMap.get(folderName);
            if (uids == null) {
                uids = new ArrayList<>();
                folderUidsMap.put(folderName, uids);
            }
            uids.add(uid);
        }

        List<FlagChange> flagChanges = new ArrayList<>();
        Iterator<Map.Entry<String, List<Integer>>> iterator = folderUidsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<Integer>> entry = iterator.next();
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
    public void updateFlags() {

        String previousFolder = null;
        try {
            BasicImapService imapService = mImapServiceController.getService();
            for (FlagChange flagChange : mFlagChanges) {
                String folder = flagChange.getFolder();
                if (!folder.equals(previousFolder)) {
                    imapService.select(folder);
                    previousFolder = folder;
                }
                switch (flagChange.getOperation()) {
                    case ADD_FLAG:
                        imapService.addFlags(flagChange.getJoinedUids(), flagChange.getFlags());
                        break;
                    case REMOVE_FLAG:
                        imapService.removeFlags(flagChange.getJoinedUids(), flagChange.getFlags());
                        break;
                }
                mSuccessFullFlagChanges.add(flagChange);
            }
        } catch (IOException | ImapException | ImapServiceNotAvailableException e) {
            if(sLogger.isActivated()){
                sLogger.debug(e.getMessage());
                e.printStackTrace(); // FIX ME :  debug purpose
            }
        }
    }

    /**
    * Interface used to notify listener when flags have been updated on the CMS server
    */
    public interface UpdateFlagTaskListener {
        /**
         * Callback method
         * @param successFullFlagChanges
         */
        void onUpdateFlagTaskExecuted(List<FlagChange> successFullFlagChanges);
    }

}

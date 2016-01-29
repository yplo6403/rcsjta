/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.cms.sync.scheduler.task;

import com.gsma.rcs.core.cms.sync.process.FlagChange;
import com.gsma.rcs.core.cms.sync.scheduler.SchedulerTask;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventFrameworkMode;
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
public class UpdateFlagTask extends SchedulerTask {

    private static final Logger sLogger = Logger.getLogger(UpdateFlagTask.class.getSimpleName());

    private final UpdateFlagTaskListener mListener;
    private final CmsLog mCmsLog;
    private final EventFrameworkMode mXmsMode;
    private final EventFrameworkMode mChatMode;
    private final List<FlagChange> mSuccessFullFlagChanges;

    private List<FlagChange> mFlagChanges;

    /**
     * Constructor
     *
     * @param flagChanges the list of changed flags
     * @param listener the update flag listener
     */
    public UpdateFlagTask(List<FlagChange> flagChanges, UpdateFlagTaskListener listener) {
        mListener = listener;
        mCmsLog = null;
        mXmsMode = mChatMode = EventFrameworkMode.DISABLED;
        mFlagChanges = flagChanges;
        mSuccessFullFlagChanges = new ArrayList<>();
    }

    /**
     * Constructor
     *
     * @param cmsLog the IMAP log accessor
     * @param listener the update flag listener
     */
    public UpdateFlagTask(CmsLog cmsLog, EventFrameworkMode xmsMode, EventFrameworkMode chatMode,
            UpdateFlagTaskListener listener) {
        mCmsLog = cmsLog;
        mXmsMode = xmsMode;
        mChatMode = chatMode;
        mListener = listener;
        mSuccessFullFlagChanges = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            if (mFlagChanges == null) { // get from db
                mFlagChanges = new ArrayList<>();
                mFlagChanges.addAll(getReadChanges());
                mFlagChanges.addAll(getDeleteChanges());
            }
            updateFlags();

        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.info(e.getMessage());
            }
        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Runtime error while updating flag status on CMS server!", e);

        } finally {
            if (mListener != null) {
                mListener.onUpdateFlagTaskExecuted(mSuccessFullFlagChanges);
            }
        }
    }

    private List<FlagChange> getReadChanges() {
        Map<String, Set<Integer>> folderUidsMap = new HashMap<>();
        List<FlagChange> flagChanges = new ArrayList<>();
        if (EventFrameworkMode.DISABLED == mXmsMode && EventFrameworkMode.DISABLED == mChatMode) {
            return flagChanges;
        }

        List<CmsObject> cmsObjectList;
        if (EventFrameworkMode.IMAP == mXmsMode && EventFrameworkMode.IMAP == mChatMode) {
            cmsObjectList = mCmsLog.getMessages(ReadStatus.READ_REPORT_REQUESTED);
        } else if (EventFrameworkMode.IMAP == mXmsMode) {
            cmsObjectList = mCmsLog.getXmsMessages(ReadStatus.READ_REPORT_REQUESTED);
        } else {
            cmsObjectList = mCmsLog.getChatMessages(ReadStatus.READ_REPORT_REQUESTED);
        }

        for (CmsObject cmsObject : cmsObjectList) {
            Integer uid = cmsObject.getUid();
            if (uid == null) {
                continue;
            }
            String folderName = cmsObject.getFolder();
            Set<Integer> uids = folderUidsMap.get(folderName);
            if (uids == null) {
                uids = new HashSet<>();
                folderUidsMap.put(folderName, uids);
            }
            uids.add(uid);
        }

        for (Map.Entry<String, Set<Integer>> entry : folderUidsMap.entrySet()) {
            String folderName = entry.getKey();
            Set<Integer> uids = entry.getValue();
            flagChanges.add(new FlagChange(folderName, uids, Flag.Seen));
        }
        return flagChanges;
    }

    private List<FlagChange> getDeleteChanges() {
        Map<String, Set<Integer>> folderUidsMap = new HashMap<>();
        List<FlagChange> flagChanges = new ArrayList<>();
        if (EventFrameworkMode.DISABLED == mXmsMode && EventFrameworkMode.DISABLED == mChatMode) {
            return flagChanges;
        }

        List<CmsObject> cmsObjectList;
        if (EventFrameworkMode.IMAP == mXmsMode && EventFrameworkMode.IMAP == mChatMode) {
            cmsObjectList = mCmsLog.getMessages(DeleteStatus.DELETED_REPORT_REQUESTED);
        } else if (EventFrameworkMode.IMAP == mXmsMode) {
            cmsObjectList = mCmsLog.getXmsMessages(DeleteStatus.DELETED_REPORT_REQUESTED);
        } else {
            cmsObjectList = mCmsLog.getChatMessages(DeleteStatus.DELETED_REPORT_REQUESTED);
        }

        for (CmsObject cmsObject : cmsObjectList) {
            String folderName = cmsObject.getFolder();
            Integer uid = cmsObject.getUid();
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
    public void updateFlags() throws NetworkException, PayloadException {
        String previousFolder = null;
        try {
            for (FlagChange flagChange : mFlagChanges) {
                String folder = flagChange.getFolder();
                if (!folder.equals(previousFolder)) {
                    getBasicImapService().select(folder);
                    previousFolder = folder;
                }
                switch (flagChange.getOperation()) {
                    case ADD_FLAG:
                        getBasicImapService().addFlags(flagChange.getJoinedUids(),
                                flagChange.getFlag());
                        break;
                    case REMOVE_FLAG:
                        getBasicImapService().removeFlags(flagChange.getJoinedUids(),
                                flagChange.getFlag());
                        break;
                }
                mSuccessFullFlagChanges.add(flagChange);
            }
        } catch (IOException e) {
            throw new NetworkException("Failed to update flags!", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to update flags!", e);

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

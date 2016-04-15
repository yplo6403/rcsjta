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

package com.gsma.rcs.core.cms.integration;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncSchedulerTask;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.utils.logger.Logger;

import android.text.TextUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Task used to update flag status on the CMS server.
 */
public class CmsSyncUpdateFlagTask extends CmsSyncSchedulerTask {

    private static final Logger sLogger = Logger.getLogger(CmsSyncUpdateFlagTask.class
            .getSimpleName());

    private final List<CmsObject> mCmsObjects;
    private final CmsLog mCmsLog;
    private final String mRemoteFolder;

    private Set<Integer> mReadRequestedUids;
    private Set<Integer> mDeletedRequestedUids;

    /**
     * Constructor
     *
     * @param remoteFolder the CMS folder
     * @param cmsObjects the CmsObjects which should be updated on the message store
     * @param cmsLog the CmsLog accesssor
     */
    public CmsSyncUpdateFlagTask(String remoteFolder, List<CmsObject> cmsObjects, CmsLog cmsLog) {
        mRemoteFolder = remoteFolder;
        mCmsObjects = cmsObjects;
        mCmsLog = cmsLog;
    }

    @Override
    public void execute(BasicImapService basicImapService) throws NetworkException,
            PayloadException, FileAccessException {
        updateFlags(basicImapService, mRemoteFolder, mCmsObjects);
    }

    /**
     * Update flags fr a remote folder
     */
    public void updateFlags(BasicImapService basicImapService, String remoteFolder,
            List<CmsObject> cmsObjects) throws NetworkException, PayloadException {

        mReadRequestedUids = new HashSet<>();
        mDeletedRequestedUids = new HashSet<>();

        try {
            basicImapService.select(remoteFolder);
            for (CmsObject cmsObject : cmsObjects) {
                Integer uid = cmsObject.getUid();
                if (uid == null) { // search uid on CMS server
                    MessageType messageType = cmsObject.getMessageType();
                    switch (messageType) {
                        case CHAT_MESSAGE:
                        case FILE_TRANSFER:
                            uid = basicImapService.searchUidWithHeader(
                                    Constants.HEADER_IMDN_MESSAGE_ID, cmsObject.getMessageId());
                            if (uid != null) {
                                cmsObject.setUid(uid);
                                mCmsLog.updateUid(messageType,
                                        cmsObject.getMessageId(), uid);
                            }
                            break;
                        case SMS:
                            // TODO FGI
                            break;
                        case MMS:
                            // TODO FGI
                            break;
                    }
                }

                if (uid == null) { // we are not able to update flags without UID
                    continue;
                }

                if (ReadStatus.READ_REPORT_REQUESTED == cmsObject.getReadStatus()) {
                    mReadRequestedUids.add(uid);
                }
                if (DeleteStatus.DELETED_REPORT_REQUESTED == cmsObject.getDeleteStatus()) {
                    mDeletedRequestedUids.add(uid);
                }
            }

            if (!mReadRequestedUids.isEmpty()) {
                basicImapService.addFlags(TextUtils.join(",", mReadRequestedUids), Flag.Seen);
            }

            if (!mDeletedRequestedUids.isEmpty()) {
                basicImapService.addFlags(TextUtils.join(",", mDeletedRequestedUids), Flag.Deleted);
            }
        } catch (IOException e) {
            throw new NetworkException("Failed to update flags!", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to update flags!", e);
        }
    }

    public Set<Integer> getReadRequestedUids() {
        return mReadRequestedUids;
    }

    public Set<Integer> getDeletedRequestedUids() {
        return mDeletedRequestedUids;
    }
}

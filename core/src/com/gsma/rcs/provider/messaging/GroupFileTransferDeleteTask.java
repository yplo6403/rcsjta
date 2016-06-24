/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.logger.Logger;

import java.util.Set;

public class GroupFileTransferDeleteTask extends DeleteTask.GroupedByChatId {

    private static final Logger sLogger = Logger.getLogger(GroupFileTransferDeleteTask.class
            .getName());

    private static final String SEL_ALL_GROUP_FILETRANSFERS = FileTransferData.KEY_CHAT_ID + "<>"
            + FileTransferData.KEY_CONTACT + " OR " + FileTransferData.KEY_CONTACT + " IS NULL";

    private static final String SEL_FILETRANSFER_BY_CHATID = FileTransferData.KEY_CHAT_ID + "=?";

    private final FileTransferServiceImpl mFileTransferService;

    private final InstantMessagingService mImService;
    private final CmsSessionController mCmsSessionCtrl;
    private final CmsLog mCmsLog;

    /**
     * Deletion of all group file transfers.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param cmsSessionController the CMS session controller
     */
    public GroupFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            CmsSessionController cmsSessionController) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CHAT_ID, false, SEL_ALL_GROUP_FILETRANSFERS);
        mFileTransferService = fileTransferService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionController;
        mCmsLog = null;
    }

    /**
     * Deletion of all file transfers that belong to the specified group chat.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id
     * @param cmsSessionCtrl the CMS session controller
     */
    public GroupFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver, String chatId,
            CmsSessionController cmsSessionCtrl) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CHAT_ID, false, SEL_FILETRANSFER_BY_CHATID, chatId);
        mFileTransferService = fileTransferService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsLog = null;
    }

    /**
     * Constructor to process a delete action for a specific file transfer.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id
     * @param transferId the transfer id
     * @param cmsSessionCtrl the CMS session controller
     */
    public GroupFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver, String chatId,
            String transferId, CmsSessionController cmsSessionCtrl) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CHAT_ID, true, null, transferId);
        mFileTransferService = fileTransferService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsLog = null;
    }

    /**
     * Constructor to process a delete event for a specific file transfer.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param transferId the transfer id
     * @param cmsLog the CMS log accessor
     */
    public GroupFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            String transferId, CmsLog cmsLog) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CHAT_ID, true, null, transferId);
        mFileTransferService = fileTransferService;
        mImService = imService;
        mCmsSessionCtrl = null;
        mCmsLog = cmsLog;
    }

    @Override
    protected void onRowDelete(String chatId, String transferId) throws PayloadException {
        FileSharingSession session = mImService.getFileSharingSession(transferId);
        if (session == null) {
            mFileTransferService.ensureThumbnailIsDeleted(transferId);
            mFileTransferService.ensureFileCopyIsDeletedIfExisting(transferId);
            mFileTransferService.removeGroupFileTransfer(transferId);
            return;

        }
        try {
            session.deleteSession();
        } catch (NetworkException e) {
            /*
             * If network is lost during a delete operation the remaining part of the delete
             * operation (delete from persistent storage) can succeed to 100% anyway since delete
             * can be executed anyway while no network connectivity is present and still succeed.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
        mFileTransferService.ensureThumbnailIsDeleted(transferId);
        mFileTransferService.ensureFileCopyIsDeletedIfExisting(transferId);
        mFileTransferService.removeGroupFileTransfer(transferId);
    }

    @Override
    protected void onCompleted(String chatId, Set<String> transferIds) {
        mFileTransferService.broadcastGroupFileTransfersDeleted(chatId, transferIds);
        if (mCmsSessionCtrl != null) {
            mCmsSessionCtrl.getFileTransferEventHandler().onDeleteGroupFileTransfer(chatId,
                    transferIds);
        } else {
            for (String delId : transferIds) {
                mCmsLog.updateRcsDeleteStatus(MessageType.FILE_TRANSFER, delId,
                        DeleteStatus.DELETED, null);
            }
        }
    }

}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.cms;

import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;

/**
 * CMS IMAP object
 */
public abstract class CmsObject {

    private final String mFolder;
    private Integer mUid;
    private ReadStatus mReadStatus;
    private DeleteStatus mDeleteStatus;
    private final PushStatus mPushStatus;
    private final MessageType mMessageType;
    private final String mMessageId;

    /**
     * Constructor
     *
     * @param folder the folder
     * @param messageId the message ID
     * @param pushStatus the push status
     * @param readStatus the read status
     * @param deleteStatus the delete status
     * @param messageType the message type
     */
    public CmsObject(String folder, String messageId, PushStatus pushStatus, ReadStatus readStatus,
            DeleteStatus deleteStatus, MessageType messageType) {
        mFolder = folder;
        mMessageId = messageId;
        mMessageType = messageType;
        mReadStatus = readStatus;
        mDeleteStatus = deleteStatus;
        mPushStatus = pushStatus;
    }

    /**
     * Constructor
     *
     * @param folder the folder
     * @param messageId the message ID
     * @param uid the cms UID
     * @param pushStatus the push status
     * @param readStatus the read status
     * @param deleteStatus the delete status
     * @param messageType the message type
     */
    public CmsObject(String folder, String messageId, Integer uid, PushStatus pushStatus,
            ReadStatus readStatus, DeleteStatus deleteStatus, MessageType messageType) {
        mFolder = folder;
        mUid = uid;
        mReadStatus = readStatus;
        mDeleteStatus = deleteStatus;
        mPushStatus = pushStatus;
        mMessageType = messageType;
        mMessageId = messageId;
    }

    /**
     * Constructor
     *
     * @param folder the folder
     * @param messageId the message ID
     * @param readStatus the read status
     * @param deleteStatus the delete status
     * @param messageType the message type
     */
    public CmsObject(String folder, String messageId, ReadStatus readStatus,
            DeleteStatus deleteStatus, MessageType messageType) {
        this(folder, messageId, PushStatus.PUSHED, readStatus, deleteStatus, messageType);
    }

    public String getFolder() {
        return mFolder;
    }

    public Integer getUid() {
        return mUid;
    }

    public void setUid(Integer uid) {
        mUid = uid;
    }

    public ReadStatus getReadStatus() {
        return mReadStatus;
    }

    public void setReadStatus(ReadStatus readStatus) {
        mReadStatus = readStatus;
    }

    public DeleteStatus getDeleteStatus() {
        return mDeleteStatus;
    }

    public MessageType getMessageType() {
        return mMessageType;
    }

    public String getMessageId() {
        return mMessageId;
    }

    public PushStatus getPushStatus() {
        return mPushStatus;
    }

    @Override
    public String toString() {
        return "msgId=" + mMessageId + "Folder=" + mFolder + ", uid=" + mUid + ", readStatus="
                + mReadStatus + ", delStatus=" + mDeleteStatus + ", pushStatus=" + mPushStatus
                + ", Type=" + mMessageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CmsObject))
            return false;
        CmsObject cmsObject = (CmsObject) o;
        if (!mFolder.equals(cmsObject.mFolder))
            return false;
        if (mUid != null ? !mUid.equals(cmsObject.mUid) : cmsObject.mUid != null)
            return false;
        if (mReadStatus != cmsObject.mReadStatus)
            return false;
        if (mDeleteStatus != cmsObject.mDeleteStatus)
            return false;
        if (mPushStatus != cmsObject.mPushStatus)
            return false;
        if (mMessageType != cmsObject.mMessageType)
            return false;
        return mMessageId.equals(cmsObject.mMessageId);
    }

    @Override
    public int hashCode() {
        int result = mFolder.hashCode();
        result = 31 * result + (mUid != null ? mUid.hashCode() : 0);
        result = 31 * result + mReadStatus.hashCode();
        result = 31 * result + mDeleteStatus.hashCode();
        result = 31 * result + mPushStatus.hashCode();
        result = 31 * result + mMessageType.hashCode();
        result = 31 * result + mMessageId.hashCode();
        return result;
    }

    public static boolean isXmsData(MessageType type) {
        switch (type) {
            case MMS:
            case SMS:
                return true;
            default:
                return false;
        }
    }
}

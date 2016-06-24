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
 * CMS IMAP XMS object
 */
public final class CmsXmsObject extends CmsObject {

    private final Long mNativeProviderId;

    /**
     * Constructor
     *
     * @param messageType the message type
     * @param folder the folder
     * @param readStatus the read status
     * @param deleteStatus the delete status
     * @param pushStatus the push status
     * @param messageId the message ID @param nativeProviderId the native provider ID
     */
    public CmsXmsObject(MessageType messageType, String folder, String messageId,
            PushStatus pushStatus, ReadStatus readStatus, DeleteStatus deleteStatus,
            Long nativeProviderId) {
        super(folder, messageId, pushStatus, readStatus, deleteStatus, messageType);
        mNativeProviderId = nativeProviderId;
    }

    /**
     * Constructor
     *
     * @param messageType the message type
     * @param folder the folder
     * @param messageId the message ID
     * @param uid the cms UID
     * @param pushStatus the push status
     * @param readStatus the read status
     * @param deleteStatus the delete status @param nativeProviderId the native provider ID
     */
    public CmsXmsObject(MessageType messageType, String folder, String messageId, Integer uid,
            PushStatus pushStatus, ReadStatus readStatus, DeleteStatus deleteStatus,
            Long nativeProviderId) {
        super(folder, messageId, uid, pushStatus, readStatus, deleteStatus, messageType);
        mNativeProviderId = nativeProviderId;
    }

    /**
     * Constructor
     *
     * @param messageType the message type
     * @param folder the folder
     * @param messageId the message ID
     * @param readStatus the read status
     * @param deleteStatus the delete status @param messageType the message type
     * @param nativeProviderId the native provider ID
     */
    public CmsXmsObject(MessageType messageType, String folder, String messageId,
            ReadStatus readStatus, DeleteStatus deleteStatus, Long nativeProviderId) {
        super(folder, messageId, readStatus, deleteStatus, messageType);
        mNativeProviderId = nativeProviderId;
    }

    public Long getNativeProviderId() {
        return mNativeProviderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CmsXmsObject))
            return false;
        if (!super.equals(o))
            return false;
        CmsXmsObject that = (CmsXmsObject) o;
        return mNativeProviderId != null ? mNativeProviderId.equals(that.mNativeProviderId)
                : that.mNativeProviderId == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mNativeProviderId != null ? mNativeProviderId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CmsXmsData{" + super.toString() + " nativeId=" + mNativeProviderId + '}';
    }
}

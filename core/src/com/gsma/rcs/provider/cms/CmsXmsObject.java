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

    private final Long mNativeId;

    /**
     * Constructor
     *
     * @param messageType the message type
     * @param folder the folder
     * @param messageId the message ID
     * @param pushStatus the push status
     * @param readStatus the read status
     * @param deleteStatus the delete status
     * @param nativeId the native ID
     */
    public CmsXmsObject(MessageType messageType, String folder, String messageId,
            PushStatus pushStatus, ReadStatus readStatus, DeleteStatus deleteStatus, Long nativeId) {
        super(folder, messageId, pushStatus, readStatus, deleteStatus, messageType);
        mNativeId = nativeId;
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
     * @param deleteStatus the delete status
     * @param nativeId the native ID
     */
    public CmsXmsObject(MessageType messageType, String folder, String messageId, Integer uid,
            PushStatus pushStatus, ReadStatus readStatus, DeleteStatus deleteStatus, Long nativeId) {
        super(folder, messageId, uid, pushStatus, readStatus, deleteStatus, messageType);
        mNativeId = nativeId;
    }

    public Long getNativeId() {
        return mNativeId;
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
        return mNativeId != null ? mNativeId.equals(that.mNativeId) : that.mNativeId == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mNativeId != null ? mNativeId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CmsXmsData{" + super.toString() + " nativeId=" + mNativeId + '}';
    }
}

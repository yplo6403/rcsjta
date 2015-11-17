/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.gsma.rcs.provider.xms.model;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

public abstract class XmsDataObject {

    private final String mMessageId;
    private final ContactId mContact;
    protected final String mBody;
    private final String mMimeType;
    private final Direction mDirection;
    private final long mTimestamp;
    private long mTimestampSent;
    private long mTimestampDelivered;
    private XmsMessage.State mState = XmsMessage.State.QUEUED;
    private XmsMessage.ReasonCode mReasonCode = XmsMessage.ReasonCode.UNSPECIFIED;
    private RcsService.ReadStatus mReadStatus = RcsService.ReadStatus.UNREAD;
    private long mNativeProviderId = -1;

    public XmsDataObject(String messageId, ContactId contact, String body, String mimeType,
            Direction dir, long timestamp) {
        mMessageId = messageId;
        mContact = contact;
        mBody = body;
        mMimeType = mimeType;
        mDirection = dir;
        mTimestamp = timestamp;
    }

    public String getMessageId() {
        return mMessageId;
    }

    public ContactId getContact() {
        return mContact;
    }

    public String getBody() {
        return mBody;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestampSent(long timestampSent) {
        mTimestampSent = timestampSent;
    }

    public long getTimestampSent() {
        return mTimestampSent;
    }

    public void setTimestampDelivered(long timestampDelivered) {
        mTimestampDelivered = timestampDelivered;
    }

    public long getTimestampDelivered() {
        return mTimestampDelivered;
    }

    public XmsMessage.State getState() {
        return mState;
    }

    public void setState(XmsMessage.State state) {
        mState = state;
    }

    public XmsMessage.ReasonCode getReasonCode() {
        return mReasonCode;
    }

    public void setReasonCode(XmsMessage.ReasonCode reasonCode) {
        mReasonCode = reasonCode;
    }

    public Long getNativeProviderId() {
        return mNativeProviderId;
    }

    public void setNativeProviderId(long nativeProviderId) {
        mNativeProviderId = nativeProviderId;
    }

    public RcsService.ReadStatus getReadStatus() {
        return mReadStatus;
    }

    public void setReadStatus(RcsService.ReadStatus readStatus) {
        mReadStatus = readStatus;
    }

    public String getCorrelator() {
        return null;
    }

}

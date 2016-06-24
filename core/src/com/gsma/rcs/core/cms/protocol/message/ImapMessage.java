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

package com.gsma.rcs.core.cms.protocol.message;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMessageNotSupportedException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.Header;

import java.util.Map;

public abstract class ImapMessage implements IImapMessage {

    protected com.gsma.rcs.imaplib.imap.ImapMessage mRawMessage;
    private HeaderPart mHeaderPart;
    private BodyPart mBodyPart;

    protected ImapMessage() {
        mHeaderPart = new HeaderPart();
    }

    protected ImapMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage) {
        this();
        mRawMessage = rawMessage;
        parseHeader();
    }

    private void parseHeader() {
        for (Map.Entry<String, Header> entry : (mRawMessage.getBody().getHeaders()).entrySet()) {
            String key = entry.getKey();
            Header header = entry.getValue();
            mHeaderPart.addHeader(key.toLowerCase(), header.getValue());
        }
    }

    public void addHeader(String name, String value) {
        mHeaderPart.addHeader(name, value);
    }

    @Override
    public String toPayload() {
        return mHeaderPart.toString() + Constants.CRLF + mBodyPart.getPayload();
    }

    protected abstract void parseBody() throws CmsSyncXmlFormatException,
            CmsSyncMissingHeaderException, CmsSyncMessageNotSupportedException,
            CmsSyncHeaderFormatException;

    @Override
    public String getHeader(String headerName) {
        return mHeaderPart.getHeaderValue(headerName);
    }

    @Override
    public BodyPart getBodyPart() {
        return mBodyPart;
    }

    @Override
    public String getFolder() {
        return mRawMessage.getFolderPath();
    }

    @Override
    public Integer getUid() {
        return mRawMessage.getUid();
    }

    @Override
    public boolean isSeen() {
        return mRawMessage.getMetadata().getFlags().contains(Flag.Seen);
    }

    @Override
    public boolean isDeleted() {
        return mRawMessage.getMetadata().getFlags().contains(Flag.Deleted);
    }

    public void setBodyPart(BodyPart bodyPart) {
        mBodyPart = bodyPart;
    }

}

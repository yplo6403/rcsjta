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
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.core.cms.protocol.message.cpim.CpimMessage;
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

public class ImapChatMessage extends ImapCpimMessage {

    private final String mChatId;

    /**
     * Constructor
     * 
     * @param rawMessage the IMAP raw message
     * @param remote the remote contact or null if group chat
     * @throws CmsSyncMissingHeaderException
     */
    public ImapChatMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage, ContactId remote)
            throws CmsSyncMissingHeaderException {
        super(rawMessage, remote);
        mChatId = getHeader(Constants.HEADER_CONTRIBUTION_ID);
        if (mChatId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID
                    + " IMAP header is missing");
        }
    }

    public String getText() {
        return ((TextCpimBody) getCpimMessage().getBody()).getContent();
    }

    public String getChatId() {
        return mChatId;
    }

    @Override
    public void parseBody() throws CmsSyncXmlFormatException, CmsSyncHeaderFormatException {
        String content = mRawMessage.getBody().getContent();
        if (TextUtils.isEmpty(content)) {
            throw new CmsSyncXmlFormatException("IMAP body is missing!");
        }
        CpimMessage cpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
        cpimMessage.parsePayload(content);
        setBodyPart(cpimMessage);
        if (mRemote == null) {
            // Group chat
            String fromHeader = cpimMessage.getHeader(Constants.HEADER_FROM);
            if (fromHeader == null) {
                throw new CmsSyncXmlFormatException("From CPIM header is missing");
            }
            ContactUtil.PhoneNumber phoneNumber = ContactUtil
                    .getValidPhoneNumberFromUri(fromHeader);
            if (phoneNumber == null) {
                throw new CmsSyncXmlFormatException("From CPIM header do not contain tel URI");
            }
            mRemote = ContactUtil.createContactIdFromValidatedData(phoneNumber);
        }
    }
}

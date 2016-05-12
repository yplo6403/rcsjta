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
import com.gsma.rcs.imaplib.imap.Header;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

public class ImapCpimMessage extends ImapMessage {

    private final ContactId mRemote;
    private Direction mDirection;

    public ImapCpimMessage(ContactId remote) {
        super();
        mRemote = remote;
    }

    /**
     * Constructor
     * 
     * @param rawMessage the IMAP raw message
     * @param remote the remote contact or null if group chat
     * @throws CmsSyncMissingHeaderException
     * @throws CmsSyncHeaderFormatException
     */
    public ImapCpimMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage, ContactId remote)
            throws CmsSyncMissingHeaderException, CmsSyncHeaderFormatException,
            CmsSyncXmlFormatException {
        super(rawMessage);
        String direction = getHeader(Constants.HEADER_DIRECTION);
        if (direction == null) {
            throw new CmsSyncMissingHeaderException("Direction IMAP header is missing");
        }
        String from = null;
        if (Constants.DIRECTION_SENT.equals(direction)) {
            mDirection = Direction.OUTGOING;
        } else {
            mDirection = Direction.INCOMING;
            from = getHeader(Constants.HEADER_FROM);
            if (from == null) {
                throw new CmsSyncMissingHeaderException("From IMAP header is missing");
            }
        }
        if (remote != null) {
            // single chat
            mRemote = remote;
        } else {
            // Group conversation
            if (Direction.INCOMING == mDirection) {
                ContactUtil.PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromUri(from);
                if (phoneNumber == null) {
                    throw new CmsSyncXmlFormatException("From IMAP header do not contain tel URI ("
                            + from + ")");
                }
                mRemote = ContactUtil.createContactIdFromValidatedData(phoneNumber);
            } else {
                mRemote = null;
            }
        }
    }

    @Override
    public void parsePayload(String payload) {
        String[] parts = payload.split(Constants.CRLFCRLF, 2);
        if (2 == parts.length) {
            for (Header header : Header.parseHeaders(parts[0]).values()) {
                addHeader(header.getKey().toLowerCase(), header.getValue());
            }
            CpimMessage cpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
            cpimMessage.parsePayload(parts[1]);
            setBodyPart(cpimMessage);
        }
    }

    public CpimMessage getCpimMessage() {
        return (CpimMessage) super.getBodyPart();
    }

    public Direction getDirection() {
        return mDirection;
    }

    public ContactId getContact() {
        return mRemote;
    }
}

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

    public ImapCpimMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage)
            throws CmsSyncMissingHeaderException, CmsSyncHeaderFormatException {
        super(rawMessage);
        String direction = getHeader(Constants.HEADER_DIRECTION);
        if (direction == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_DIRECTION
                    + " IMAP header is missing");
        }
        String remote;
        if (Constants.DIRECTION_SENT.equals(direction)) {
            mDirection = Direction.OUTGOING;
            remote = getHeader(Constants.HEADER_TO);
        } else {
            mDirection = Direction.INCOMING;
            remote = getHeader(Constants.HEADER_FROM);
        }
        if (remote == null) {
            throw new CmsSyncMissingHeaderException(
                    mDirection == Direction.OUTGOING ? Constants.HEADER_TO : Constants.HEADER_FROM
                            + " IMAP header is missing");
        }
        ContactUtil.PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromUri(remote);
        if (phoneNumber == null) {
            mRemote = null;
        } else {
            mRemote = ContactUtil.createContactIdFromValidatedData(phoneNumber);
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

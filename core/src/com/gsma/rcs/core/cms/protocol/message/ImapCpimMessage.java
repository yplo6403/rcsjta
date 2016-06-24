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
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

public class ImapCpimMessage extends ImapMessage {

    protected ContactId mRemote;
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
     */
    public ImapCpimMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage, ContactId remote)
            throws CmsSyncMissingHeaderException {
        super(rawMessage);
        mRemote = remote;
        String direction = getHeader(Constants.HEADER_DIRECTION);
        if (direction == null) {
            throw new CmsSyncMissingHeaderException("Direction IMAP header is missing");
        }
        if (Constants.DIRECTION_SENT.equals(direction)) {
            mDirection = Direction.OUTGOING;
        } else {
            mDirection = Direction.INCOMING;
        }
    }

    @Override
    public void parseBody() throws CmsSyncXmlFormatException, CmsSyncMissingHeaderException, CmsSyncHeaderFormatException {
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

    @Override
    public String toString() {
        return "ImapCpim{" + "remote=" + mRemote + ",dir=" + mDirection + '}';
    }
}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms.imap.message;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.cms.imap.message.cpim.CpimMessage;
import com.gsma.rcs.cms.imap.message.cpim.text.TextCpimBody;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.sonymobile.rcs.imap.Header;

public class ImapCpimMessage extends ImapMessage {

    private ContactId mContact;
    private Direction mDirection;

    public ImapCpimMessage(){
        super();
    }

    public ImapCpimMessage(com.sonymobile.rcs.imap.ImapMessage rawMessage) throws CmsSyncMissingHeaderException, CmsSyncHeaderFormatException {
        super(rawMessage);

        String direction = getHeader(Constants.HEADER_DIRECTION);
        if(direction == null){
            throw new CmsSyncMissingHeaderException(Constants.HEADER_DIRECTION + " IMAP header is missing");
        }

        String headerContact;
        if(Constants.DIRECTION_SENT.equals(direction)){
            mDirection = Direction.OUTGOING;
            headerContact = getHeader(Constants.HEADER_TO);
        }
        else{
            mDirection = Direction.INCOMING;
            headerContact = getHeader(Constants.HEADER_FROM);
        }

        mContact = CmsUtils.headerToContact(headerContact);
        if (mContact == null) {
            throw new CmsSyncHeaderFormatException("Bad format for header : " + headerContact);
        }
    }

    @Override
    public void parsePayload(String payload) {
        String[] parts = payload.split(Constants.CRLFCRLF,2);
        if(2 == parts.length ){
            for(Header header : Header.parseHeaders(parts[0]).values()){
                addHeader(header.getKey(), header.getValue());
            }
            CpimMessage cpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
            cpimMessage.parsePayload(parts[1]);
            setBodyPart(cpimMessage);
        }
    }

    public CpimMessage getCpimMessage(){
        return (CpimMessage)super.getBodyPart();
    }

    public ContactId getContact(){
        return mContact;
    }

    public Direction getDirection(){
        return mDirection;
    }
}

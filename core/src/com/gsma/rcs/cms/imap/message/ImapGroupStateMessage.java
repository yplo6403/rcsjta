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
import com.gsma.rcs.cms.event.exception.CmsSyncException;
import com.gsma.rcs.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.cms.imap.message.groupstate.GroupStateDocument;
import com.gsma.rcs.cms.imap.message.groupstate.GroupStateParser;
import com.gsma.rcs.core.ParseFailureException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.sonymobile.rcs.imap.Header;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class ImapGroupStateMessage extends ImapMessage {

    private final String mChatId;
    private final Direction mDirection;
    private final String mSubject;
    private final long mTimestamp;
    private final String mRejoinId;
    private final List<ContactId> mParticipants;

    public ImapGroupStateMessage(com.sonymobile.rcs.imap.ImapMessage rawMessage) throws CmsSyncException {
        super(rawMessage);

        //TODO FGI : check if direction available in IMAP header
        mDirection = Direction.OUTGOING;
        try {
            mChatId = getHeader(Constants.HEADER_CONTRIBUTION_ID);
            if(mChatId == null){
                throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID + " IMAP header is missing");
            }

            mSubject = getHeader(Constants.HEADER_SUBJECT);

            String xml = getBodyPart().toPayload();
            GroupStateParser parser = new GroupStateParser(new InputSource(new ByteArrayInputStream(xml.toString().getBytes())));
            GroupStateDocument document = parser.parse().getGroupStateDocument();
            mTimestamp = document.getDate();
            mRejoinId = document.getLastfocussessionid();
            mParticipants = document.getParticipants();

        } catch (ParserConfigurationException | SAXException | ParseFailureException e) {
            e.printStackTrace();
            throw new CmsSyncXmlFormatException(e);
        }
    }

    @Override
    public void parsePayload(String payload) {
        String[] parts = payload.split(Constants.CRLFCRLF,2);
        if(2 == parts.length ){
            for(Header header : Header.parseHeaders(parts[0]).values()){
                addHeader(header.getKey(), header.getValue());
            }
            setBodyPart(new BodyPart(parts[1]));
        }
    }

    public String getRejoinId(){
        return mRejoinId;
    }

    public String getSubject(){
        return mSubject;
    }

    public List<ContactId> getParticipants(){
        return mParticipants;
    }

    public long getTimestamp(){
        return mTimestamp;
    }

    public String getChatId(){
        return mChatId;
    }

    public Direction getDirection(){
        return mDirection;
    }
}

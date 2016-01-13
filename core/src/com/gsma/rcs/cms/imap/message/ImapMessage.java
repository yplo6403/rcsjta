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
import com.gsma.rcs.cms.imap.message.cpim.CpimMessage;
import com.gsma.rcs.cms.imap.message.cpim.text.TextCpimBody;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.Header;

public abstract class ImapMessage implements IImapMessage {

    protected com.sonymobile.rcs.imap.ImapMessage mRawMessage;
    protected HeaderPart mHeaderPart;
    protected CpimMessage mCpimMessage;

    protected ImapMessage(){
        mHeaderPart = new HeaderPart();
    }

    protected ImapMessage(com.sonymobile.rcs.imap.ImapMessage rawMessage) {
        this();
        mRawMessage = rawMessage;
        parsePayload(mRawMessage.getPayload());
    }

    public void addHeader(String name, String value){
        mHeaderPart.addHeader(name, value);
    }

    @Override
    public String toPayload(){
        StringBuilder sb = new StringBuilder();
        sb.append(mHeaderPart);
        sb.append(Constants.CRLF);
        sb.append(mCpimMessage.toPayload());
        return sb.toString();
    }

    @Override
    public void parsePayload(String payload) {
        {
            String[] parts = payload.split(Constants.CRLFCRLF,2);
            if(2 == parts.length ){
                for(Header header : Header.parseHeaders(parts[0]).values()){
                    addHeader(header.getKey(), header.getValue());
                }
                mCpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
                mCpimMessage.parsePayload(parts[1]);
            }
        }
    }

    @Override
    public String getHeader(String headerName){
        return mHeaderPart.getHeaderValue(headerName);
    }

    @Override
    public CpimMessage getCpimMessage(){
        return mCpimMessage;
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

}

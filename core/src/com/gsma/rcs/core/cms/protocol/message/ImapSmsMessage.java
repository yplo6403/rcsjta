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
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.core.cms.utils.HeaderCorrelatorUtils;
import com.gsma.services.rcs.contact.ContactId;

public class ImapSmsMessage extends ImapCpimMessage {

    private String mCorrelator;
    private long mDate;

    public ImapSmsMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage, ContactId remote)
            throws CmsSyncMissingHeaderException, CmsSyncHeaderFormatException,
            CmsSyncXmlFormatException {
        super(rawMessage, remote);
        mCorrelator = getHeader(Constants.HEADER_MESSAGE_CORRELATOR);
        if (mCorrelator == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_MESSAGE_CORRELATOR
                    + " IMAP header is missing");
        }
        String dateHeader = getHeader(Constants.HEADER_DATE);
        if (dateHeader == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_DATE
                    + " IMAP header is missing");
        }
        mDate = DateUtils.parseDate(dateHeader, DateUtils.CMS_IMAP_DATE_FORMAT);
    }

    public ImapSmsMessage(ContactId remote, String from, String to, String direction, long date,
            String content, String conversationId, String contributionId, String imdnMessageId) {
        super(remote);
        addHeader(Constants.HEADER_FROM, from);
        addHeader(Constants.HEADER_TO, to);
        addHeader(Constants.HEADER_DATE,
                DateUtils.getDateAsString(date, DateUtils.CMS_IMAP_DATE_FORMAT));
        addHeader(Constants.HEADER_CONVERSATION_ID, conversationId);
        addHeader(Constants.HEADER_CONTRIBUTION_ID, contributionId);
        addHeader(Constants.HEADER_IMDN_MESSAGE_ID, imdnMessageId);
        addHeader(Constants.HEADER_DIRECTION, direction);
        addHeader(Constants.HEADER_MESSAGE_CORRELATOR, HeaderCorrelatorUtils.buildHeader(content));
        addHeader(Constants.HEADER_MESSAGE_CONTEXT, Constants.PAGER_MESSAGE);
        addHeader(Constants.HEADER_CONTENT_TYPE, Constants.MESSAGE_CPIM);

        HeaderPart cpimHeaders = new HeaderPart();
        cpimHeaders.addHeader(Constants.HEADER_FROM, from);
        cpimHeaders.addHeader(Constants.HEADER_TO, to);
        cpimHeaders.addHeader("NS", "imdn <urn:ietf:params:imdn>");
        cpimHeaders.addHeader("NS", "rcs <http://www.gsma.com>");
        cpimHeaders.addHeader("imdn.Message-ID", imdnMessageId);
        cpimHeaders.addHeader(Constants.HEADER_DATE_TIME,
                DateUtils.getDateAsString(date, DateUtils.CMS_CPIM_DATE_FORMAT));

        TextCpimBody textCpimBody = new TextCpimBody("text/plain; charset=utf-8", content);
        CpimMessage cpimMessage = new CpimMessage(cpimHeaders, textCpimBody);
        setBodyPart(cpimMessage);
    }

    public String getCorrelator() {
        return mCorrelator;
    }

    public long getDate() {
        return mDate;
    }

    @Override
    public String toString() {
        return "ImapSmsMessage{remote=" + getContact() + ",uid=" + getUid() + ",dir="
                + getDirection() + ",correlator='" + mCorrelator + '\'' + ",date=" + mDate + '}';
    }
}

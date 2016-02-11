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

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncImdnFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class ImapImdnMessage extends ImapCpimMessage {

    final static String ANONYMOUS = "<sip:anonymous@anonymous.invalid>";

    private final boolean isOneToOne;
    private String mImdnId;
    private ImdnDocument mImdnDocument;

    public ImapImdnMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage)
            throws CmsSyncMissingHeaderException, CmsSyncHeaderFormatException,
            CmsSyncImdnFormatException {
        super(rawMessage);

        mImdnId = getHeader(Constants.HEADER_IMDN_MESSAGE_ID);
        if (mImdnId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_IMDN_MESSAGE_ID
                    + " IMAP header is missing");
        }

        String from = getCpimMessage().getHeader(Constants.HEADER_FROM);
        if (from == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_FROM
                    + " IMAP header is missing");
        }
        isOneToOne = ANONYMOUS.equals(from);

        String xml = getBodyPart().getPayload();
        if (!xml.isEmpty()) {
            try {
                mImdnDocument = ChatUtils.parseCpimDeliveryReport(xml);
            } catch (SAXException | ParserConfigurationException | ParseFailureException e) {
                throw new CmsSyncImdnFormatException(e);
            }
        }
    }

    public ImdnDocument getImdnDocument() {
        return mImdnDocument;
    }

    public String getImdnId() {
        return mImdnId;
    }

    public boolean isOneToOne() {
        return isOneToOne;
    }

    /**
     * For OneToOne Imdn, get contact from IMAP headers For GC Imdn, get contact from CPIM headers.
     * 
     * @return
     */
    public ContactId getContact() {
        if (isOneToOne) {
            return super.getContact();
        }

        String from = getCpimMessage().getHeader(Constants.HEADER_FROM);
        if (from.startsWith("<") && from.endsWith(">")) {
            from = from.substring(1, from.length() - 1);
        }
        if (from.contains("?")) {
            from = from.substring(0, from.indexOf("?"));
        }
        return CmsUtils.headerToContact(from);
    }
}

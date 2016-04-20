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
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class ImapImdnMessage extends ImapCpimMessage {

    final static String ANONYMOUS = "<sip:anonymous@anonymous.invalid>";

    private final boolean mOneToOne;
    private ContactId mRemote;
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
        mOneToOne = ANONYMOUS.equals(from);
        if (mOneToOne) {
            // For OneToOne, get contact from IMAP headers.
            mRemote = super.getContact();

        } else if (RcsService.Direction.OUTGOING == getDirection()) {
            mRemote = null;

        } else {
            // For GC, get contact from CPIM headers.
            ContactUtil.PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromUri(from);
            if (phoneNumber == null) {
                throw new CmsSyncMissingHeaderException("From header is invalid (" + from + ")");
            }
            mRemote = ContactUtil.createContactIdFromValidatedData(phoneNumber);
        }
        String cpim = getBodyPart().getPayload();
        if (!cpim.isEmpty()) {
            try {
                mImdnDocument = ChatUtils.parseCpimDeliveryReport(cpim);
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
        return mOneToOne;
    }

    public ContactId getContact() {
        return mRemote;
    }
}

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
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class ImapImdnMessage extends ImapCpimMessage {

    private String mImdnId;
    private ImdnDocument mImdnDocument;

    /**
     * Constructor
     * 
     * @param rawMessage the IMAP raw message
     * @param remote the remote contact or null if group chat
     * @throws CmsSyncMissingHeaderException
     * @throws CmsSyncHeaderFormatException
     * @throws CmsSyncImdnFormatException
     */
    public ImapImdnMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage, ContactId remote)
            throws CmsSyncMissingHeaderException, CmsSyncHeaderFormatException,
            CmsSyncImdnFormatException, CmsSyncXmlFormatException {
        super(rawMessage, remote);
        mImdnId = getHeader(Constants.HEADER_IMDN_MESSAGE_ID);
        if (mImdnId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_IMDN_MESSAGE_ID
                    + " IMAP header is missing");
        }
        String cpim = getBodyPart().getPayload();
        if (TextUtils.isEmpty(cpim)) {
            /* CPIM messages are always instantiated with the body */
            throw new CmsSyncXmlFormatException("IMDN message has not body content! (ftId="
                    + mImdnId + ")");
        }
        // The payload is peeked
        try {
            mImdnDocument = ChatUtils.parseCpimDeliveryReport(cpim);

        } catch (SAXException | ParserConfigurationException | ParseFailureException e) {
            throw new CmsSyncImdnFormatException(e);
        }
    }

    public ImdnDocument getImdnDocument() {
        return mImdnDocument;
    }

    public String getImdnId() {
        return mImdnId;
    }

}

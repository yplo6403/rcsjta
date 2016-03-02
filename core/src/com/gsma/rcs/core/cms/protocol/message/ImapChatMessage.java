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
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimIdentity;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

public class ImapChatMessage extends ImapCpimMessage {

    final static String ANONYMOUS = "<sip:anonymous@anonymous.invalid>";

    private final boolean mIsOneToOne;
    private final String mChatId;
    private final ContactId mContact;

    public ImapChatMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage)
            throws CmsSyncMissingHeaderException, CmsSyncHeaderFormatException {
        super(rawMessage);

        mChatId = getHeader(Constants.HEADER_CONTRIBUTION_ID);
        if (mChatId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID
                    + " IMAP header is missing");
        }

        String from = getCpimMessage().getHeader(Constants.HEADER_FROM);
        if (from == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_FROM
                    + " IMAP header is missing");
        }
        mIsOneToOne = ANONYMOUS.equals(from);

        if(mIsOneToOne) {
            mContact = super.getContact();
        } else if( Direction.OUTGOING == getDirection()) {
            mContact = null;
        } else{ // For incoming GC msg, retrieve contact from the "from" CPIM header
            String uri = new CpimIdentity(getCpimMessage().getHeader(Constants.HEADER_FROM)).getUri();
            PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromUri(uri);
            mContact = (phoneNumber != null) ? ContactUtil.createContactIdFromValidatedData(phoneNumber) : null;
        }
    }

    public String getText() {
        return ((TextCpimBody) getCpimMessage().getBody()).getContent();
    }

    public boolean isOneToOne() {
        return mIsOneToOne;
    }

    public String getChatId() {
        return mChatId;
    }

    public ContactId getContact() {
        return mContact;
    }
}

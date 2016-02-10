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

public class ImapChatMessage extends ImapCpimMessage {

    final static String ANONYMOUS = "<sip:anonymous@anonymous.invalid>";

    private final boolean isOneToOne;
    private final String mChatId;

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
        isOneToOne = ANONYMOUS.equals(from);
    }

    public String getText() {
        return ((TextCpimBody) getCpimMessage().getBody()).getContent();
    }

    public boolean isOneToOne() {
        return isOneToOne;
    }

    public String getChatId() {
        return mChatId;
    }
}

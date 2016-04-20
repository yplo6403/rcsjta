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
import com.gsma.rcs.core.cms.event.exception.CmsSyncImdnFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.protocol.message.cpim.CpimMessage;
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

public class ImapFileTransferMessage extends ImapCpimMessage {

    private final boolean isOneToOne;
    private final ContactId mRemote;
    private final String mChatId;
    private String mImdnId;
    private FileTransferHttpInfoDocument mFileTransferHttpInfoDocument;

    public ImapFileTransferMessage(RcsSettings rcsSettings,
            com.gsma.rcs.imaplib.imap.ImapMessage rawMessage) throws CmsSyncMissingHeaderException,
            CmsSyncHeaderFormatException, CmsSyncImdnFormatException {
        super(rawMessage);
        mChatId = getHeader(Constants.HEADER_CONTRIBUTION_ID);
        if (mChatId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID
                    + " IMAP header is missing");
        }
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
        isOneToOne = ImapChatMessage.ANONYMOUS.equals(from);
        if (isOneToOne) {
            // For OneToOne, get contact from IMAP headers.
            mRemote = super.getContact();

        } else if (RcsService.Direction.OUTGOING == getDirection()) {
            mRemote = null;

        } else {
            // For GC, get contact from CPIM headers.
            ContactUtil.PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromUri(from);
            mRemote = ContactUtil.createContactIdFromValidatedData(phoneNumber);
        }
        CpimMessage cpim = (CpimMessage) getBodyPart();
        if (!cpim.getPayload().isEmpty()) {
            try {
                TextCpimBody cpimBody = (TextCpimBody) cpim.getBody();
                mFileTransferHttpInfoDocument = FileTransferUtils.parseFileTransferHttpDocument(
                        cpimBody.getContent().getBytes(), rcsSettings);
            } catch (PayloadException e) {
                throw new CmsSyncImdnFormatException(e);
            }
        }
    }

    public FileTransferHttpInfoDocument getFileTransferHttpInfoDocument() {
        return mFileTransferHttpInfoDocument;
    }

    public String getImdnId() {
        return mImdnId;
    }

    public boolean isOneToOne() {
        return isOneToOne;
    }

    public String getChatId() {
        return mChatId;
    }

    public ContactId getContact() {
        return mRemote;
    }
}

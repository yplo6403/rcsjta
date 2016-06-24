/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.protocol.message;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.core.cms.protocol.message.cpim.CpimMessage;
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

public class ImapFileTransferMessage extends ImapCpimMessage {

    private final String mChatId;
    private final RcsSettings mRcsSettings;
    private String mImdnId;
    private FileTransferHttpInfoDocument mInfoDocument;

    public ImapFileTransferMessage(RcsSettings rcsSettings,
            com.gsma.rcs.imaplib.imap.ImapMessage rawMessage, ContactId remote)
            throws CmsSyncMissingHeaderException {
        super(rawMessage, remote);
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
        mRcsSettings = rcsSettings;
    }

    public FileTransferHttpInfoDocument getInfoDocument() {
        return mInfoDocument;
    }

    public String getImdnId() {
        return mImdnId;
    }

    public String getChatId() {
        return mChatId;
    }

    @Override
    public void parseBody() throws CmsSyncXmlFormatException, CmsSyncHeaderFormatException {
        String content = mRawMessage.getBody().getContent();
        if (TextUtils.isEmpty(content)) {
            throw new CmsSyncXmlFormatException("FToHTTP message has not body content! (chatId="
                    + mChatId + ")(ftId=" + mImdnId + ")");
        }
        CpimMessage cpimMessage = new CpimMessage(new HeaderPart(), new TextCpimBody());
        cpimMessage.parsePayload(content);
        setBodyPart(cpimMessage);
        if (mRemote == null) {
            // Group chat
            String fromHeader = cpimMessage.getHeader(Constants.HEADER_FROM);
            if (fromHeader == null) {
                throw new CmsSyncXmlFormatException("From CPIM header is missing");
            }
            ContactUtil.PhoneNumber phoneNumber = ContactUtil
                    .getValidPhoneNumberFromUri(fromHeader);
            if (phoneNumber == null) {
                throw new CmsSyncXmlFormatException("From CPIM header do not contain tel URI");
            }
            mRemote = ContactUtil.createContactIdFromValidatedData(phoneNumber);
        }
        TextCpimBody cpimBody = (TextCpimBody) cpimMessage.getBody();
        content = cpimBody.getContent();
        try {
            mInfoDocument = FileTransferUtils.parseFileTransferHttpDocument(content.getBytes(),
                    mRcsSettings);

        } catch (PayloadException e) {
            throw new CmsSyncXmlFormatException(e);
        }
    }

}

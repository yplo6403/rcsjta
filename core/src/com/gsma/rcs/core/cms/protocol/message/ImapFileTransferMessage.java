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
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

public class ImapFileTransferMessage extends ImapCpimMessage {

    private final String mChatId;
    private String mImdnId;
    private FileTransferHttpInfoDocument mInfoDocument;

    public ImapFileTransferMessage(RcsSettings rcsSettings,
            com.gsma.rcs.imaplib.imap.ImapMessage rawMessage, ContactId remote)
            throws CmsSyncMissingHeaderException, CmsSyncHeaderFormatException,
            CmsSyncXmlFormatException {
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
        CpimMessage cpim = (CpimMessage) getBodyPart();
        TextCpimBody cpimBody = (TextCpimBody) cpim.getBody();
        String content = cpimBody.getContent();
        if (TextUtils.isEmpty(content)) {
            /* CPIM messages are always instantiated with the body */
            throw new CmsSyncXmlFormatException("FToHTTP message has not body content! (chatId="
                    + mChatId + ")(ftId=" + mImdnId + ")");
        }
        // The body is peeked
        try {
            mInfoDocument = FileTransferUtils.parseFileTransferHttpDocument(content.getBytes(),
                    rcsSettings);

        } catch (PayloadException e) {
            throw new CmsSyncXmlFormatException(e);
        }
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

}

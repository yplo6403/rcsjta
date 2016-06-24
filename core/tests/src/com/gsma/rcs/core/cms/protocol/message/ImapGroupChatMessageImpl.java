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

import com.gsma.rcs.core.cms.integration.ImapCmsUtilTest;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Formatter;
import java.util.Locale;

/**
 * @author Philippe LEMORDANT.
 */
public class ImapGroupChatMessageImpl implements ImapCmsUtilTest.IImapRcsMessage {

    private static final String CONFERENCE_ID = "sip:Conference-Factory@volteofr.com";
    private final String mChatId;
    private final RcsService.Direction mDirection;
    private final long mTimestamp;
    private final String mMessageId;
    private final ContactId mRemote;
    private final ContactId mLocal;
    private final String mFolder;
    private final String mSubject;
    private final ContactId mOriginator;
    private boolean mSeen;
    private final boolean mDeleted;
    private final String mContent;
    private Integer mUid;

    // @formatter:off

    private static final String GROUP_CHAT_MESSAGE = "Date: %1$s\n" +
            "From: %2$s\n" +
            "To: %3$s\n" +
            "Message-ID: <28087089.11302.1465906199332@RCS5frontox1>\n" +
            "Subject: %4$s\n" +
            "MIME-Version: 1.0\n" +
            "Content-Type: message/cpim\n" +
            "Content-Transfer-Encoding: 8bit\n" +
            "Conversation-ID: %5$s\n" +
            "Contribution-ID: %5$s\n" +
            "IMDN-Message-ID: %6$s\n" +
            "Message-Direction: %7$s\n" +
            "\n" +
            "From: %8$s\n" +
            "To: <sip:anonymous@anonymous.invalid>\n" +
            "NS: imdn <urn:ietf:params:imdn>\n" +
            "DateTime: %9$s\n" +
            "imdn.Message-ID: %6$s\n" +
            "imdn.Disposition-Notification: positive-delivery, display\n" +
            "\n" +
            "Content-type: text/plain;charset=UTF-8\n" +
            "Content-length: %10$s\n" +
            "\n" +
            "%11$s";

    // @formatter:on

    public ImapGroupChatMessageImpl(String chatId, ContactId originator, String subject,
            RcsService.Direction dir, long timestamp, String content, boolean seen, boolean deleted) {
        mDirection = dir;
        mTimestamp = timestamp;
        mOriginator = originator;
        mSubject = subject;
        mMessageId = IdGenerator.generateMessageID();
        mContent = content;
        mSeen = seen;
        mDeleted = deleted;
        if (RcsService.Direction.INCOMING == mDirection) {
            mRemote = mOriginator;
            mLocal = null;
        } else {
            mRemote = null;
            mLocal = mOriginator;
        }
        mChatId = chatId;
        mFolder = CmsUtils.groupChatToCmsFolder(mChatId, mChatId);
    }

    @Override
    public String toPayload() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        String imapDate = DateUtils.getDateAsString(System.currentTimeMillis(),
                DateUtils.CMS_IMAP_DATE_FORMAT);
        String cpimDate = DateUtils.getDateAsString(mTimestamp, DateUtils.CMS_CPIM_DATE_FORMAT);
        formatter.format(GROUP_CHAT_MESSAGE, imapDate, getImapFrom(), getImapTo(), mSubject,
                mChatId, mMessageId, ImapCmsUtilTest.convertDirToImap(mDirection), getCpimFrom(),
                cpimDate, mContent.length() + 2, mContent);
        return formatter.toString();
    }

    @Override
    public String getFolder() {
        return mFolder;
    }

    @Override
    public Integer getUid() {
        return mUid;
    }

    private String getImapFrom() {
        return PhoneUtils.formatContactIdToUri(mOriginator).toString();
    }

    private String getImapTo() {
        if (RcsService.Direction.INCOMING == mDirection) {
            return PhoneUtils.formatContactIdToUri(mRemote).toString();
        }
        return CONFERENCE_ID;
    }

    private String getCpimFrom() {
        return PhoneUtils.formatContactIdToUri(mOriginator).toString();
    }

    @Override
    public boolean isSeen() {
        return mSeen;
    }

    @Override
    public boolean isDeleted() {
        return mDeleted;
    }

    @Override
    public String getHeader(String headerName) {
        return null;
    }

    @Override
    public BodyPart getBodyPart() {
        return null;
    }

    @Override
    public String getChatId() {
        return mChatId;
    }

    @Override
    public String getMessageId() {
        return mMessageId;
    }

    @Override
    public void setUid(Integer uid) {
        mUid = uid;
    }

    @Override
    public ContactId getRemote() {
        return mRemote;
    }

    @Override
    public void markAsSeen() {
        mSeen = true;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    public String getContent() {
        return mContent;
    }

    public String getMimeType() {
        return "text/plain";
    }

    public ContactId getLocalContact() {
        return mLocal;
    }
}

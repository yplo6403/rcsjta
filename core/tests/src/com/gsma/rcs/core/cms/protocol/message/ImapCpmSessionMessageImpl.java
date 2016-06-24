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

import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

/**
 * @author Philippe LEMORDANT.
 */
public class ImapCpmSessionMessageImpl implements ImapCmsUtilTest.IImapRcsMessage {

    private final String mChatId;
    private final RcsService.Direction mDirection;
    private final long mTimestamp;
    private final String mSubject;
    private final ContactId mFromContact;
    private final String mMessageId;
    private final boolean mSeen;
    private final boolean mDeleted;
    private final String mFolder;
    private List<ContactId> mParticipants;
    private Integer mUid;

    // @formatter:off

    private static final String CPM_SESSION_MESSAGE = "Date: %1$s\n" +
            "From: tel:%2$s\n" +
            "To: sip:Conference-Factory@volteofr.com\n" +
            "Message-ID: <902489143.11297.1465906189564@RCS5frontox1>\n" +
            "Subject: %3$s\n" +
            "MIME-Version: 1.0\n" +
            "Content-Type: Application/X-CPM-Session\n" +
            "Content-Transfer-Encoding: 8bit\n" +
            "Conversation-ID: %4$s\n" +
            "Contribution-ID: %4$s\n" +
            "IMDN-Message-ID: %5$s\n" +
            "Message-Direction: %6$s\n" +
            "\n" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<session><session-type>Group</session-type><sdp>v=0\n" +
            "o=- 3674894989 3674894989 IN IP4 sip.imsnsn.fr\n" +
            "s=-\n" +
            "c=IN IP4 172.20.166.68\n" +
            "t=0 0\n" +
            "m=message 36152 TCP/MSRP *\n" +
            "a=accept-types:message/cpim\n" +
            "a=accept-wrapped-types:text/plain application/im-iscomposing+xml message/imdn+xml application/vnd.gsma.rcs-ft-http+xml\n" +
            "a=setup:actpass\n" +
            "a=path:msrp://172.20.166.68:36152/1465906189109;tcp\n" +
            "a=sendrecv\n" +
            "</sdp><invited-participants>%7$s</invited-participants></session>";

    // @formatter:on

    public ImapCpmSessionMessageImpl(String chatId, ContactId fromContact,
            RcsService.Direction dir, long timestamp, String subject, List<ContactId> participants,
            boolean seen, boolean deleted) {
        mChatId = chatId;
        mDirection = dir;
        mTimestamp = timestamp;
        mSubject = subject;
        mParticipants = participants;
        mFromContact = fromContact;
        mMessageId = IdGenerator.generateMessageID();
        mSeen = seen;
        mDeleted = deleted;
        mFolder = CmsUtils.groupChatToCmsFolder(mChatId, mChatId);
    }

    @Override
    public String getFolder() {
        return mFolder;
    }

    @Override
    public Integer getUid() {
        return mUid;
    }

    @Override
    public String toPayload() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        List<Uri> telUris = new ArrayList<>();
        for (ContactId contact : mParticipants) {
            telUris.add(PhoneUtils.formatContactIdToUri(contact));
        }
        String participants = TextUtils.join(";", telUris);
        String date = DateUtils.getDateAsString(mTimestamp, DateUtils.CMS_IMAP_DATE_FORMAT);
        formatter.format(CPM_SESSION_MESSAGE, date, mFromContact.toString(), mSubject, mChatId,
                mMessageId, ImapCmsUtilTest.convertDirToImap(mDirection), participants);
        return formatter.toString();
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
    public String getMessageId() {
        return mMessageId;
    }

    @Override
    public ContactId getRemote() {
        return null;
    }

    @Override
    public String getChatId() {
        return mChatId;
    }

    @Override
    public void setUid(Integer uid) {
        mUid = uid;
    }

    @Override
    public void markAsSeen() {
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    public String getSubject() {
        return mSubject;
    }

    public List<ContactId> getParticipants() {
        return mParticipants;
    }

}

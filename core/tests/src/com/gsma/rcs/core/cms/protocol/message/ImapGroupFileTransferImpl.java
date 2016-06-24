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
public class ImapGroupFileTransferImpl implements ImapCmsUtilTest.IImapRcsMessage {

    private static final String CONFERENCE_ID = "sip:Conference-Factory@volteofr.com";

    private final String mChatId;
    private final RcsService.Direction mDirection;
    private final long mTimestamp;
    private final String mMessageId;
    private final ContactId mRemote;
    private final boolean mSeen;
    private final boolean mDeleted;
    private final String mFolder;
    private final ImapOneToOneFileTransferImpl.FileTransferDescriptorTest mIconDescriptor;
    private final ContactId mLocal;
    private final ContactId mOriginator;
    private final String mSubject;
    private Integer mUid;
    private final ImapOneToOneFileTransferImpl.FileTransferDescriptorTest mFileDesc;

    // @formatter:off

    public static final String FILE_TRANSFER_CORE_XML =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<file>" +
                        "<file-info type=\"file\">" +
                            "<file-size>%1$d</file-size>" +
                            "<file-name>%2$s</file-name>" +
                            "<content-type>image/jpeg</content-type>" +
                            "<data url = \"%3$s\" until=\"%4$s\"/>" +
                        "</file-info>" +
                    "</file>\n";

    public static final String FILE_TRANSFER_ICON_XML =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<file>" +
                        "<file-info type=\"thumbnail\">\n" +
                            "<file-size>%1$d</file-size>\n" +
                            "<content-type>image/jpeg</content-type>\n" +
                            "<data url = \"%2$s\" until=\"%3$s\"/>\n" +
                        "</file-info>\n" +
                        "<file-info type=\"file\">" +
                            "<file-size>%4$d</file-size>" +
                            "<file-name>%5$s</file-name>" +
                            "<content-type>image/jpeg</content-type>" +
                            "<data url = \"%6$s\" until=\"%7$s\"/>" +
                        "</file-info>" +
                    "</file>\n";


    private static final String GROUP_FILE_TRANSFER_MSG = "Date: %1$s\n" +
            "From: %2$s\n" +
            "To: %3$s\n" +
            "Message-ID: <478888650.7437.1457459294956@RCS5frontox1>\n" +
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
            "imdn.Message-ID: %6$s\n" +
            "DateTime: %9$s\n" +
            "imdn.Disposition-Notification: positive-delivery\n" +
            "\n" +
            "Content-type: application/vnd.gsma.rcs-ft-http+xml;charset=utf-8\n" +
            "Content-length: %10$d\n" +
            "\n" +
            "%11$s";

    // @formatter:on

    public ImapGroupFileTransferImpl(String chatId, ContactId originator, String subject,
            RcsService.Direction dir, long timestamp,
            ImapOneToOneFileTransferImpl.FileTransferDescriptorTest fileDescriptor,
            ImapOneToOneFileTransferImpl.FileTransferDescriptorTest iconDescriptor, boolean seen,
            boolean deleted) {
        mDirection = dir;
        mTimestamp = timestamp;
        mOriginator = originator;
        mSubject = subject;
        mChatId = chatId;
        mMessageId = IdGenerator.generateMessageID();
        mSeen = seen;
        mDeleted = deleted;
        if (RcsService.Direction.INCOMING == mDirection) {
            mRemote = mOriginator;
            mLocal = null;
        } else {
            mRemote = null;
            mLocal = mOriginator;
        }
        mFolder = CmsUtils.groupChatToCmsFolder(mChatId, mChatId);
        mFileDesc = fileDescriptor;
        mIconDescriptor = iconDescriptor;
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
        String xmlContent;
        if (mIconDescriptor != null) {
            String untilIconDate = DateUtils.getDateAsString(mIconDescriptor.mTimestampUntil,
                    DateUtils.CMS_CPIM_DATE_FORMAT);
            String untilDate = DateUtils.getDateAsString(mFileDesc.mTimestampUntil,
                    DateUtils.CMS_CPIM_DATE_FORMAT);
            formatter.format(FILE_TRANSFER_ICON_XML, mIconDescriptor.mSize, mIconDescriptor.mUrl,
                    untilIconDate, mFileDesc.mSize, mFileDesc.mFilename, mFileDesc.mUrl, untilDate);
            xmlContent = formatter.toString();
        } else {
            String untilDate = DateUtils.getDateAsString(mFileDesc.mTimestampUntil,
                    DateUtils.CMS_CPIM_DATE_FORMAT);
            formatter.format(FILE_TRANSFER_CORE_XML, mFileDesc.mSize, mFileDesc.mFilename,
                    mFileDesc.mUrl, untilDate);
            xmlContent = formatter.toString();
        }
        String imapDate = DateUtils.getDateAsString(mTimestamp, DateUtils.CMS_IMAP_DATE_FORMAT);
        String cpimDate = DateUtils.getDateAsString(mTimestamp, DateUtils.CMS_CPIM_DATE_FORMAT);
        sb = new StringBuilder();
        formatter = new Formatter(sb, Locale.US);
        formatter.format(GROUP_FILE_TRANSFER_MSG, imapDate, getImapFrom(), getImapTo(), mSubject,
                mChatId, mMessageId, ImapCmsUtilTest.convertDirToImap(mDirection), getCpimFrom(),
                cpimDate, xmlContent.length() + 2, xmlContent);
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
        return mRemote;
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

    public ImapOneToOneFileTransferImpl.FileTransferDescriptorTest getFileDesc() {
        return mFileDesc;
    }

    public ImapOneToOneFileTransferImpl.FileTransferDescriptorTest getIconDescriptor() {
        return mIconDescriptor;
    }

    public String getMimeType() {
        return "image/jpeg";
    }

    public ContactId getLocalContact() {
        return mLocal;
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
}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.provider.xms.model;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.exception.CmsSyncHeaderFormatException;
import com.gsma.rcs.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.cpim.CpimMessage;
import com.gsma.rcs.cms.imap.message.cpim.multipart.MultipartCpimBody;
import com.gsma.rcs.cms.imap.message.cpim.multipart.MultipartCpimBody.Part;
import com.gsma.rcs.cms.imap.message.cpim.text.TextCpimBody;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class XmsDataObjectFactory {

    private final static Logger sLogger = Logger.getLogger(XmsDataObjectFactory.class
            .getSimpleName());

    public static SmsDataObject createSmsDataObject(IImapMessage imapMessage)
            throws CmsSyncHeaderFormatException, CmsSyncMissingHeaderException {

        // Part body = imapMessage.g.getRawMessage().getBody();
        String directionStr = imapMessage.getHeader(Constants.HEADER_DIRECTION);
        Direction direction;
        String header;
        if (Constants.DIRECTION_RECEIVED.equals(directionStr)) {
            direction = Direction.INCOMING;
            header = imapMessage.getHeader(Constants.HEADER_FROM);
        } else {
            direction = Direction.OUTGOING;
            header = imapMessage.getHeader(Constants.HEADER_TO);
        }
        ContactId contactId = CmsUtils.headerToContact(header);
        if (contactId == null) {
            throw new CmsSyncHeaderFormatException("Bad format for header : " + header);
        }
        ReadStatus readStatus = imapMessage.isSeen() ? ReadStatus.READ : ReadStatus.UNREAD;
        String messageCorrelator = imapMessage.getHeader(Constants.HEADER_MESSAGE_CORRELATOR);
        if (messageCorrelator == null) {
            throw new CmsSyncMissingHeaderException("Message-Correlator IMAP header is missing");
        }
        CpimMessage cpimMessage = imapMessage.getCpimMessage();
        // when fetching only headers cpim message is null
        String content = (cpimMessage == null ? "" : ((TextCpimBody) cpimMessage.getBody())
                .getContent());
        SmsDataObject smsDataObject = new SmsDataObject(IdGenerator.generateMessageID(), contactId,
                content, direction, DateUtils.parseDate(
                        imapMessage.getHeader(Constants.HEADER_DATE),
                        DateUtils.CMS_IMAP_DATE_FORMAT), readStatus, messageCorrelator);
        State state;
        if (Direction.INCOMING == direction) {
            state = (readStatus == ReadStatus.READ ? State.DISPLAYED : State.RECEIVED);
        } else {
            state = State.SENT;
        }
        smsDataObject.setState(state);
        return smsDataObject;
    }

    public static MmsDataObject createMmsDataObject(Context context, RcsSettings rcsSettings,
            IImapMessage imapMessage) throws CmsSyncHeaderFormatException, FileAccessException {
        String directionStr = imapMessage.getHeader(Constants.HEADER_DIRECTION);
        Direction direction;
        String header;
        if (Constants.DIRECTION_RECEIVED.equals(directionStr)) {
            direction = Direction.INCOMING;
            header = imapMessage.getHeader(Constants.HEADER_FROM);
        } else {
            direction = Direction.OUTGOING;
            header = imapMessage.getHeader(Constants.HEADER_TO);
        }
        ContactId contactId = CmsUtils.headerToContact(header);
        if (contactId == null) {
            throw new CmsSyncHeaderFormatException("Bad format for header : " + header);
        }
        String messageId = IdGenerator.generateMessageID();
        MultipartCpimBody multipartCpimBody = (MultipartCpimBody) imapMessage.getCpimMessage()
                .getBody();
        List<MmsPart> mmsParts = new ArrayList<>();
        for (Part part : multipartCpimBody.getParts()) {
            String contentType = part.getContentType();
            if (MimeManager.isImageType(contentType)) {
                byte[] data;
                if (Constants.HEADER_BASE64.equals(part.getContentTransferEncoding())) {
                    data = Base64.decodeBase64(part.getContent().getBytes());
                } else {
                    data = part.getContent().getBytes();
                }
                Uri uri = MmsUtils.saveContent(rcsSettings, contentType, part.getContentId(), data);
                String fileName = FileUtils.getFileName(context, uri);
                Long fileLength = (long) data.length;
                long maxIconSize = rcsSettings.getMaxFileIconSize();
                String imageFilename = FileUtils.getPath(context, uri);
                byte[] fileIcon = ImageUtils.tryGetThumbnail(imageFilename, maxIconSize);
                mmsParts.add(new MmsDataObject.MmsPart(messageId, contactId, fileName, fileLength,
                        contentType, uri, fileIcon));

            } else if (Constants.CONTENT_TYPE_TEXT_PLAIN.equals(contentType)) {
                String content = part.getContent();
                mmsParts.add(new MmsDataObject.MmsPart(messageId, contactId, contentType, content));
            } else {
                if (sLogger.isActivated()) {
                    sLogger.warn("Discard part having type " + contentType);
                }
            }
        }
        ReadStatus readStatus = imapMessage.isSeen() ? ReadStatus.READ : ReadStatus.UNREAD;
        MmsDataObject mmsDataObject = new MmsDataObject(
                imapMessage.getHeader(Constants.HEADER_MESSAGE_ID), messageId, contactId,
                imapMessage.getHeader(Constants.HEADER_SUBJECT), direction, readStatus,
                DateUtils.parseDate(imapMessage.getHeader(Constants.HEADER_DATE),
                        DateUtils.CMS_IMAP_DATE_FORMAT), null, null, mmsParts);
        State state;
        if (Direction.INCOMING == direction) {
            state = (readStatus == ReadStatus.READ ? State.DISPLAYED : State.RECEIVED);
        } else {
            state = State.SENT;
        }
        mmsDataObject.setState(state);
        return mmsDataObject;
    }
}

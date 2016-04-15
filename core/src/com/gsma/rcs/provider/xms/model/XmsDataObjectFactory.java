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

package com.gsma.rcs.provider.xms.model;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.protocol.message.ImapMmsMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapSmsMessage;
import com.gsma.rcs.core.cms.protocol.message.cpim.CpimMessage;
import com.gsma.rcs.core.cms.protocol.message.cpim.multipart.MultipartCpimBody;
import com.gsma.rcs.core.cms.protocol.message.cpim.multipart.MultipartCpimBody.Part;
import com.gsma.rcs.core.cms.protocol.message.cpim.text.TextCpimBody;
import com.gsma.rcs.core.cms.utils.MmsUtils;
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
import com.gsma.services.rcs.cms.MmsPartLog;
import com.gsma.services.rcs.cms.MmsPartLog.MimeType;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class XmsDataObjectFactory {

    private final static Logger sLogger = Logger.getLogger(XmsDataObjectFactory.class
            .getSimpleName());

    public static SmsDataObject createSmsDataObject(ImapSmsMessage imapSmsMessage) {

        ContactId contact = imapSmsMessage.getContact();
        Direction direction = imapSmsMessage.getDirection();
        ReadStatus readStatus = imapSmsMessage.isSeen() ? ReadStatus.READ : ReadStatus.UNREAD;
        CpimMessage cpimMessage = imapSmsMessage.getCpimMessage();
        // when fetching only headers cpim message is null
        String content = (cpimMessage == null ? "" : ((TextCpimBody) cpimMessage.getBody())
                .getContent());
        SmsDataObject smsDataObject = new SmsDataObject(IdGenerator.generateMessageID(), contact,
                content, direction, imapSmsMessage.getDate(), readStatus,
                imapSmsMessage.getCorrelator());
        smsDataObject.setState(Direction.INCOMING == direction ? State.RECEIVED : State.SENT );
        return smsDataObject;
    }

    public static MmsDataObject createMmsDataObject(Context context, RcsSettings rcsSettings,
            ImapMmsMessage imapMmsMessage) throws FileAccessException {

        String messageId = IdGenerator.generateMessageID();
        ContactId contactId = imapMmsMessage.getContact();
        Direction direction = imapMmsMessage.getDirection();
        MultipartCpimBody multipartCpimBody = imapMmsMessage.getCpimBody();
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
                String imagePath = FileUtils.getPath(context, uri);
                byte[] fileIcon = ImageUtils.tryGetThumbnail(imagePath, maxIconSize);
                mmsParts.add(new MmsDataObject.MmsPart(messageId, contactId, fileName, fileLength,
                        contentType, uri, fileIcon));

            } else if (MimeManager.isTextType(contentType)) {
                String content = part.getContent();
                mmsParts.add(new MmsDataObject.MmsPart(messageId, contactId, MimeType.TEXT_MESSAGE, content));
            } else {
                if (sLogger.isActivated()) {
                    sLogger.warn("Discard part having type " + contentType);
                }
            }
        }
        ReadStatus readStatus = imapMmsMessage.isSeen() ? ReadStatus.READ : ReadStatus.UNREAD;
        MmsDataObject mmsDataObject = new MmsDataObject(imapMmsMessage.getMmsId(), messageId,
                contactId, imapMmsMessage.getSubject(), direction, readStatus,
                imapMmsMessage.getDate(), null, null, mmsParts);
        mmsDataObject.setState(Direction.INCOMING == direction ? State.RECEIVED : State.SENT );
        return mmsDataObject;
    }
}

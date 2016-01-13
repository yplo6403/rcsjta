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

package com.gsma.rcs.cms.imap.message;

import android.content.Context;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.cpim.CpimMessage;
import com.gsma.rcs.cms.imap.message.cpim.multipart.MultipartCpimBody;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.MimeManager;
import com.sonymobile.rcs.imap.Header;

import java.util.List;

public class ImapMmsMessage extends ImapMessage {

    public ImapMmsMessage(com.sonymobile.rcs.imap.ImapMessage rawMessage) {
        super(rawMessage);
    }

    public ImapMmsMessage(Context context, String from, String to, String direction, long date,
                          String subject, String conversationId, String contributionId, String imdnMessageId,
                          String mmsId, List<MmsPart> mmsParts) {
        super();

        addHeader(Constants.HEADER_FROM, from);
        addHeader(Constants.HEADER_TO, to);
        addHeader(Constants.HEADER_DATE,
                DateUtils.getDateAsString(date, DateUtils.CMS_IMAP_DATE_FORMAT));
        addHeader(Constants.HEADER_CONVERSATION_ID, conversationId);
        addHeader(Constants.HEADER_CONTRIBUTION_ID, contributionId);
        addHeader(Constants.HEADER_MESSAGE_ID, mmsId);
        addHeader(Constants.HEADER_IMDN_MESSAGE_ID, imdnMessageId);
        addHeader(Constants.HEADER_DIRECTION, direction);
        addHeader(Constants.HEADER_MESSAGE_CONTEXT, Constants.MULTIMEDIA_MESSAGE);
        addHeader(Constants.HEADER_CONTENT_TYPE, Constants.MESSAGE_CPIM);

        HeaderPart cpimHeaders = new HeaderPart();
        cpimHeaders.addHeader(Constants.HEADER_FROM, from);
        cpimHeaders.addHeader(Constants.HEADER_TO, to);
        cpimHeaders.addHeader(Constants.HEADER_DATE_TIME,
                DateUtils.getDateAsString(date, DateUtils.CMS_CPIM_DATE_FORMAT));
        if (subject != null) {
            cpimHeaders.addHeader(Constants.HEADER_SUBJECT, subject);
        }
        cpimHeaders.addHeader("NS", "imdn <urn:ietf:params:imdn>");
        cpimHeaders.addHeader("NS", "rcs <http://www.gsma.com>");
        cpimHeaders.addHeader("imdn.Message-ID", imdnMessageId);

        MultipartCpimBody multipartCpimBody = new MultipartCpimBody();
        for (MmsPart mmsPart : mmsParts) {
            String mimeType = mmsPart.getMimeType();
            String content = mmsPart.getContentText();
            String contentId = mmsPart.getFileName();
            String transferEncoding = null;
            // If not text or SMIL ?
            if (MimeManager.isImageType(mimeType)) { // base 64
                byte[] bytes = MmsUtils.getContent(context.getContentResolver(), mmsPart.getFile());
                if (bytes == null) {
                    continue;
                }
                transferEncoding = Constants.HEADER_BASE64;
                content = Base64.encodeBase64ToString(bytes);
            }
            HeaderPart headers = new HeaderPart();
            if(mimeType!=null){
                headers.addHeader(Constants.HEADER_CONTENT_TYPE, mimeType);
            }
            if(contentId!=null){
                headers.addHeader(Constants.HEADER_CONTENT_ID, contentId);
            }
            if(transferEncoding!=null){
                headers.addHeader(Constants.HEADER_CONTENT_TRANSFER_ENCODING,transferEncoding);
            }
            multipartCpimBody.addMultiPart(headers, content);
        }
        mCpimMessage = new CpimMessage(cpimHeaders, multipartCpimBody);
    }

    @Override
    public void parsePayload(String payload) {
        String[] parts = payload.split(Constants.CRLFCRLF,2);
        if(2 == parts.length ){
            for(Header header : Header.parseHeaders(parts[0]).values()){
                addHeader(header.getKey(), header.getValue());
            }
            mCpimMessage = new CpimMessage(new HeaderPart(), new MultipartCpimBody());
            mCpimMessage.parsePayload(parts[1]);
        }
    }
}

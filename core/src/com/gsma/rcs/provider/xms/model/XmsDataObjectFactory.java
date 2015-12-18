// TODO FG add copyrights

package com.gsma.rcs.provider.xms.model;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.event.ImapHeaderFormatException;
import com.gsma.rcs.cms.imap.message.ImapMmsMessage;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.message.mime.MmsMimeMessage;
import com.gsma.rcs.cms.imap.message.mime.MultiPart;
import com.gsma.rcs.cms.imap.message.mime.SmsMimeMessage;
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
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import com.sonymobile.rcs.imap.Part;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class XmsDataObjectFactory {

    public static SmsDataObject createSmsDataObject(ImapSmsMessage imapMessage)
            throws ImapHeaderFormatException {

        Part body = imapMessage.getRawMessage().getBody();
        String directionStr = body.getHeader(Constants.HEADER_DIRECTION);
        Direction direction;
        String header;
        if (Constants.DIRECTION_RECEIVED.equals(directionStr)) {
            direction = Direction.INCOMING;
            header = body.getHeader(Constants.HEADER_FROM);
        } else {
            direction = Direction.OUTGOING;
            header = body.getHeader(Constants.HEADER_TO);
        }
        ContactId contactId = CmsUtils.headerToContact(header);
        if (contactId == null) {
            throw new ImapHeaderFormatException("Bad format for header : " + header);
        }
        ReadStatus readStatus = imapMessage.isSeen() ? ReadStatus.READ : ReadStatus.UNREAD;
        SmsDataObject smsDataObject = new SmsDataObject(IdGenerator.generateMessageID(), contactId,
                ((SmsMimeMessage) imapMessage.getPart()).getBodyPart(), direction,
                DateUtils.parseDate(body.getHeader(Constants.HEADER_DATE),
                        DateUtils.CMS_IMAP_DATE_FORMAT), readStatus,
                body.getHeader(Constants.HEADER_MESSAGE_CORRELATOR));
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
            ImapMmsMessage imapMessage) throws ImapHeaderFormatException, FileAccessException {
        Part body = imapMessage.getRawMessage().getBody();
        String directionStr = body.getHeader(Constants.HEADER_DIRECTION);
        Direction direction;
        String header;
        if (Constants.DIRECTION_RECEIVED.equals(directionStr)) {
            direction = Direction.INCOMING;
            header = body.getHeader(Constants.HEADER_FROM);
        } else {
            direction = Direction.OUTGOING;
            header = body.getHeader(Constants.HEADER_TO);
        }
        ContactId contactId = CmsUtils.headerToContact(header);
        if (contactId == null) {
            throw new ImapHeaderFormatException("Bad format for header : " + header);
        }
        String messageId = IdGenerator.generateMessageID();
        MmsMimeMessage mmsMimeMessage = (MmsMimeMessage) imapMessage.getPart();
        List<MmsPart> mmsParts = new ArrayList<>();
        for (MultiPart multipart : mmsMimeMessage.getMimebody().getMultiParts()) {
            String contentType = multipart.getContentType();
            // TODO FG : is checking correct ? How is handled SMIL ?
            // Use MimeManage.isImage
            if (MmsUtils.sContentTypeImage.contains(contentType)) {
                byte[] data;
                if (Constants.HEADER_BASE64.equals(multipart.getContentTransferEncoding())) {
                    data = Base64.decodeBase64(multipart.getContent().getBytes());
                } else {
                    data = multipart.getContent().getBytes();
                }
                Uri uri = MmsUtils.saveContent(rcsSettings, contentType, multipart.getContentId(),
                        data);
                String fileName = FileUtils.getFileName(context, uri);
                Long fileLength = (long) data.length;
                long maxIconSize = rcsSettings.getMaxFileIconSize();
                String imageFilename = FileUtils.getPath(context, uri);
                byte[] fileIcon = ImageUtils.tryGetThumbnail(imageFilename, maxIconSize);
                mmsParts.add(new MmsDataObject.MmsPart(messageId, contactId, fileName, fileLength,
                        contentType, uri, fileIcon));

            } else if (Constants.CONTENT_TYPE_TEXT.equals(contentType)) {
                String content = multipart.getContent();
                mmsParts.add(new MmsDataObject.MmsPart(messageId, contactId, contentType, content));
            }
        }
        ReadStatus readStatus = imapMessage.isSeen() ? ReadStatus.READ : ReadStatus.UNREAD;
        MmsDataObject mmsDataObject = new MmsDataObject(
                body.getHeader(Constants.HEADER_MESSAGE_ID), messageId, contactId,
                body.getHeader(Constants.HEADER_SUBJECT), direction, readStatus,
                DateUtils.parseDate(body.getHeader(Constants.HEADER_DATE),
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

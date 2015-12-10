
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
import com.gsma.rcs.core.ims.service.presence.pidf.Contact;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.PartData;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;

import com.gsma.services.rcs.contact.ContactId;
import com.sonymobile.rcs.imap.Part;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class XmsDataObjectFactory {

    public static XmsDataObject createXmsDataObject(XmsLog xmsLog, String messageId) {
        Cursor cursor = null;
        XmsDataObject xmsDataObject;
        try {
            cursor = xmsLog.getXmsMessage(messageId);
            if (!cursor.moveToNext()) {
                return null;
            }
            String mimeType = cursor.getString(cursor.getColumnIndex(XmsData.KEY_MIME_TYPE));
            String contact = cursor.getString(cursor.getColumnIndex(XmsData.KEY_CONTACT));
            String content = cursor.getString(cursor.getColumnIndex(XmsData.KEY_CONTENT));
            Direction direction = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndex(XmsData.KEY_DIRECTION)));
            ReadStatus readStatus = ReadStatus.valueOf(cursor.getInt(cursor
                    .getColumnIndex(XmsData.KEY_READ_STATUS)));
            long date = cursor.getLong(cursor.getColumnIndex(XmsData.KEY_TIMESTAMP));

            if (MimeType.TEXT_MESSAGE.equals(mimeType)) {
                xmsDataObject = new SmsDataObject(messageId,
                        ContactUtil.createContactIdFromTrustedData(contact), content, direction, date,
                        readStatus);
            } else {
                String mmsId = cursor.getString(cursor.getColumnIndex(XmsData.KEY_MMS_ID));
                List<MmsDataObject.MmsPart> mmsParts = createMmsPart(xmsLog, messageId);
                String body = null;
                for(MmsPart mmsPart : mmsParts){ // search for body
                    if(MimeType.TEXT_MESSAGE.equals(mmsPart.getMimeType())){
                        body = mmsPart.getBody();
                        break;
                    }
                }
                xmsDataObject = new MmsDataObject(mmsId, messageId,
                        ContactUtil.createContactIdFromTrustedData(contact), content,
                        direction, readStatus, date, 0l, 0l,mmsParts );
            }
            return xmsDataObject;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private static List<MmsDataObject.MmsPart> createMmsPart(XmsLog xmsLog, String messageId) {
        Cursor cursor = null;
        List<MmsDataObject.MmsPart> mmsParts = new ArrayList<>();
        try {
            cursor = xmsLog.getMmsPart(messageId);
            int contactIdx = cursor.getColumnIndex(PartData.KEY_CONTACT);
            int mimetypeIdx = cursor.getColumnIndex(PartData.KEY_MIME_TYPE);
            int filenameIdx = cursor.getColumnIndex(PartData.KEY_FILENAME);
            int fileiconIdx = cursor.getColumnIndex(PartData.KEY_FILEICON);
            int filesizeIdx = cursor.getColumnIndex(PartData.KEY_FILESIZE);
            int contentIdx = cursor.getColumnIndex(PartData.KEY_CONTENT);
            while (cursor.moveToNext()) {
                mmsParts.add(new MmsDataObject.MmsPart(messageId, ContactUtil
                        .createContactIdFromTrustedData(cursor.getString(contactIdx)), cursor
                        .getString(mimetypeIdx), cursor.getString(filenameIdx), cursor
                        .getLong(filesizeIdx), cursor.getString(contentIdx), cursor
                        .getBlob(fileiconIdx)));
            }
            return mmsParts;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public static SmsDataObject createSmsDataObject(ImapSmsMessage imapMessage) throws ImapHeaderFormatException {

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
        if(contactId == null){
            throw new ImapHeaderFormatException("Bad format for header : " + header );
        }
        ReadStatus readStatus = imapMessage.isSeen()? ReadStatus.READ : ReadStatus.UNREAD;
        SmsDataObject smsDataObject = new SmsDataObject(
                IdGenerator.generateMessageID(),
                contactId,
                ((SmsMimeMessage)imapMessage.getPart()).getBodyPart(),
                direction,
                DateUtils.parseDate(body.getHeader(Constants.HEADER_DATE), DateUtils.CMS_IMAP_DATE_FORMAT),
                readStatus,
                body.getHeader(Constants.HEADER_MESSAGE_CORRELATOR));
        State state;
        if(Direction.INCOMING == direction){
            state = (readStatus == ReadStatus.READ ? State.DISPLAYED : State.RECEIVED);
        }
        else{
            state = State.SENT;
        }
        smsDataObject.setState(state);
        return smsDataObject;
    }

    public static MmsDataObject createMmsDataObject(Context context, RcsSettings rcsSettings,
            ImapMmsMessage imapMessage) throws ImapHeaderFormatException {
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
        if(contactId == null){
            throw new ImapHeaderFormatException("Bad format for header : " + header );
        }
        String messageId = IdGenerator.generateMessageID();
        MmsMimeMessage mmsMimeMessage = (MmsMimeMessage) imapMessage.getPart();
        List<MmsPart> mmsParts = new ArrayList<>();
        for (MultiPart multipart : mmsMimeMessage.getMimebody().getMultiParts()) {
            String contentType = multipart.getContentType();
            String content = "";
            String fileName = "";
            long fileLength = 0l;
            byte[] fileIcon = null;
            if (MmsUtils.sContentTypeImage.contains(contentType)) {
                byte[] data;
                if (Constants.HEADER_BASE64.equals(multipart.getContentTransferEncoding())) {
                    data = Base64.decodeBase64(multipart.getContent().getBytes());
                } else {
                    data = multipart.getContent().getBytes();
                }
                Uri uri = MmsUtils.saveContent(rcsSettings, contentType, multipart.getContentId(), data);
                content = uri.toString();
                fileName = FileUtils.getFileName(context, uri);
                fileLength = data.length;
                long maxIconSize = rcsSettings.getMaxFileIconSize();
                fileIcon = ImageUtils.tryGetThumbnail(context, uri, maxIconSize);

            } else if (Constants.CONTENT_TYPE_TEXT.equals(contentType)) {
                content = multipart.getContent();
            }

            mmsParts.add(new MmsDataObject.MmsPart(messageId,
                    contactId,
                    contentType, fileName, fileLength,
                    content, fileIcon));
        }

        ReadStatus readStatus = imapMessage.isSeen()? ReadStatus.READ : ReadStatus.UNREAD;
        MmsDataObject mmsDataObject = new MmsDataObject(
                body.getHeader(Constants.HEADER_MESSAGE_ID),
                messageId,
                contactId,
                body.getHeader(Constants.HEADER_SUBJECT),
                direction,
                readStatus,
                DateUtils.parseDate(body.getHeader(Constants.HEADER_DATE), DateUtils.CMS_IMAP_DATE_FORMAT),
                0,
                0,
                mmsParts
                );
        State state;
        if (Direction.INCOMING == direction) {
            state = (readStatus == ReadStatus.READ ? State.DISPLAYED : State.RECEIVED);
        }
        else{
            state = State.SENT;
        }
        mmsDataObject.setState(state);
        return mmsDataObject;
    }
}

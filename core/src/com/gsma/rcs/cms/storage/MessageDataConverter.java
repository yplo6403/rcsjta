package com.gsma.rcs.cms.storage;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.ImapMmsMessage;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.message.mime.SmsMimeMessage;
import com.gsma.rcs.cms.provider.xms.model.MmsData;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.services.rcs.RcsService.Direction;
import com.sonymobile.rcs.imap.Part;

public class MessageDataConverter {
    
    public static SmsData convertIntoSmsData(ImapSmsMessage imapMessage){
        
        Part body = imapMessage.getRawMessage().getBody();
        String directionStr = body.getHeader(Constants.HEADER_DIRECTION);
        Direction direction;
        String contact;
        if(Constants.DIRECTION_RECEIVED.equals(directionStr)){
            direction = Direction.INCOMING;
            contact =  body.getHeader(Constants.HEADER_FROM);
        }
        else{
            direction = Direction.OUTGOING;
            contact =  body.getHeader(Constants.HEADER_TO);
        }
        contact = CmsUtils.headerToContact(contact);
        return new SmsData(
                null,
                null,
                contact,
                ((SmsMimeMessage)imapMessage.getPart()).getBodyPart() ,
                DateUtils.parseDate(body.getHeader(Constants.HEADER_DATE), DateUtils.CMS_IMAP_DATE_FORMAT),
                direction,
                imapMessage.isSeen()? ReadStatus.READ : ReadStatus.UNREAD,
                body.getHeader(Constants.HEADER_MESSAGE_CORRELATOR));
    }

    public static MmsData convertIntoMmsData(ImapMmsMessage imapMessage){

        Part body = imapMessage.getRawMessage().getBody();
        String directionStr = body.getHeader(Constants.HEADER_DIRECTION);
        Direction direction;
        String contact;
        if(Constants.DIRECTION_RECEIVED.equals(directionStr)){
            direction = Direction.INCOMING;
            contact =  body.getHeader(Constants.HEADER_FROM);
        }
        else{
            direction = Direction.OUTGOING;
            contact =  body.getHeader(Constants.HEADER_TO);
        }
        contact = CmsUtils.headerToContact(contact);
        MmsData mmsData = new MmsData(
                null,
                null,
                null,
                body.getHeader(Constants.HEADER_MESSAGE_ID),
                contact,
                body.getHeader(Constants.HEADER_SUBJECT),
                null,
                DateUtils.parseDate(body.getHeader(Constants.HEADER_DATE), DateUtils.CMS_IMAP_DATE_FORMAT),
                direction,
                imapMessage.isSeen()? ReadStatus.READ : ReadStatus.UNREAD
                );
        return mmsData;
    }


}

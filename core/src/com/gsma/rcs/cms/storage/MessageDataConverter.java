package com.gsma.rcs.cms.storage;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.sms.ImapSmsMessage;
import com.gsma.rcs.cms.imap.message.sms.SmsPart;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
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
        
        if(contact.toLowerCase().startsWith(Constants.TEL_PREFIX)){
            contact = contact.substring(Constants.TEL_PREFIX.length());
        }
        
        return new SmsData(
                null,
                contact,
                ((SmsPart)imapMessage.getPart()).getContent() ,
                DateUtils.parseDate(body.getHeader(Constants.HEADER_DATE)),
                direction,
                imapMessage.isSeen()? ReadStatus.READ : ReadStatus.UNREAD,
                body.getHeader(Constants.HEADER_MESSAGE_CORRELATOR));
    }

}

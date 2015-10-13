package com.gsma.rcs.cms.imap.message.sms;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.rcs.cms.utils.HeaderCorrelatorUtils;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.Header;
import com.sonymobile.rcs.imap.IPart;
import com.sonymobile.rcs.imap.ImapMessage;

import java.util.ArrayList;
import java.util.Collection;

public class ImapSmsMessage implements IImapMessage {
        
    private ImapMessage mImapMessage;
    private SmsPart mPart;
    
    public ImapSmsMessage(ImapMessage imapMessage) {
        super();
        mImapMessage = imapMessage;
        mPart = new SmsPart(mImapMessage.getPayload());
    }
    
    public ImapSmsMessage(String from, String to, String direction, long date,
            String content, String conversationId, String contributionId, String imdnMessageId) {
        super();
        
        String dateStr = DateUtils.getDateAsString(date);        
        Collection<Header> mailHeaders = new ArrayList<Header>();
        mailHeaders.add(Header.createHeader(Constants.HEADER_FROM + ":" + from ));
        mailHeaders.add(Header.createHeader(Constants.HEADER_TO + ": tel:" + to ));
        mailHeaders.add(Header.createHeader(Constants.HEADER_DATE + ": " + dateStr ));
        //TODO FGI TO BE  FIXED
        mailHeaders.add(Header.createHeader(Constants.HEADER_CONVERSATION_ID + ": " + conversationId ));
        mailHeaders.add(Header.createHeader(Constants.HEADER_CONTRIBUTION_ID + ": " + contributionId ));
        mailHeaders.add(Header.createHeader(Constants.HEADER_IMDN_MESSAGE_ID + ": " + imdnMessageId ));        
        mailHeaders.add(Header.createHeader(Constants.HEADER_DIRECTION + ": " + direction ));                
        mailHeaders.add(Header.createHeader(Constants.HEADER_MESSAGE_CORRELATOR + ": " + HeaderCorrelatorUtils.buildHeader(content) ));
        mailHeaders.add(Header.createHeader(Constants.HEADER_MESSAGE_CONTEXT + ": " + Constants.PAGER_MESSAGE ));
        mailHeaders.add(Header.createHeader(Constants.HEADER_CONTENT_TYPE + ": " + Constants.CONTENT_TYPE_CPIM ));        
        mailHeaders.add(Header.createHeader(Constants.HEADER_CONTENT_TRANSFER_ENCODING + ": " + Constants.HEADER_BASE64 ));
        
        Collection<Header> cpimHeaders = new ArrayList<Header>();
        cpimHeaders.add(Header.createHeader(Constants.HEADER_FROM + ":" + from ));
        cpimHeaders.add(Header.createHeader(Constants.HEADER_TO + ": " + to ));        
        cpimHeaders.add(Header.createHeader("imdn.Message-ID" + ": " + imdnMessageId ));
        cpimHeaders.add(Header.createHeader("DateTime: " + dateStr));
        
        Collection<Header> mimeHeaders = new ArrayList<Header>();        
        mimeHeaders.add(Header.createHeader(Constants.HEADER_CONTENT_TYPE + ": text/plain; charset=utf-8"));
        
        mPart = new SmsPart(mailHeaders, cpimHeaders, mimeHeaders, content);        
    }

    @Override
    public IPart getPart() {
        return mPart;
    }

    @Override
    public String getFolder() {
        return mImapMessage.getFolderPath();
    }

    @Override
    public Integer getUid() {
        return mImapMessage.getUid();
    }

    @Override
    public boolean isSeen() {
        return mImapMessage.getMetadata().getFlags().contains(Flag.Seen);
    }

    @Override
    public boolean isDeleted() {
        return mImapMessage.getMetadata().getFlags().contains(Flag.Deleted);
    } 
   
   public ImapMessage getRawMessage(){
       return mImapMessage;
   }
}

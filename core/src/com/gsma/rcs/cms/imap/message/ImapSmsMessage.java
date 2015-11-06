package com.gsma.rcs.cms.imap.message;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.mime.MimeBody;
import com.gsma.rcs.cms.imap.message.mime.MmsMimeBody;
import com.gsma.rcs.cms.imap.message.mime.MimeHeaders;
import com.gsma.rcs.cms.imap.message.mime.SmsMimeBody;
import com.gsma.rcs.cms.imap.message.mime.SmsMimeMessage;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.rcs.cms.utils.HeaderCorrelatorUtils;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.IPart;
import com.sonymobile.rcs.imap.ImapMessage;

public class ImapSmsMessage implements IImapMessage {
        
    private ImapMessage mImapMessage;
    private SmsMimeMessage mimeMessage;
    
    public ImapSmsMessage(ImapMessage imapMessage) {
        super();
        mImapMessage = imapMessage;
        mimeMessage = new SmsMimeMessage(mImapMessage.getPayload());
    }
    
    public ImapSmsMessage(String from, String to, String direction, long date,
            String content, String conversationId, String contributionId, String imdnMessageId) {
        super();

        String dateStr = DateUtils.getDateAsString(date);

        mimeMessage = new SmsMimeMessage();
        MimeHeaders mailHeaders = new MimeHeaders();
        mailHeaders.addHeader(Constants.HEADER_FROM, from);
        mailHeaders.addHeader(Constants.HEADER_TO, to);
        mailHeaders.addHeader(Constants.HEADER_DATE, dateStr);
        mailHeaders.addHeader(Constants.HEADER_CONVERSATION_ID, conversationId);
        mailHeaders.addHeader(Constants.HEADER_CONTRIBUTION_ID, contributionId);
        mailHeaders.addHeader(Constants.HEADER_IMDN_MESSAGE_ID, imdnMessageId);
        mailHeaders.addHeader(Constants.HEADER_DIRECTION, direction);
        mailHeaders.addHeader(Constants.HEADER_MESSAGE_CORRELATOR, HeaderCorrelatorUtils.buildHeader(content));
        mailHeaders.addHeader(Constants.HEADER_MESSAGE_CONTEXT, Constants.PAGER_MESSAGE);
        mailHeaders.addHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_CPIM);
        //mailHeaders.add(Header.createHeader(Constants.HEADER_CONTENT_TRANSFER_ENCODING + ": " + Constants.HEADER_BASE64 ));
        mimeMessage.addHeaderPart(mailHeaders);

        MimeHeaders cpimHeaders =  new MimeHeaders();
        cpimHeaders.addHeader(Constants.HEADER_FROM, from);
        cpimHeaders.addHeader(Constants.HEADER_TO, to);
        cpimHeaders.addHeader("imdn.Message-ID", imdnMessageId);
        cpimHeaders.addHeader("DateTime", dateStr);
        mimeMessage.addHeaderPart(cpimHeaders);

        MimeHeaders mimeHeaders =  new MimeHeaders();
        mimeHeaders.addHeader(Constants.HEADER_CONTENT_TYPE, "text/plain; charset=utf-8");
        mimeMessage.addHeaderPart(mimeHeaders);

        MimeBody mimeBody = new SmsMimeBody();
        mimeBody.parsePayload(content);
        mimeMessage.setBodyPart(mimeBody);
    }

    @Override
    public IPart getPart() {
        return mimeMessage;
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

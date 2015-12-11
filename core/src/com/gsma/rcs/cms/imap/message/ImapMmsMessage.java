package com.gsma.rcs.cms.imap.message;

import android.content.Context;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.mime.MimeHeaders;
import com.gsma.rcs.cms.imap.message.mime.MmsMimeBody;
import com.gsma.rcs.cms.imap.message.mime.MmsMimeMessage;
import com.gsma.rcs.cms.utils.DateUtils;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.MimeManager;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.IPart;
import com.sonymobile.rcs.imap.ImapMessage;

import java.util.List;

public class ImapMmsMessage implements IImapMessage {

    private ImapMessage mImapMessage;
    private MmsMimeMessage mimeMessage;

    public ImapMmsMessage(ImapMessage imapMessage) {
        super();
        mImapMessage = imapMessage;
        mimeMessage = new MmsMimeMessage(mImapMessage.getPayload());
    }

    public ImapMmsMessage(
            Context context,
            String from,
            String to,
            String direction,
            long date,
            String subject,
            String conversationId,
            String contributionId,
            String imdnMessageId,
            String mmsId,
            List<MmsPart> mmsParts
            ) {
        super();

        mimeMessage = new MmsMimeMessage();
        MimeHeaders mailHeaders = new MimeHeaders();
        mailHeaders.addHeader(Constants.HEADER_FROM, from);
        mailHeaders.addHeader(Constants.HEADER_TO, to);
        mailHeaders.addHeader(Constants.HEADER_DATE, DateUtils.getDateAsString(date, DateUtils.CMS_IMAP_DATE_FORMAT));
        mailHeaders.addHeader(Constants.HEADER_CONVERSATION_ID, conversationId);
        mailHeaders.addHeader(Constants.HEADER_CONTRIBUTION_ID, contributionId);
        mailHeaders.addHeader(Constants.HEADER_MESSAGE_ID, mmsId);
        mailHeaders.addHeader(Constants.HEADER_IMDN_MESSAGE_ID, imdnMessageId);
        mailHeaders.addHeader(Constants.HEADER_DIRECTION, direction);
        mailHeaders.addHeader(Constants.HEADER_MESSAGE_CONTEXT, Constants.MULTIMEDIA_MESSAGE);
        mailHeaders.addHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_CPIM);
        mimeMessage.addHeaderPart(mailHeaders);

        MimeHeaders cpimHeaders = new MimeHeaders();
        cpimHeaders.addHeader(Constants.HEADER_FROM, from);
        cpimHeaders.addHeader(Constants.HEADER_TO, to);
        cpimHeaders.addHeader(Constants.HEADER_DATE_TIME, DateUtils.getDateAsString(date, DateUtils.CMS_CPIM_DATE_FORMAT));
        if(subject!=null){
            cpimHeaders.addHeader(Constants.HEADER_SUBJECT, subject);
        }
        cpimHeaders.addHeader("NS", "imdn <urn:ietf:params:imdn>");
        cpimHeaders.addHeader("NS", "rcs <http://www.gsma.com>");
        cpimHeaders.addHeader("imdn.Message-ID", imdnMessageId);
        mimeMessage.addHeaderPart(cpimHeaders);

        MmsMimeBody mimeBody  = new MmsMimeBody();
        for(MmsPart mmsPart : mmsParts){
            String mimeType = mmsPart.getMimeType();
            String content = mmsPart.getBody();
            String contentId = mmsPart.getFileName();
            String transferEncoding = null;
            if(MimeManager.isImageType(mimeType)){ // base 64
                byte[] bytes = MmsUtils.getContent(context.getContentResolver(), mmsPart.getFile());
                if(bytes==null){
                    continue;
                }
                transferEncoding = Constants.HEADER_BASE64;
                content = Base64.encodeBase64ToString(bytes);
            }
            mimeBody.addMultiPart(mmsPart.getMimeType(),contentId, transferEncoding, content);
        }
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

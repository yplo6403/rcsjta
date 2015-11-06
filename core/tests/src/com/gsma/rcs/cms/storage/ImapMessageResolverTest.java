package com.gsma.rcs.cms.storage;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class ImapMessageResolverTest extends AndroidTestCase {

    public void testSms(){
        
        String payload= new StringBuilder()
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: tel:+33640332859").append(Constants.CRLF)
                .append("Date: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: received").append(Constants.CRLF)
                .append("Message-Correlator: 1").append(Constants.CRLF)
                .append("Message-Context: \"pager-message\"").append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF)
                .append("imdn.Message-ID: 1443517760826").append(Constants.CRLF)
                .append("DateTime: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("1)").append(Constants.CRLF).toString();
        
        Integer uid = 12;                
        Part part = new Part();
        part.fromPayload(payload);        
        ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
        metadata.getFlags().add(Flag.Seen);
        ImapMessage imapMessage =  new ImapMessage(uid, metadata, part);
        
        ImapMessageResolver imapMessageResolver = new ImapMessageResolver();
        MessageType type = imapMessageResolver.resolveType(imapMessage);
        Assert.assertEquals(MessageType.SMS, type);
        
        Assert.assertTrue(imapMessageResolver.resolveMessage(type, imapMessage) instanceof ImapSmsMessage);
        
    }
    
    public void testMms(){
        
        String payload= new StringBuilder()
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: tel:+33640332859").append(Constants.CRLF)
                .append("Date: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                .append("Conversation-ID: 1443517760826").append(Constants.CRLF)
                .append("Contribution-ID: 1443517760826").append(Constants.CRLF)
                .append("IMDN-Message-ID: 1443517760826").append(Constants.CRLF)
                .append("Message-Direction: received").append(Constants.CRLF)
                .append("Message-Correlator: 1").append(Constants.CRLF)
                .append("Message-Context: \"multimedia-message\"").append(Constants.CRLF)
                .append("Content-Type: Message/CPIM").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("From: +33642575779").append(Constants.CRLF)
                .append("To: +33640332859").append(Constants.CRLF)
                .append("imdn.Message-ID: 1443517760826").append(Constants.CRLF)
                .append("DateTime: mar., 29 09 2015 11:09:20.826 +0200").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("Content-Type: text/plain; charset=utf-8").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("1)").append(Constants.CRLF).toString();
        
        Integer uid = 12;                
        Part part = new Part();
        part.fromPayload(payload);        
        ImapMessageMetadata metadata = new ImapMessageMetadata(uid);
        metadata.getFlags().add(Flag.Seen);
        ImapMessage imapMessage =  new ImapMessage(uid, metadata, part);
        
        ImapMessageResolver imapMessageResolver = new ImapMessageResolver();
        MessageType type = imapMessageResolver.resolveType(imapMessage);
        Assert.assertEquals(MessageType.MMS, type);
        
        //TODO
        //Assert.assertTrue(imapMessageResolver.resolveMessage(type, imapMessage) instanceof ImapMmsMessage);
        
    }
}

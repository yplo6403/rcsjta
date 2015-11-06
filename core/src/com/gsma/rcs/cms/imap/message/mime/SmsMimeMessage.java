package com.gsma.rcs.cms.imap.message.mime;

import com.gsma.rcs.cms.Constants;
import com.sonymobile.rcs.imap.Header;
import com.sonymobile.rcs.imap.IPart;

import java.util.ArrayList;

public class SmsMimeMessage extends MimeMessage implements IPart{

    public SmsMimeMessage(){
        mMimeHeaders = new ArrayList<>();
        mMimeBody = new SmsMimeBody();
    }

    public SmsMimeMessage(String payload){
        this();
        fromPayload(payload);
    }

    @Override
    public String toPayload() {
        return toString();
    }

    @Override
    public void fromPayload(String payload) {

        String[] parts = payload.split(Constants.CRLFCRLF,4);
        if(4 == parts.length ){

            MimeHeaders mailHeaders = new MimeHeaders();
            for(Header header : Header.parseHeaders(parts[0]).values()){
                mailHeaders.addHeader(header.getKey(), header.getValue());
            }
            mMimeHeaders.add(mailHeaders);

            MimeHeaders cpimHeaders = new MimeHeaders();
            for(Header header : Header.parseHeaders(parts[1]).values()){
                cpimHeaders.addHeader(header.getKey(), header.getValue());
            }
            mMimeHeaders.add(cpimHeaders);

            MimeHeaders mimeHeaders = new MimeHeaders();
            for(Header header : Header.parseHeaders(parts[2]).values()){
                mimeHeaders.addHeader(header.getKey(), header.getValue());
            }
            mMimeHeaders.add(mimeHeaders);
            mMimeBody.parsePayload(parts[3]);
        }
    }
}

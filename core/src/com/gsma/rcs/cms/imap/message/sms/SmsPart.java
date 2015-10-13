package com.gsma.rcs.cms.imap.message.sms;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.utils.Base64;

import com.sonymobile.rcs.imap.Header;
import com.sonymobile.rcs.imap.IPart;
import com.sonymobile.rcs.imap.Part;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class SmsPart implements IPart{
    
    private Collection<Header> mMailHeaders = new ArrayList<Header>();
    private Collection<Header> mCpimHeaders = new ArrayList<Header>();
    private Collection<Header> mMimeHeaders = new ArrayList<Header>();
    private String mContent;
    
    public SmsPart(String payload){
        fromPayload(payload);
    }
    
    public SmsPart(Collection<Header> mailHeaders, Collection<Header> cpimHeaders, Collection<Header> mimeHeaders, String content){      
        mMailHeaders = mailHeaders;
        mCpimHeaders = cpimHeaders;
        mMimeHeaders = mimeHeaders;
        mContent = content;
    }
    

    @Override
    public String toPayload() {
        
        StringBuffer payload = new StringBuffer();
        for (Header header : mMailHeaders) {
            payload.append(header.getKey());
            payload.append(':');
            payload.append(' ');
            payload.append(header.getValue());
            payload.append(Constants.CRLF);            
        }
        payload.append(Constants.CRLF);
        
        StringBuffer body = new StringBuffer();
        for (Header header : mCpimHeaders) {
            body.append(header.getKey());
            body.append(':');
            body.append(' ');
            body.append(header.getValue());
            body.append(Constants.CRLF);            
        }
        body.append(Constants.CRLF);
        for (Header header : mMimeHeaders) {
            body.append(header.getKey());
            body.append(':');
            body.append(' ');
            body.append(header.getValue());
            body.append(Constants.CRLF);            
        }
        body.append(Constants.CRLF); 
        body.append(mContent);
        
        payload.append(new String(Base64.encodeBase64(body.toString().getBytes())));
        return payload.toString();
    }
    
//    @Override
//    public String toPayload() {
//        
//        StringBuffer sb = new StringBuffer();
//        for (Header header : mMailHeaders) {
//            sb.append(header.getKey());
//            sb.append(':');
//            sb.append(' ');
//            sb.append(header.getValue());
//            sb.append(Constants.CRLF);            
//        }
//        sb.append(Constants.CRLF);
//        for (Header header : mCpimHeaders) {
//            sb.append(header.getKey());
//            sb.append(':');
//            sb.append(' ');
//            sb.append(header.getValue());
//            sb.append(Constants.CRLF);            
//        }
//        sb.append(Constants.CRLF);
//        for (Header header : mMimeHeaders) {
//            sb.append(header.getKey());
//            sb.append(':');
//            sb.append(' ');
//            sb.append(header.getValue());
//            sb.append(Constants.CRLF);            
//        }
//        sb.append(Constants.CRLF); 
//        sb.append(mContent);
//        return sb.toString();
//    }

//    @Override
//    public void fromPayload(String payload) {
//                
//        String[] parts = payload.split(Constants.CRLFCRLF,4);
//        if(4 == parts.length ){
//            mMailHeaders = Header.parseHeaders(parts[0]).values();
//            mCpimHeaders = Header.parseHeaders(parts[1]).values();
//            mMimeHeaders = Header.parseHeaders(parts[2]).values();
//            mContent = parts[3];
//        }               
//    }
    
    @Override
    public void fromPayload(String payload) {
                
        String[] parts = payload.split(Constants.CRLFCRLF,2);
        if(2 == parts.length ){                                    
            Map<String, Header> headersMap = Header.parseHeaders(parts[0]);
            mMailHeaders = headersMap.values();
            Header cteHeader = headersMap.get(Constants.HEADER_CONTENT_TRANSFER_ENCODING);            
            String body;
            if(cteHeader!=null && Constants.HEADER_BASE64.equals(cteHeader.getValue())){ // body base64 encoded
                body = new String(Base64.decodeBase64(parts[1].getBytes()));
            }
            else{
                body = parts[1];
            }            
            String[] bodyParts =  body.split(Constants.CRLFCRLF,3);
            if(3 == bodyParts.length ){
                mCpimHeaders = Header.parseHeaders(bodyParts[0]).values();
                mMimeHeaders = Header.parseHeaders(bodyParts[1]).values();
                mContent = bodyParts[2];                
            }
        }               
    }
    
    public String getContent(){
        return mContent;
    }

}

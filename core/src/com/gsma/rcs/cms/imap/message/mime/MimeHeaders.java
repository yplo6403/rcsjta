package com.gsma.rcs.cms.imap.message.mime;

import com.gsma.rcs.cms.Constants;

import java.util.ArrayList;
import java.util.List;

public class MimeHeaders {

    class Header{
        private String mKey;
        private String mValue;
        public Header (String key, String value){
            mKey = key;
            mValue = value;
        }
        public String toString(){
            return new StringBuilder(mKey).append(Constants.HEADER_SEP).append(mValue).append(Constants.CRLF).toString();
        }
    }

    List<Header> mHeaders;

    public MimeHeaders(){
        mHeaders = new ArrayList<>();
    }

    public void addHeader(String key, String value){
        mHeaders.add(new Header(key, value));
    }

    public void addHeader(Header header){
        mHeaders.add(header);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(Header header : mHeaders){
            sb.append(header);
        }
        return sb.toString();
    }
}

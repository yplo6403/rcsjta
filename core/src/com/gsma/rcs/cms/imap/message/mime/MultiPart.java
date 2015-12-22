/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.gsma.rcs.cms.imap.message.mime;

import com.gsma.rcs.cms.Constants;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MultiPart {

    private Map<String,String> mHeaders = new LinkedHashMap();
    private String mContent;

    public MultiPart(String payload){
        fromPayload(payload);
    }

    public MultiPart(String contentType, String contentId, String contentTransferEncoding, String content){
        mHeaders.put(Constants.HEADER_CONTENT_TYPE, contentType);
        if(contentId!=null){
            mHeaders.put(Constants.HEADER_CONTENT_ID, contentId);
        }
        if(contentTransferEncoding!=null) {
            mHeaders.put(Constants.HEADER_CONTENT_TRANSFER_ENCODING, contentTransferEncoding);
        }
        mContent = content;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String,String>> iter =  mHeaders.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry<String,String> entry = iter.next();
            sb.append(entry.getKey()).append(Constants.HEADER_SEP).append(entry.getValue()).append(Constants.CRLF);
        }
        sb.append(Constants.CRLF);
        sb.append(mContent);
        sb.append(Constants.CRLF);
        return sb.toString();
    }

    private void fromPayload(String mimeContent){
        String[] parts = mimeContent.split(Constants.CRLFCRLF,2);
        parseHeaders(parts[0]);
        mContent = parts[1].substring(0, parts[1].lastIndexOf(Constants.CRLF));
    }

    private void parseHeaders(String headerContent){
        for(String header : headerContent.split(Constants.CRLF)){
            String[] val = header.split(Constants.HEADER_SEP);
            mHeaders.put(val[0].trim(), val[1].trim());
        }
    }

    public String getContentType(){
        return mHeaders.get(Constants.HEADER_CONTENT_TYPE);
    }

    public String getContentTransferEncoding(){
        return mHeaders.get(Constants.HEADER_CONTENT_TRANSFER_ENCODING);
    }

    public String getContentId(){
        return mHeaders.get(Constants.HEADER_CONTENT_ID);
    }

    public String getContent(){
        return mContent;
    }
}
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
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;

import java.util.ArrayList;
import java.util.List;

public class MmsMimeBody implements MimeBody {

    private static final Logger sLogger = Logger.getLogger(MmsMimeBody.class.getSimpleName());

    private static final String C_BOUNDARY = "boundary_";

    private MultiPart mSmilMultipart;
    List<MultiPart> mMultiParts;

    private String mBoundary;

    public MmsMimeBody(){
        mMultiParts = new ArrayList<>();
    }

    public void addMultiPart(String contentType, String contentId, String contentTransferEncoding, String content){
        MultiPart multiPart = new MultiPart(contentType, contentId, contentTransferEncoding, content);
        mMultiParts.add(multiPart);
        if(Constants.CONTENT_TYPE_APP_SMIL.equals(contentType)){
            mSmilMultipart = multiPart;
        }
    }

    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();
        if(mBoundary == null){
            mBoundary = new StringBuilder(C_BOUNDARY).append(System.currentTimeMillis()).toString();
        }

        // content type : multipart/related
        sb.append(Constants.HEADER_CONTENT_TYPE).append(Constants.HEADER_SEP).append(Constants.CONTENT_TYPE_MULTIPART_RELATED).append(";")
                .append(Constants.BOUNDARY).append("\"").append(mBoundary).append("\";");

        List<MultiPart> multiParts = new ArrayList<>(mMultiParts);
        // if present set smil multipart as first
        if(mSmilMultipart!=null){
            sb.append("start=").append(mSmilMultipart.getContentId()).append(";type=").append(mSmilMultipart.getContentType());
            sb.append(Constants.CRLFCRLF);
            sb.append(Constants.BOUDARY_SEP).append(mBoundary).append(Constants.CRLF);
            sb.append(mSmilMultipart);
            sb.append(Constants.CRLF);
            multiParts.remove(mSmilMultipart);
        }
        else{
            sb.append(Constants.CRLFCRLF);
        }

        for(MultiPart multiPart : multiParts){
            sb.append(Constants.BOUDARY_SEP).append(mBoundary).append(Constants.CRLF);
            sb.append(multiPart);
            sb.append(Constants.CRLF);
        }

        sb.append(Constants.BOUDARY_SEP).append(mBoundary).append(Constants.BOUDARY_SEP);
        return sb.toString();
    }

    public void parsePayload(String payload){

        if(sLogger.isActivated()){
            sLogger.debug("Payload :");
            sLogger.debug(payload);
        }

        String[] parts = payload.split(Constants.CRLFCRLF,2);
        String contentType = parts[0];

        if(!checkMultipart(contentType)){
            StringBuilder message = new StringBuilder("This content type is not supported").append(Constants.CRLF).append(payload);
            new RuntimeException(message.toString());
        }
        // remove first boundary and last boundary separator
        int length = Constants.BOUDARY_SEP.length() + mBoundary.length();
        String bodyContent = parts[1].substring(length, parts[1].lastIndexOf(Constants.BOUDARY_SEP));
        checkBodyContent(bodyContent);
    }

    private boolean checkMultipart(String contentType){
        boolean isMultipart = false;
        String values[]= contentType.split(";",2);

        String header= values[0];
        header = header.trim();
        if (Constants.CONTENT_TYPE_MULTIPART_RELATED.equals(header.split(Constants.HEADER_SEP)[1])) {
            isMultipart = true;
        }

        String parameters = values[1];
        for(String param : parameters.split(";")){
            param = param.trim();
            if(param.startsWith(Constants.BOUNDARY)){
                mBoundary = StringUtils.removeQuotes(param.split("=")[1]);
                break;
            }
        }
        return isMultipart && mBoundary!=null;
    }

    private void checkBodyContent(String mimeContent) {
        String delimiter = new StringBuilder(Constants.CRLF).append(Constants.BOUDARY_SEP).append(mBoundary).toString();
        for(String mimePart : mimeContent.split(delimiter)){
            if(mimePart.startsWith(Constants.CRLF)){
                mimePart = mimePart.substring(Constants.CRLF.length());
            }
            if(mimePart.isEmpty()){
                continue;
            }
            addMultiPart(new MultiPart(mimePart));
        }
    }

    private void addMultiPart(MultiPart multiPart){
        mMultiParts.add(multiPart);
    }

    void setBoundary(String boundary){
        mBoundary = boundary;
    }

    public  List<MultiPart> getMultiParts(){
        return mMultiParts;
    }
}

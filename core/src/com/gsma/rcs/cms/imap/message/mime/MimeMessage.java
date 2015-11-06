package com.gsma.rcs.cms.imap.message.mime;

import com.gsma.rcs.cms.Constants;
import java.util.List;

public abstract class MimeMessage {

    List<MimeHeaders> mMimeHeaders;
    MimeBody mMimeBody;

    public void addHeaderPart(MimeHeaders mimeHeaders){
        mMimeHeaders.add(mimeHeaders);
    }

    public void setBodyPart(MimeBody mimeBody){
        mMimeBody = mimeBody;
    }

    public String getBodyPart(){
        return (mMimeBody ==null ? null : mMimeBody.toString());
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(MimeHeaders headerPart : mMimeHeaders){
            sb.append(headerPart).append(Constants.CRLF);
        }
        sb.append(mMimeBody);
        return sb.toString();
    }
}

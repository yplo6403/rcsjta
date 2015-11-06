package com.gsma.rcs.cms.imap.message.mime;

public class SmsMimeBody implements MimeBody{

    private String mContent;

    public SmsMimeBody() {
    }

    @Override
    public String toString() {
        return mContent;
    }

    @Override
    public void parsePayload(String payload) {
        mContent = payload;
    }
}

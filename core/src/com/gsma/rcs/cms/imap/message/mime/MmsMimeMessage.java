package com.gsma.rcs.cms.imap.message.mime;

import com.gsma.rcs.cms.Constants;
import com.sonymobile.rcs.imap.Header;
import com.sonymobile.rcs.imap.IPart;

import java.util.ArrayList;

public class MmsMimeMessage extends MimeMessage implements IPart {

    public MmsMimeMessage() {
        mMimeHeaders = new ArrayList<>();
        mMimeBody = new MmsMimeBody();
    }

    public MmsMimeMessage(String payload) {
        this();
        fromPayload(payload);
    }

    @Override
    public String toPayload() {
        return toString();
    }

    @Override
    public void fromPayload(String payload) {

        String[] parts = payload.split(Constants.CRLFCRLF, 3);
        if (3 == parts.length) {

            MimeHeaders mailHeaders = new MimeHeaders();
            for (Header header : Header.parseHeaders(parts[0]).values()) {
                mailHeaders.addHeader(header.getKey(), header.getValue());
            }
            mMimeHeaders.add(mailHeaders);

            MimeHeaders cpimHeaders = new MimeHeaders();
            for (Header header : Header.parseHeaders(parts[1]).values()) {
                cpimHeaders.addHeader(header.getKey(), header.getValue());
            }
            mMimeHeaders.add(cpimHeaders);
            mMimeBody.parsePayload(parts[2]);
        }
    }

    public MmsMimeBody getMimebody() {
        return(MmsMimeBody)mMimeBody;
    }
}

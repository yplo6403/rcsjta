package com.gsma.rcs.cms.imap.message.mime;

public interface MimeBody {
    String toString();
    void parsePayload(String payload);
}

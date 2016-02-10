
package com.gsma.rcs.imaplib.imap;

public interface IPart {

    public String toPayload();

    public void fromPayload(String payload);
}

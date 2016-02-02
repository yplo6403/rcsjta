
package com.sonymobile.rcs.imap;

public interface IPart {

    public String toPayload();

    public void fromPayload(String payload);
}

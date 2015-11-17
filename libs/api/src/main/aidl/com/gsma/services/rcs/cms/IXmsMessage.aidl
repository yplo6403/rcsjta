// IXmsMessage.aidl
package com.gsma.services.rcs.cms;

import com.gsma.services.rcs.contact.ContactId;
import android.net.Uri;

interface IXmsMessage {

    String getMessageId();

    ContactId getRemoteContact();

    String getMimeType();

    int getDirection();

    long getTimestamp();

    long getTimestampSent();

    long getTimestampDelivered();

    int getState();

    int getReasonCode();

    boolean isRead();

    String getBody();

}

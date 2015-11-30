package com.gsma.rcs.cms.event;

import com.gsma.services.rcs.contact.ContactId;

public interface IRcsXmsEventListener {
    void onReadRcsMessage(String messageId);
    void onDeleteRcsMessage(String messageId);
    void onReadRcsConversation(ContactId contact);
    void onDeleteRcsConversation(ContactId contact);
    void onDeleteAll();
}

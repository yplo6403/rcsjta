package com.gsma.rcs.cms.event;

public interface RcsXmsEventListener {
    void onReadRcsMessage(String contact, String baseId);
    void onReadRcsConversation(String contact);
    void onDeleteRcsSms(String contact, String baseId);
    void onDeleteRcsMms(String contact, String baseId, String mms_id);
    void onDeleteRcsConversation(String contact);
}

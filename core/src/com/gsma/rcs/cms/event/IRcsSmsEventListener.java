package com.gsma.rcs.cms.event;

public interface IRcsSmsEventListener {   
    public void onReadRcsMessage(String contact, String baseId);
    public void onReadRcsConversation(String contact);
    public void onDeleteRcsSms(String contact, String baseId);
    public void onDeleteRcsConversation(String contact);
}

package com.gsma.rcs.cms.imap.message;

import com.sonymobile.rcs.imap.IPart;

public interface IImapMessage {
    
    public String getFolder();
    
    public Integer getUid();
    
    public IPart getPart();
    
    public boolean isSeen();
    
    public boolean isDeleted();

}

package com.gsma.rcs.cms.imap.service;

/**
 *
 */
public class ImapServiceNotAvailableException extends Exception {

    
    public ImapServiceNotAvailableException(String message ){
        super(message);
    }
    /**
     * @param e
     */
    public ImapServiceNotAvailableException(Exception e ){
        super(e);
        e.printStackTrace();
    }
}

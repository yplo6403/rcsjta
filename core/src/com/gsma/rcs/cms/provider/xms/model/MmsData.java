package com.gsma.rcs.cms.provider.xms.model;

import com.gsma.services.rcs.RcsService.Direction;

import android.net.Uri;

public class MmsData extends AbstractXmsData {
    
    String mSubject;
    Uri mAttachment;
    
    public MmsData(long nativeProviderId, String contact, String messageId, String content, String subject, Uri attachment, long date, Direction direction) {
        super(nativeProviderId, contact, content, date, direction);
        this.mSubject = subject;
        this.mAttachment = attachment;
        mMimeType = MimeType.MMS;
    };
    
//    public MyMmsMessage(String nativeProviderId, String contact, String content, String subject, Uri attachment, long date, Direction direction,
//            String messageId, boolean read, boolean readStatus) {
//        super(nativeProviderId, contact, content, date, direction,
//                messageId, read, readStatus);
//        this.mSubject = subject;
//        this.mAttachment = attachment;
//        mMimeType = MimeType.MMS;
//    };
    
    public String getSubject(){
        return mSubject;
    }

    public Uri getAttachment(){
        return mAttachment;
    }
}

package com.gsma.rcs.cms.provider.xms.model;

import com.gsma.rcs.cms.utils.HeaderCorrelatorUtils;
import com.gsma.services.rcs.RcsService.Direction;

public class SmsData extends AbstractXmsData {
    
    private String mMessageCorrelator;
    
    public SmsData(String baseId, String contact){
        super();
        mBaseId = baseId;
        mContact = contact;
    }
    
    public SmsData(String baseId, Long nativeProviderId){
        super();
        mBaseId = baseId;
        mNativeProviderId = nativeProviderId;
    }

    public SmsData(String baseId, String contact, ReadStatus readStatus, DeleteStatus deleteStatus){
        super();
        mBaseId = baseId;
        mContact = contact;
        mReadStatus = readStatus;
        mDeleteStatus = deleteStatus;        
    } 

    public SmsData(Long nativeProviderId, String contact, String content, long date, Direction direction, ReadStatus readStatus) {
        super(nativeProviderId, contact, content, date, direction);
        mReadStatus = readStatus;
        mMimeType = MimeType.SMS;          
        mMessageCorrelator = HeaderCorrelatorUtils.buildHeader(content);
    };
    
    public SmsData(Long nativeProviderId, String contact, String content, long date, Direction direction, ReadStatus readStatus, String messageCorrelator) {
        super(nativeProviderId, contact, content, date, direction);
        mReadStatus = readStatus;
        mMimeType = MimeType.SMS;  
        mMessageCorrelator = messageCorrelator;
    };
    
    public SmsData(String baseId,  Long nativeProviderId, String contact, String content, long date, long deliveryDate,  Direction direction, ReadStatus readStatus, DeleteStatus deleteStatus, String messageCorrelator) {
        super(nativeProviderId, contact, content, date, direction);
        mReadStatus = readStatus;
        mMimeType = MimeType.SMS;  
        mMessageCorrelator = messageCorrelator;
        mBaseId = baseId;
        mDeleteStatus = deleteStatus;
        mDeliveryDate = deliveryDate;
    };
        
    public String getMessageCorrelator() {
        return mMessageCorrelator;        
    }
}

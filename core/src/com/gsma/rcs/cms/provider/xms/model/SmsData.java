package com.gsma.rcs.cms.provider.xms.model;

import com.gsma.rcs.cms.utils.HeaderCorrelatorUtils;
import com.gsma.services.rcs.RcsService.Direction;

public class SmsData extends XmsData {
    
    private String mMessageCorrelator;

    public SmsData(){
        super();
    }

    private SmsData(Long nativeProviderId, Long nativeThreadId, String contact, String content, long date, Direction direction){
        mNativeProviderId = nativeProviderId;
        mNativeThreadId = nativeThreadId;
        mContact = contact;
        mContent = content;
        mDate = date;
        mDirection = direction;
        mMimeType = MimeType.SMS;
    }

    public SmsData(Long nativeProviderId, Long nativeThreadId, String contact, String content, long date, Direction direction, ReadStatus readStatus) {
        this(nativeProviderId, nativeThreadId, contact, content, date, direction);
        mReadStatus = readStatus;
        mMessageCorrelator = HeaderCorrelatorUtils.buildHeader(content);
    };
    
    public SmsData(Long nativeProviderId, Long nativeThreadId, String contact, String content, long date, Direction direction, ReadStatus readStatus, String messageCorrelator) {
        this(nativeProviderId, nativeThreadId, contact, content, date, direction);
        mReadStatus = readStatus;
        mMessageCorrelator = messageCorrelator;
    };
    
    public SmsData(String baseId, Long nativeProviderId, Long nativeThreadId, String contact, String content, long date, long deliveryDate,  Direction direction, ReadStatus readStatus, DeleteStatus deleteStatus, String messageCorrelator) {
        this(nativeProviderId, nativeThreadId, contact, content, date, direction);
        mReadStatus = readStatus;
        mMessageCorrelator = messageCorrelator;
        mBaseId = baseId;
        mDeleteStatus = deleteStatus;
        mDeliveryDate = deliveryDate;
    };

    public String getMessageCorrelator() {
        return mMessageCorrelator;
    }
}

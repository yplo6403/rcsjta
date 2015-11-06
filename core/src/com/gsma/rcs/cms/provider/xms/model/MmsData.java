package com.gsma.rcs.cms.provider.xms.model;

import com.gsma.rcs.cms.Constants;
import com.gsma.services.rcs.RcsService.Direction;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MmsData extends XmsData {

    private static final String CONTACT_SEP = ",";

    String mSubject;
    String mMmsId;
    List<MmsPart> mParts = new ArrayList<>();

    public MmsData(){
        super();
    }

    private MmsData(Long nativeProviderId, Long nativeThreadId, String mmsId, String subject, String content, long date, Direction direction) {
        mNativeProviderId = nativeProviderId;
        mNativeThreadId = nativeThreadId;
        mMmsId = mmsId;
        mDate = date;
        mDirection = direction;
        mMimeType = MimeType.MMS;
        mSubject = subject;
        mContent = content;
    };

    public MmsData(Long nativeProviderId, Long nativeThreadId, String mmsId, TreeSet<String> contacts, String subject, String content, long date, Direction direction) {
        this(nativeProviderId, nativeThreadId, mmsId, subject, content, date, direction);
        mContact =  formatContact(contacts);
    };

    public MmsData(Long nativeProviderId, Long nativeThreadId, String mmsId, TreeSet<String> contacts, String subject, String content, long date, Direction direction, ReadStatus readStatus) {
        this(nativeProviderId, nativeThreadId, mmsId, subject, content, date, direction);
        mContact =  formatContact(contacts);
        mReadStatus = readStatus;
    }

    public MmsData(String baseId, Long nativeProviderId, Long nativeThreadId, String mmsId, String contacts, String subject, String content, long date, Direction direction, ReadStatus readStatus) {
        this(nativeProviderId, nativeThreadId, mmsId, subject, content, date, direction);
        mBaseId = baseId;
        mReadStatus = readStatus;
        mContact = contacts;
    }
    public String getSubject(){
        return mSubject;
    }

//    public Uri getAttachment(){
//        return mAttachment;
//    }

    public String getMmsId(){
        return mMmsId;
    }

    public void setMmsId(String mmsId){
        mMmsId = mmsId;
    }

    public List<MmsPart> getParts(){
        return mParts;
    }

    private String formatContact(TreeSet<String> contacts){
        StringBuilder sb = new StringBuilder();
        int size = contacts.size();
        int i=0;
        for(String contact : contacts){
            sb.append(contact);
            if(i < size-1){
                sb.append(CONTACT_SEP);
            }
            i++;
        }
        return sb.toString();
    }

    public void setParts(List<MmsPart> parts){
        mParts = parts;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MmsData{").append(Constants.CRLF)
        .append("nativeProviderId:").append(mNativeProviderId).append(Constants.CRLF)
        .append("nativeThreadId:").append(mNativeThreadId).append(Constants.CRLF)
        .append("mmsId:").append(mMmsId).append(Constants.CRLF)
        .append("contacts:").append(mContact).append(Constants.CRLF)
        .append("subject:").append(mSubject).append(Constants.CRLF)
        .append("content:").append(mContent).append(Constants.CRLF)
        .append("date:").append(mDate).append(Constants.CRLF)
        .append("direction:").append(mDirection).append(Constants.CRLF)
        .append('}').append(Constants.CRLF);
        return sb.toString();
    }
}

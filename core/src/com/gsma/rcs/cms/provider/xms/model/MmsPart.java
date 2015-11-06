package com.gsma.rcs.cms.provider.xms.model;

public class MmsPart {

    String mBaseId;
    String mNativeId;
    String mContentType;
    String mContentId;
    String mPath;
    byte[] mThumb;
    String mText;

    public MmsPart(String baseId, String nativeId, String contentType, String contentId, String path, String text) {
        mContentType = contentType;
        mContentId = contentId;
        mPath = path;
        mText = text;
        mBaseId = baseId;
        mNativeId = nativeId;
    }

    public MmsPart(String baseId, String nativeId, String contentType, String contentId, String path, String text, byte[] thumb) {
        this(baseId, nativeId, contentType, contentId, path, text);
        mThumb = thumb;
    }

    public String getContentType() {
        return mContentType;
    }

    public String getContentId() {
        return mContentId;
    }

    public String getPath() {
        return mPath;
    }

    public String getText() {
        return mText;
    }

    public byte[] getThumb(){
        return mThumb;
    }

    public String getNativeId(){
       return mNativeId;
    }

    public String getBaseId(){
        return mBaseId;
    }
}
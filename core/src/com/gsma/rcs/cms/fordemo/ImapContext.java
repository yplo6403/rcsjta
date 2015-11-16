package com.gsma.rcs.cms.fordemo;

import java.util.HashMap;
import java.util.Map;

public class ImapContext {

    private static final String SEP = "_##_";

    // key baseId, value uid
    Map<String,Integer> mUids = new HashMap<>();

    // key folderName_##_uid, value baseId
    Map<String,String> mBaseIds = new HashMap<>();


    public void addNewEntry(String folderName, Integer uid, String baseId){
        mUids.put(baseId, uid);
        mBaseIds.put(new StringBuilder(folderName).append(SEP).append(uid).toString(), baseId);
    }

    public  Map<String,Integer> getUids(){
        return mUids;
    }

    public  Map<String,String> getBaseIds(){
        return mBaseIds;
    }

    public  String getBaseId(String folderName, Integer uid ){
        return mBaseIds.get(new StringBuilder(folderName).append(SEP).append(uid).toString());
    }

    public Integer getUid(String baseId){
        return mUids.get(baseId);
    }

    public void importLocalContext(ImapContext context){
        mUids.putAll(context.getUids());
        mBaseIds.putAll(context.getBaseIds());
    }
}

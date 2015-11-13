package com.gsma.rcs.cms.fordemo;

import java.util.HashMap;
import java.util.Map;

public class ImapContext {

    private static final String SEP = "_##_";

    // key baseId, value uid
    Map<String,Integer> mNewUids = new HashMap<>();
    // key baseId, value uid
    Map<String,Integer> mUids = new HashMap<>();

    // key folderName_##_uid, value baseId
    Map<String,String> mNewBaseIds = new HashMap<>();
    Map<String,String> mBaseIds = new HashMap<>();


    public void addNewEntry(String folderName, Integer uid, String baseId){
        mNewUids.put(baseId, uid);
        mNewBaseIds.put(new StringBuilder(folderName).append(SEP).append(uid).toString(), baseId);
    }

    public void saveNewEntries(){
        mUids.putAll(mNewUids);
        mBaseIds.putAll(mNewBaseIds);
        mNewUids.clear();
        mNewBaseIds.clear();
    }

    public  Map<String,Integer> getNewUids(){
        return mNewUids;
    }

    public  String getNewBaseId(String folderName, Integer uid ){
        return mNewBaseIds.get(new StringBuilder(folderName).append(SEP).append(uid).toString());
    }

    public Integer getUid(String baseId){
        return mUids.get(baseId);
    }
}

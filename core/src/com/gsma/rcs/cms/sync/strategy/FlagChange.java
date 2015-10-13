
package com.gsma.rcs.cms.sync.strategy;

import com.sonymobile.rcs.imap.Flag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("javadoc")
public class FlagChange {

    public enum Operation {ADD_FLAG, REMOVE_FLAG};
    
    private String mFolder;
    private Integer mUid;
    private Set<Flag> mFlags;
    private Operation mOperation = Operation.ADD_FLAG;
    
    /**
     * @param folder 
     * @param uid
     * @param seen
     * @param deleted
     */
    public FlagChange(String folder, Integer uid, Set<Flag> flags) {
        super();
        mFolder = folder;
        mUid = uid;
        mFlags = flags;
    }

    public FlagChange(String folder, Integer uid, Flag flag, Operation operation) {
        super();
        mFolder = folder;
        mUid = uid;
        mFlags = new HashSet<Flag>(Arrays.asList(flag));
        mOperation = operation;
    }
    
    public Integer getUid() {
        return mUid;
    }

    public Boolean addSeenFlag() {
        return mOperation==Operation.ADD_FLAG && mFlags.contains(Flag.Seen);
    }

    public Boolean addDeletedFlag() {
        return mOperation==Operation.ADD_FLAG && mFlags.contains(Flag.Deleted);
    }

    public Boolean removeSeenFlag() {
        return mOperation==Operation.REMOVE_FLAG && mFlags.contains(Flag.Seen);
    }

    public Boolean removeDeletedFlag() {
        return mOperation==Operation.REMOVE_FLAG && mFlags.contains(Flag.Deleted);
    }

    public String getFolder() {
        return mFolder;
    }
    
    public Flag[] getFlags(){
        return mFlags.toArray(new Flag[mFlags.size()]);
    }
    
    public Operation getOperation(){
        return mOperation;
    }

}

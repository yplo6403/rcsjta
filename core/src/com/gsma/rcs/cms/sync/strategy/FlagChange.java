
package com.gsma.rcs.cms.sync.strategy;

import com.gsma.rcs.cms.utils.ListUtils;

import com.sonymobile.rcs.imap.Flag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("javadoc")
public class FlagChange {

    public enum Operation {
        ADD_FLAG, REMOVE_FLAG
    };

    private String mFolder;
    private List<Integer> mUids;
    private Set<Flag> mFlags;
    private Operation mOperation = Operation.ADD_FLAG;

    /**
     * @param folder
     * @param uids
     * @param flags
     */
    public FlagChange(String folder, List<Integer> uids, Set<Flag> flags) {
        super();
        mFolder = folder;
        mUids = uids;
        mFlags = flags;
    }

    /**
     * @param folder
     * @param uids
     * @param flag
     */
    public FlagChange(String folder, List<Integer> uids, Flag flag) {
        super();
        mFolder = folder;
        mUids = uids;
        mFlags = new HashSet<>(Arrays.asList(flag));
    }

    public FlagChange(String folder, Integer uid, Flag flag, Operation operation) {
        super();
        mFolder = folder;
        mUids = Arrays.asList(new Integer[] {
            uid
        });
        mFlags = new HashSet<>(Arrays.asList(flag));
        mOperation = operation;
    }

    public String getJoinedUids() {
        return ListUtils.join(mUids, ",");
    }

    public List<Integer> getUids() {
        return mUids;
    }

    public Boolean addSeenFlag() {
        return mOperation == Operation.ADD_FLAG && mFlags.contains(Flag.Seen);
    }

    public Boolean addDeletedFlag() {
        return mOperation == Operation.ADD_FLAG && mFlags.contains(Flag.Deleted);
    }

    public Boolean removeSeenFlag() {
        return mOperation == Operation.REMOVE_FLAG && mFlags.contains(Flag.Seen);
    }

    public Boolean removeDeletedFlag() {
        return mOperation == Operation.REMOVE_FLAG && mFlags.contains(Flag.Deleted);
    }

    public String getFolder() {
        return mFolder;
    }

    public Flag[] getFlags() {
        return mFlags.toArray(new Flag[mFlags.size()]);
    }

    public Operation getOperation() {
        return mOperation;
    }

}

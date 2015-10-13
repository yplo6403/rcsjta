
package com.gsma.rcs.cms.imap;

import com.gsma.rcs.cms.Constants;

import java.util.Map;

public class ImapFolder {

    private String mName;
    private Map<String, String> mCounters;

    /**
     * Default constructor
     * 
     * @param name
     * @param counters
     */
    public ImapFolder(String name, Map<String, String> counters) {
        super();
        mName = name;
        mCounters = counters;
    }

    /**
     * @return name
     */
    public String getName() {
        return mName;
    }

    /** public */
    void addCounter(String counter, String value) {
        mCounters.put(counter, value);
    }

    /** public */
    void addCounters(Map<String, String> counters) {
        mCounters.putAll(counters);
    }

    /**
     * @return uidvalidity
     */
    public Integer getUidValidity() {
        return getValueAsInteger(Constants.METADATA_UIDVALIDITY);
    }

    /**
     * @return HighestModseq
     */
    public Integer getHighestModseq() {
        return getValueAsInteger(Constants.METADATA_HIGHESTMODSEQ);
    }

    /**
     * @return UidNext
     */
    public Integer getUidNext() {
        return getValueAsInteger(Constants.METADATA_UIDNEXT);
    }

    @Override
    public String toString() {
        return new StringBuilder("ImapFolder [mName=").append(mName).append(", ")
                .append(Constants.METADATA_UIDVALIDITY).append("=").append(getUidValidity())
                .append(", ").append(Constants.METADATA_HIGHESTMODSEQ).append("=")
                .append(getHighestModseq()).append(", ").append(Constants.METADATA_UIDNEXT)
                .append("=").append(getUidNext()).append(", ").append("]").toString();

    }

    private Integer getValueAsInteger(String key) {
        String val = mCounters.get(key);
        return (val == null) ? null : Integer.valueOf(val);
    }
}

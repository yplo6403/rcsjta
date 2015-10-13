
package com.gsma.rcs.cms.imap.cmd;

import com.sonymobile.rcs.imap.Flag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CmdUtils {

    // Example:
    // * 1 FETCH (FLAGS (\Seen) INTERNALDATE "21-Jul-2014 13:20:08 +0000" RFC822.SIZE 66 ENVELOPE
    // (NIL NIL (("Joe" NIL "joe" "sony.com")) (("Joe" NIL "joe" "sony.com")) (("Joe" NIL "joe"
    // "sony.com")) NIL NIL NIL NIL NIL) BODY ("TEXT" "PLAIN" ("charset" "us-ascii") NIL NIL "7BIT"
    // 39 1))
    /**
     * Parse metadata from imap response
     * 
     * @param l line
     * @return Map<String, String>
     */
    public static Map<String, String> parseMetadata(String l) {
        Map<String, String> map = new HashMap<String, String>();

        l = l.substring(l.indexOf('(') + 1, l.lastIndexOf(')')) + " ";

        char[] arr = l.toCharArray();

        String k = "";
        String v = "";

        int mode = 0; // 0 reading key, 1 reading value, 2 reading plain value, 3 reading quoted
                      // value, 4 reading parenthesis value
        int par = 0;
        for (char c : arr) {
            switch (mode) {
                case 0:
                    if (c == ' ' && k.length() > 0) {
                        // done reading key
                        mode = 1;
                    } else if (c != ' ') {
                        k += c;
                    }
                    break;
                case 1:
                    if (c == '"') {
                        mode = 3;
                    } else if (c == '(') {
                        mode = 4;
                        par = -1;
                    } else {
                        v += c;
                        mode = 2;
                    }
                    break;
                case 2:
                    if (c == ' ') {
                        mode = 0;
                        map.put(k, v);
                        k = "";
                        v = "";
                    } else {
                        v += c;
                    }
                    break;
                case 3:
                    if (c == '"') {
                        map.put(k, v);
                        k = "";
                        v = "";
                        mode = 0;
                    } else {
                        v += c;
                    }
                    break;
                case 4:
                    if (c == '(') {
                        v += c;
                        par--;
                    } else if (c == ')') {
                        par++;
                        if (par == 0) {
                            map.put(k, v);
                            k = "";
                            v = "";
                            mode = 0;
                        } else {
                            v += c;
                        }
                    } else {
                        v += c;
                    }
                    break;
            }
        }
        return map;
    }

    /**
     * @param flags
     * @param rawValue
     */
    public static void fillFlags(Set<Flag> flags, String rawValue) {
        String[] sflags = rawValue.split(" ");
        for (String f : sflags) {
            if (f.startsWith("\\")) {
                f = f.substring(1);
                flags.add(Flag.valueOf(f));
            }
        }
    }

    /**
     * Parse flag from raw data
     * 
     * @param rawValue
     * @return Set<Flag>
     */
    public static Set<Flag> parseFlags(String rawValue) {
        Set<Flag> flags = new HashSet<Flag>();
        for (String f : rawValue.split(" ")) {
            if (f.startsWith("\\")) {
                f = f.substring(1);
                flags.add(Flag.valueOf(f));
            }
        }
        return flags;
    }
}

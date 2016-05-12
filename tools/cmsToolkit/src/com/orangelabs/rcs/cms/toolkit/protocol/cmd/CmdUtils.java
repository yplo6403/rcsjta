/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.orangelabs.rcs.cms.toolkit.protocol.cmd;

import com.gsma.rcs.imaplib.imap.Flag;

import java.util.HashSet;
import java.util.Set;

public class CmdUtils {

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

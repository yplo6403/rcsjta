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

package com.gsma.rcs.core.cms.protocol.cmd;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.imaplib.imap.Part;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated class for handling LIST STATUS IMAP command
 */
public class ListCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_LIST;
    private static final String sPattern = "^LIST \\(.*\\) \"/\" (.*)$";

    private static final int sExpectedValues = 1;

    final List<String> mFolders = new ArrayList();

    @Override
    public String buildCommand(Object... params) {
        return sCommand;
    }

    @Override
    public boolean handleLine(String oneLine) {
        return false;
    }

    @Override
    public void handleLines(List<String> lines) {

        for (String line : lines) {
            line = line.substring(2).trim();

            String[] values = extractCounterValuesFromLine(sPattern, line);

            if (values == null || values.length != sExpectedValues) {
                continue;
            }

            mFolders.add(values[0]);
        }
    }

    @Override
    public void handlePart(Part part) {
    }

    @Override
    public List<String> getResult() {
        return mFolders;
    }
}

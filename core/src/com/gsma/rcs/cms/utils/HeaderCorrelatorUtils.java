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

package com.gsma.rcs.cms.utils;

import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.logger.Logger;

import java.io.UnsupportedEncodingException;

public class HeaderCorrelatorUtils {

    private final static Logger sLogger = Logger.getLogger(HeaderCorrelatorUtils.class
            .getSimpleName());

    // TODO FGI : we have to finish to implement the message-correlator header algorithm
    private static final int LIMIT_LENGTH = 160;

    /* package private */static final String PREFIX = "=?utf-8?b?";
    /* package private */static final String SUFFIX = "?=";

    public static String buildHeader(String content) {
        if (content == null) {
            return "";
        }
        int length = content.length();
        if (length > LIMIT_LENGTH) {
            length = LIMIT_LENGTH;
        }
        content = content.substring(0, length);
        content = content.replaceAll("(\\r|\\n)", " ");

        for (char c : content.toCharArray()) {
            if (c > 128) {
                // no US ASCII character : base 64 encoding
                try {
                    return PREFIX
                            + new String(Base64.encodeBase64(content.getBytes("UTF-8")), "UTF-8")
                            + SUFFIX;
                } catch (UnsupportedEncodingException e) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Could not decode content '" + content + "'");
                    }
                    return "";
                }
            }
        }
        return content;
    }
}

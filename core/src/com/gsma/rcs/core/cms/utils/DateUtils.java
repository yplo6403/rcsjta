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

package com.gsma.rcs.core.cms.utils;

import com.gsma.rcs.utils.logger.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateUtils {

    private final static Logger sLogger = Logger.getLogger(DateUtils.class.getSimpleName());

    public static final String CMS_IMAP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    public static final String CMS_CPIM_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private static final String MMS_FILE_DATE_FORMAT = "yyyyMMdd_HHmmss";

    public static String getDateAsString(long date, String format) {
        return new SimpleDateFormat(format, Locale.US).format(date);
    }

    public static String getMmsFileDate(long date) {
        return new SimpleDateFormat(MMS_FILE_DATE_FORMAT, Locale.US).format(date);
    }

    public static Long parseDate(String date, String format) {
        long d = -1;
        try {
            d = new SimpleDateFormat(format, Locale.US).parse(date).getTime();

        } catch (ParseException e) {
            if (sLogger.isActivated()) {
                sLogger.warn("Invalid date: " + date);
            }
        }
        return d;
    }
}
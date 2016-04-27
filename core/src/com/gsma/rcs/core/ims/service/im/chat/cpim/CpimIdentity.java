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
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat.cpim;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable class to decode the content of the CPIM identity header according to RFC3862
 * 
 * @author Philippe LEMORDANT
 */
public class CpimIdentity {
    /**
     * Pattern to extract display name and Uri from CPIM 'From' header using a regular expression of
     * the CPIM 'From' header.
     * <p>
     * Extract of RFC3862 <b>Common Presence and Instant Messaging (CPIM): Message Format</b> <br>
     * From-header = "From" ": " [ Formal-name ] "<" URI ">" ; "From" is case-sensitive
     */
    private final static Pattern PATTERN_URI_WITH_OPTIONAL_DISPLAY_NAME = Pattern
            .compile("^\\s*?\"?([^\"<]*)\"?\\s*?<([^>]*)>$");

    /**
     * Pattern to extract the Accept-contact tag.
     */
    private final static Pattern PATTERN_ACCEPT_CONTACT = Pattern
            .compile("\\?Accept-Contact=([^\\?]*)");

    /**
     * Pattern to extract the sip.instance tag.
     */
    private final static Pattern PATTERN_SIP_INSTANCE = Pattern
            .compile("\\+sip.instance=\"([^\"]*)\"");

    /**
     * The optional display name (may be null)
     */
    private final String mDisplayName;

    private final String mUri;

    /**
     * The SIP instance (may be null)
     */
    private String mSipInstance;

    /**
     * Constructor
     * 
     * @param identity the CPIM identity
     */
    public CpimIdentity(final String identity) {
        if (identity == null)
            throw new IllegalArgumentException("Null argument");
        Matcher matcher = PATTERN_URI_WITH_OPTIONAL_DISPLAY_NAME.matcher(identity);
        if (matcher.find()) {
            String result = matcher.group(1).trim();
            mDisplayName = (result.length() == 0) ? null : result;
            mUri = matcher.group(2);
            // Extract the SIP instance (if any)
            String acceptContact = getAcceptContact();
            if (acceptContact != null) {
                matcher = PATTERN_SIP_INSTANCE.matcher(acceptContact);
                if (matcher.find()) {
                    mSipInstance = matcher.group(1);
                }
            }
            return;
        }
        throw new IllegalArgumentException("Invalid uri: " + identity);
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getUri() {
        return mUri;
    }

    public String getSipInstance() {
        return mSipInstance;
    }

    private String getAcceptContact() {
        try {
            Matcher matcher = PATTERN_ACCEPT_CONTACT.matcher(mUri);
            if (matcher.find()) {
                return URLDecoder.decode(matcher.group(1), "UTF-8");
            }
            return null;

        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "CpimIdentity [displayName=" + mDisplayName + ", uri=" + mUri + ", SIP instance="
                + mSipInstance + "]";
    }

}

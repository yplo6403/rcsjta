/*
 *
 * MmsLib
 * 
 * Module name: com.orange.labs.mms
 * Version:     2.0
 * Created:     2013-06-06
 * Author:      vkxr8185 (Yoann Hamon)
 * 
 * Copyright (C) 2013 Orange
 *
 * This software is confidential and proprietary information of France Telecom Orange.
 * You shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 */

package com.orange.labs.mms;

import com.gsma.services.rcs.contact.ContactId;
import com.orange.labs.mms.priv.PartMMS;

import java.util.List;

public final class MmsMessage {

    private final String mSubject;
    private final ContactId mFrom;
    private final List<ContactId> mTo;
    private final List<PartMMS> mParts;

    public MmsMessage(ContactId contact, List<ContactId> contacts, String subject, List<PartMMS> parts) {
        mFrom = contact;
        mTo = contacts;
        mSubject = subject;
        mParts = parts;
    }

    /**
     * Return a list of recipients
     *
     * @return A list of recipients
     */
    public List<ContactId> getTo() {
        return mTo;
    }

    /**
     * Return the subject of the message
     *
     * @return The subject (if any)
     */
    public String getSubject() {
        return mSubject;
    }

    /**
     * Return a list of the parts included in the message
     *
     * @return A set of part
     */
    public List<PartMMS> getParts() {
        return mParts;
    }

    public ContactId getFrom() {
        return mFrom;
    }

}

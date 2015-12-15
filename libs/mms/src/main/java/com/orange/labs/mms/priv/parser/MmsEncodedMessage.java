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

package com.orange.labs.mms.priv.parser;

import com.gsma.services.rcs.contact.ContactId;
import com.orange.labs.mms.MmsMessage;
import com.orange.labs.mms.priv.MmsFormatException;
import com.orange.labs.mms.priv.PartMMS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MmsEncodedMessage {

    private final String mTransactionId;
    private final long mDate;
    private final MmsMessage mMmsMessage;
    private final String mFrom;
    private final List<String> mTo;

    public MmsEncodedMessage(MmsMessage msg) {
        mMmsMessage = msg;
        Random r = new Random();
        mTransactionId = String.valueOf(r.nextInt(999999999));
        mDate = System.currentTimeMillis();
        mFrom = mMmsMessage.getFrom().toString().concat("/TYPE=PLMN");
        mTo = new ArrayList<>();
        for (ContactId contact : msg.getTo()) {
            mTo.add(contact.toString().concat("/TYPE=PLMN"));
        }
    }

    public long getDate() {
        return mDate;
    }

    public List<PartMMS> getParts() {
        return mMmsMessage.getParts();
    }

    public String getFrom() {
        return mFrom;
    }

    public List<String> getTo() {
        return mTo;
    }

    public String getSubject() {
        return mMmsMessage.getSubject();
    }

    public String getTransactionId() {
        return mTransactionId;
    }

    public String getContentType() {
        return "application/vnd.wap.multipart.mixed";
    }

    public int getType() {
        return MmsEncoder.MESSAGE_TYPE_SEND_REQ;
    }

    public byte[] encode() throws MmsFormatException {
        MmsEncoder mmsEncoder = new MmsEncoder(this);
        return mmsEncoder.encode();
    }
}

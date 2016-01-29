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

package com.gsma.rcs.core.cms.xms.mms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class MmsEncodedMessage {

    private final String mTransactionId;
    private final long mDate;
    private final String mFrom;
    private final String mSubject;
    private final List<String> mTo;
    private final List<MmsDataObject.MmsPart> mParts;
    private final Context mCtx;

    public MmsEncodedMessage(Context ctx, ContactId sender, ContactId recipient, String subject,
            String transactionId, List<MmsDataObject.MmsPart> parts) {
        mFrom = sender.toString().concat("/TYPE=PLMN");
        mTo = new ArrayList<>();
        mTo.add(recipient.toString().concat("/TYPE=PLMN"));
        mSubject = subject;
        mTransactionId = transactionId;
        mDate = System.currentTimeMillis();
        mParts = parts;
        mCtx = ctx;
    }

    public long getDate() {
        return mDate;
    }

    public List<MmsDataObject.MmsPart> getParts() {
        return mParts;
    }

    public String getFrom() {
        return mFrom;
    }

    public List<String> getTo() {
        return mTo;
    }

    public String getSubject() {
        return mSubject;
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

    public byte[] encode() throws MmsFormatException, FileAccessException {
        MmsEncoder mmsEncoder = new MmsEncoder(mCtx, this);
        return mmsEncoder.encode();
    }
}

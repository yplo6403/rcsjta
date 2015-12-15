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

package com.orange.labs.mms.priv;

public final class PartMMS {

    private final String mMimeType;
    private final byte[] mContent;

    public PartMMS(String mimeType, byte[] content) {
        mMimeType = mimeType;
        mContent = content;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public byte[] getContent() {
        return mContent;
    }
}

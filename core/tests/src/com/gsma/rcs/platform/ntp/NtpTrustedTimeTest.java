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

package com.gsma.rcs.platform.ntp;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.content.Context;
import android.test.AndroidTestCase;

import junit.framework.Assert;

public class NtpTrustedTimeTest extends AndroidTestCase {

    private RcsSettings mRcsSettings;
    private Context  mContext;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mRcsSettings = RcsSettings.getInstance(new LocalContentResolver(mContext.getContentResolver()));
        AndroidFactory.setApplicationContext(mContext, mRcsSettings);
    }


    public void test(){

        long testOffset = 2L; //2ms : acceptable offset due to test execution

        Assert.assertTrue(Math.abs(System.currentTimeMillis()-NtpTrustedTime.currentTimeMillis()) < testOffset);

        NtpTrustedTime ntpTrustedTime = NtpTrustedTime.createInstance(getContext(), mRcsSettings);
        long localOffset = mRcsSettings.getNtpLocalOffset();
        Assert.assertTrue(Math.abs(System.currentTimeMillis() + localOffset - NtpTrustedTime.currentTimeMillis()) < testOffset);

        if(ntpTrustedTime.forceRefresh()){
            localOffset = mRcsSettings.getNtpLocalOffset();
            Assert.assertTrue(Math.abs(System.currentTimeMillis() + localOffset - NtpTrustedTime.currentTimeMillis()) < testOffset);
        }
    }

}

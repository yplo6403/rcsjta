/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.cms.sync.adapter;

import com.gsma.rcs.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

/**
 * RCS account manager
 */
public class CmsAccountManager {

    private static volatile CmsAccountManager sInstance;
    private Account mAccount;

    /**
     * Constructor
     * 
     * @param context Application context
     */
    private CmsAccountManager(Context context) throws CmsAccountException {

        String accountName = context.getString(R.string.rcs_cms_account_name);
        String accountType = context.getString(R.string.rcs_cms_account_type);

        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(accountType);
        if (accounts.length > 0) {
            mAccount = accounts[0];
            return;
        }

        mAccount = new Account(accountName, accountType);
        if (accountManager.addAccountExplicitly(mAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in in your <provider> element in the
             * manifest, then call context.setIsSyncable(account, AUTHORITY, 1) here.
             */
        } else {
            throw new CmsAccountException("Failed to create Cms account");
        }
    }

    /**
     * Creates a singleton instance of RcsAccountManager
     * 
     * @param context Application context
     * @param contactManager accessor for contact provider
     * @return singleton instance of RcsAccountManager
     */
    public static CmsAccountManager createInstance(Context context) throws CmsAccountException {
        synchronized (CmsAccountManager.class) {
            if (sInstance == null) {
                sInstance = new CmsAccountManager(context);
            }
            return sInstance;
        }
    }

    public Account getAccount() {
        return mAccount;
    }
}

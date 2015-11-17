package com.gsma.rcs.cms.integration;

import com.gsma.rcs.cms.provider.settings.CmsSettings;

import android.content.Context;

public class CmsSettingsMock{
    
    private static String mServerAddress = "imap://172.20.65.102";
    private static String mUserLogin = "test_integration";
    private static String mUserPwd = "test_integration";
    private static String mMyNumber = "myNumber";
    private static String mDefaultDirectory = "Default";
    private static String mDirectorySeparator = "/";
    
    
    public static CmsSettings getCmsSettings(Context context){        
        CmsSettings settings = CmsSettings.createInstance(context);
        settings.setMyNumber(mMyNumber);
        settings.setServerAddress(mServerAddress);
        settings.setUserLogin(mUserLogin);
        settings.setUserPwd(mUserPwd);
        settings.setCmsDefaultDirectory(mDefaultDirectory);
        settings.setCmsDirectorySeparator(mDirectorySeparator);
        return settings;       
    }

}

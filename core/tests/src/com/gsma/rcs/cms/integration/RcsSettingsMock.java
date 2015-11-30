package com.gsma.rcs.cms.integration;

import android.content.Context;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

public class RcsSettingsMock{
    
    private static String mServerAddress = "imap://172.20.65.102";
    private static String mUserLogin = "test_integration";
    private static String mUserPwd = "test_integration";
    private static String mDefaultDirectory = "Default";
    private static String mDirectorySeparator = "/";
    
    
    public static RcsSettings getRcsSettings(Context context){
        RcsSettings settings = RcsSettings.createInstance(new LocalContentResolver(context));
        settings.setCmsServerAddress(mServerAddress);
        settings.setCmsUserLogin(mUserLogin);
        settings.setCmsUserPwd(mUserPwd);
        settings.setCmsDefaultDirectoryName(mDefaultDirectory);
        settings.setCmsDirectorySeparator(mDirectorySeparator);
        return settings;       
    }

}

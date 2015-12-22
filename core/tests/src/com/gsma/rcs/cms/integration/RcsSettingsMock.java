package com.gsma.rcs.cms.integration;

import android.content.Context;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

public class RcsSettingsMock{
    
    private static final String mServerAddress = "imap://172.20.65.102";
    private static final String mUserLogin = "test_integration";
    private static final String mUserPwd = "test_integration";
    private static final String mDefaultDirectory = "Default";
    private static final String mDirectorySeparator = "/";
    private static final String mContact = "+33601020304";

    private static String mOriServerAddress;
    private static String mOriUserLogin;
    private static String mOriUserPwd;
    private static String mOriDefaultDirectory;
    private static String mOriDirectorySeparator;
    private static ContactId mOriContact;

    public static RcsSettings getMockSettings(Context context){
        RcsSettings settings = RcsSettings.createInstance(new LocalContentResolver(context));

        mOriServerAddress = settings.getCmsServerAddress();
        mOriUserLogin = settings.getCmsUserLogin();
        mOriUserPwd = settings.getCmsUserPwd();
        mOriDefaultDirectory = settings.getCmsDefaultDirectoryName();
        mOriDirectorySeparator = settings.getCmsDirectorySeparator();
        mOriContact = settings.getUserProfileImsUserName();

        settings.setCmsServerAddress(mServerAddress);
        settings.setCmsUserLogin(mUserLogin);
        settings.setCmsUserPwd(mUserPwd);
        settings.setCmsDefaultDirectoryName(mDefaultDirectory);
        settings.setCmsDirectorySeparator(mDirectorySeparator);
        settings.setUserProfileImsUserName(ContactUtil.createContactIdFromTrustedData("+33601020304"));
        return settings;
    }

    public static void restoreSettings(){
        RcsSettings settings = RcsSettings.createInstance(null);
        settings.setCmsServerAddress(mOriServerAddress);
        settings.setCmsUserLogin(mOriUserLogin);
        settings.setCmsUserPwd(mOriUserPwd);
        settings.setCmsDefaultDirectoryName(mOriDefaultDirectory);
        settings.setCmsDirectorySeparator(mOriDirectorySeparator);
        settings.setUserProfileImsUserName(mOriContact);
    }

}

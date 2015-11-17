package com.gsma.rcs.cms.provider.settings;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 *
 */
public class CmsSettings {
    
    private static final String WHERE_CLAUSE = new StringBuilder(CmsSettingsData.KEY_KEY).append(
            "=?").toString();

    /**
     * Current instance
     */
    private static volatile CmsSettings sInstance;

    /**
     * Local Content resolver
     */
    final private LocalContentResolver mLocalContentResolver;    

        
    /**
     * Create singleton instance
     * @param context 
     * @return RcsSettings instance
     */
    public static CmsSettings createInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (CmsSettings.class) {
            if (sInstance == null) {
                sInstance = new CmsSettings(context);
            }
            return sInstance;
        }
    }
    
    private CmsSettings(Context context){        
        mLocalContentResolver= new LocalContentResolver(context);
        RcsSettings rcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        AndroidFactory.setApplicationContext(context, rcsSettings);
        
        String param;
        ContactId contactId = rcsSettings.getUserProfileImsUserName();
        String msisdn = null;
        if (contactId!=null) {
            msisdn = contactId.toString();
            if (msisdn.startsWith("+")) {
                msisdn = msisdn.substring(1);
            }
        }
        
      param = readParameter(CmsSettingsData.CMS_IMAP_USER_LOGIN);
      if(CmsSettingsData.DEFAULT_CMS_IMAP_USER_LOGIN.equals(param) && msisdn!=null){
          writeParameter(CmsSettingsData.CMS_IMAP_USER_LOGIN, msisdn);
      }
      param = readParameter(CmsSettingsData.CMS_IMAP_USER_PWD);
      if(CmsSettingsData.DEFAULT_CMS_IMAP_USER_PWD.equals(param) && msisdn!=null){
          writeParameter(CmsSettingsData.CMS_IMAP_USER_PWD, msisdn);
      }
      param = readParameter(CmsSettingsData.CMS_MY_NUMBER);
      if(CmsSettingsData.DEFAULT_CMS_MY_NUMBER.equals(param) && contactId!=null){
          writeParameter(CmsSettingsData.CMS_MY_NUMBER, contactId.toString());
      }        
    }
    
    public static CmsSettings getInstance(){
        return sInstance;
    }
    
    public void setServerAddress(String serverAddress){
        if(!serverAddress.isEmpty()){
            writeParameter(CmsSettingsData.CMS_IMAP_SERVER_ADDRESS, serverAddress);    
        }
    }
    
    public String getServerAddress(){
        return readParameter(CmsSettingsData.CMS_IMAP_SERVER_ADDRESS);
    }
    
    public String getUserLogin(){
        return readParameter(CmsSettingsData.CMS_IMAP_USER_LOGIN);
    }
    
    public void setUserLogin(String userLogin){
        if(!userLogin.isEmpty()){
            writeParameter(CmsSettingsData.CMS_IMAP_USER_LOGIN, userLogin);    
        }
    }

    public String getUserPwd(){
        return readParameter(CmsSettingsData.CMS_IMAP_USER_PWD);
        }
    
    public void setUserPwd(String userPwd){
        if(!userPwd.isEmpty()){
            writeParameter(CmsSettingsData.CMS_IMAP_USER_PWD, userPwd);    
        }
    }
        
    public String getMyNumber(){
        return readParameter(CmsSettingsData.CMS_MY_NUMBER);
        }
    
    public void setMyNumber(String myNumber){
        if(!myNumber.isEmpty()){
            writeParameter(CmsSettingsData.CMS_MY_NUMBER, myNumber);
        }
    }

    public Boolean getPushSms(){
        return Boolean.parseBoolean(readParameter(CmsSettingsData.CMS_PUSH_SMS));
    }

    public void setPushSms(boolean enable){
        writeParameter(CmsSettingsData.CMS_PUSH_SMS, String.valueOf(enable));
    }

    public Boolean getPushMms(){
        return Boolean.parseBoolean(readParameter(CmsSettingsData.CMS_PUSH_MMS));
    }

    public void setPushMms(boolean enable){
        writeParameter(CmsSettingsData.CMS_PUSH_MMS, String.valueOf(enable));
    }

    public Boolean getUpdateFlagsWithImapXms(){
        return Boolean.parseBoolean(readParameter(CmsSettingsData.CMS_UPDATE_FLAGS_WITH_IMAP_XMS));
    }

    public void setUpdateFlagsWithImapXms(boolean update){
        writeParameter(CmsSettingsData.CMS_UPDATE_FLAGS_WITH_IMAP_XMS, String.valueOf(update));
    }

    public String getCmsDefaultDirectory(){
        return readParameter(CmsSettingsData.CMS_DEFAULT_DIRECTORY);
    }

    public void setCmsDefaultDirectory(String defaultDirectory){
        writeParameter(CmsSettingsData.CMS_DEFAULT_DIRECTORY, defaultDirectory);
    }

    public String getCmsDirectorySeparator(){
        return readParameter(CmsSettingsData.CMS_DIRECTORY_SEPARATOR);
    }

    public void setCmsDirectorySeparator(String directorySeparator){
        writeParameter(CmsSettingsData.CMS_DIRECTORY_SEPARATOR, directorySeparator);
    }

    public boolean isEmpty(){
        return getServerAddress().isEmpty() || CmsSettingsData.DEFAULT_CMS_IMAP_SERVER_ADDRESS.equals(getServerAddress()) ||
        getUserLogin().isEmpty() || CmsSettingsData.DEFAULT_CMS_IMAP_USER_LOGIN.equals(getUserLogin()) ||
        getUserPwd().isEmpty() ||  CmsSettingsData.DEFAULT_CMS_IMAP_USER_PWD.equals(getUserPwd())|| 
        getMyNumber().isEmpty() || CmsSettingsData.DEFAULT_CMS_MY_NUMBER.equals(getMyNumber());
    }
    
    /**
     * Read a parameter from database
     * 
     * @param key Key
     * @return Value
     */
    public String readParameter(String key) {
        Cursor c = null;
        try {
            String[] whereArg = new String[] {
                key
            };
            c = mLocalContentResolver.query(CmsSettingsData.CONTENT_URI, null, WHERE_CLAUSE,
                    whereArg, null);
            CursorUtil.assertCursorIsNotNull(c, CmsSettingsData.CONTENT_URI);
            if (!c.moveToFirst()) {
                throw new IllegalArgumentException("Illegal setting key:".concat(key));
            }
            return c.getString(c.getColumnIndexOrThrow(CmsSettingsData.KEY_VALUE));

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    /**
     * Write a string setting parameter to Database
     * 
     * @param key
     * @param value
     * @return the number of rows updated
     */
    public int writeParameter(String key, String value) {
        if (value == null) {
            return 0;
        }
        ContentValues values = new ContentValues();
        values.put(CmsSettingsData.KEY_VALUE, value);
        String[] whereArgs = new String[] {
            key
        };
        return mLocalContentResolver.update(CmsSettingsData.CONTENT_URI, values, WHERE_CLAUSE,
                whereArgs);
    }
}

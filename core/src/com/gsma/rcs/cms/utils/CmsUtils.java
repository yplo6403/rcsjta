package com.gsma.rcs.cms.utils;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.utils.logger.Logger;

import local.org.bouncycastle.operator.RuntimeOperatorException;

public class CmsUtils {

    private static final Logger sLogger = Logger.getLogger(CmsUtils.class.getSimpleName());

    public static String convertContactToCmsRemoteFolder(MessageData.MessageType messageType, String contact){
        CmsSettings settings = CmsSettings.getInstance();
        if(settings==null){
            return null;
        }

        if(messageType == MessageData.MessageType.SMS ||messageType== MessageData.MessageType.MMS){
            StringBuilder sb = new StringBuilder(settings.getCmsDefaultDirectory())
                    .append(settings.getCmsDirectorySeparator())
                    .append(Constants.TEL_PREFIX)
                    .append(contact);
            return sb.toString();
        }
        String msg = new StringBuilder("MessageType not supported : ").append(messageType).toString();
        if(sLogger.isActivated()){
            sLogger.info(msg);
        }
        throw new RuntimeOperatorException(msg);
    }

    public static String convertCmsRemoteFolderToContact(MessageData.MessageType messageType, String cmsRemoteFolder){
        CmsSettings settings = CmsSettings.getInstance();
        if(settings==null){
            return null;
        }

        if(messageType == MessageData.MessageType.SMS ||messageType== MessageData.MessageType.MMS){
            String contact = cmsRemoteFolder;
            String prefix = new StringBuilder(settings.getCmsDefaultDirectory())
                    .append(settings.getCmsDirectorySeparator())
                    .append((Constants.TEL_PREFIX)).toString();
            if(cmsRemoteFolder.startsWith(prefix)){
                contact = cmsRemoteFolder.substring(prefix.length());
            }
            else{
                if(sLogger.isActivated()){
                    sLogger.warn(new StringBuilder("Can not convert cms remote folder into contact  ").append(cmsRemoteFolder).toString());
                }
            }
            return contact;
        }
        String msg = new StringBuilder("MessageType not supported : ").append(messageType).toString();
        if(sLogger.isActivated()){
            sLogger.info(msg);
        }
        throw new RuntimeOperatorException(msg);
    }
}

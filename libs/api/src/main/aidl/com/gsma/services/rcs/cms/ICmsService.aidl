package com.gsma.services.rcs.cms;

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.cms.IXmsMessage;
import com.gsma.services.rcs.cms.IXmsMessageListener;
import android.net.Uri;

/**
 * Common Message Store service API
 */
interface ICmsService {

	int getServiceVersion();

	void syncOneToOneConversation(in ContactId contact);

	void syncGroupConversation(in String chatId);

	void syncAll();

	IXmsMessage getXmsMessage(in String messageId);

	void sendTextMessage(in ContactId contact, String text);

    boolean isAllowedToSendMultimediaMessage();

	IXmsMessage sendMultimediaMessage(in ContactId contact, in List<Uri> files, in String subject, in String body);

    void markXmsMessageAsRead(in String messageId);

	void addEventListener(in IXmsMessageListener listener);

    void removeEventListener(in IXmsMessageListener listener);

    void deleteXmsMessages();

    void deleteXmsMessages2(in ContactId contact);

    void deleteXmsMessage(in String messageId);

    void deleteImapData();
}
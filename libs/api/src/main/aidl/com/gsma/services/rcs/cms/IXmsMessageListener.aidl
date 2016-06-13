// IXmsMessageListener.aidl
package com.gsma.services.rcs.cms;

import com.gsma.services.rcs.contact.ContactId;

interface IXmsMessageListener {

    void onStateChanged(in ContactId contact, in String mimeType, in String msgId,
			in int state, in int reasonCode);

	void onDeleted(in ContactId contact, in List<String> msgIds);

	void onRead(in ContactId contact, in String msgId);

}

package com.gsma.services.rcs.cms;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for CMS synchronization events
 */
interface ICmsSynchronizationListener {

	void onAllSynchronized();

	void onOneToOneConversationSynchronized(in ContactId contact);

	void onGroupConversationSynchronized(in String chatId);
}

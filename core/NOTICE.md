#RCS-CMS stack for Android 



> Installation:
> The RCS\_core.apk must be first installed on the device. Client applications (i.e. RCS_RI.apk, RCS_settings.apk and cmsToolKit.apk) must be installed afterwards otherwise permission to bind to the core service is denied.

News:

	* Manage permissions for Android Marshmallow

Bugs fixed:

	* Cms: Bad aggregation of event framework messages
	* Cms: wrong value for contribution and conversion id in the eventfw message sent via MSRP
	* Core: bad format of the date validity for HTTP file transfer information
	* Core: do not insert +g.oma.sip-im tag into capabilities options
	* Cms: do not update list of participant if empty
	* Core does not fill correctly the CPIM TO header when sending IMDN notification
	* RI: do not display dummy contact for outgoing group chat messages
	* CMS RI: priority given to the display name of the Android Address Book
	* CMS: TerminatingEventFrameworkSession not registered by IMS session service
	* Core: NPE if connection event occurs before core starts
	* CMS RI: GroupTalkView only allows viewing of delivery info for outgoing direction
	* CMS: TerminatingEventFrameworkSession not registered by IMS session service
	* Core : Timer must be based on system time
	* Reset IMAP counters upon deletion of mailbox on the CMS
	* RI: while being disconnected from the API, the connection manager behaves wrongly
	* Core: initialize Core background handler before start
	* Core : when the stack is stopped, there are two remaining threads (RcsCoreService and Core).

	
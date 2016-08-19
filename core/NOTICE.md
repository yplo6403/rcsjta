#RCS-CMS stack for Android 

Licensing :

The RCS stack is under Apache 2 license (see LICENSE-2.0.txt) and uses the following open source libraries:

 - NIST SIP: see LICENSE-NIST.txt.
 - [DNS Java](http://www.dnsjava.org/): see LICENSE-DNS.txt.
 - The [Legion Of The Bouncy Castle](http://www.bouncycastle.org/java.html): see LICENSE-BOUNCYCASTLE.txt.
 - The [Android SMS/MMS Sending Library](https://github.com/klinker41/android-smsmms): see LICENSE-KLINKER41.txt.

> Installation:
> The RCS\_core.apk must be first installed on the device. Client applications (i.e. RCS_RI.apk, RCS_settings.apk and cmsToolKit.apk) must be installed afterwards otherwise permission to bind to the core service is denied.

Limitations :

	* MMS received while mobile data is OFF are converted by the network into indirect notification. 
	Those messages are not discovered by the RCS stack.
	* Group chat is not tested since group chat messages are not correctly pushed on CMS.
	* RCS File transfer is not tested since object is not correctly pushed on CMS.


News:
	
	* 26/08/2016: Core stack can/must be set as default SMS app.
	* 26/08/2016: Use of Klinker library for sending SMS and MMS for Android devices.
	* 26/08/2016: Min SDK is set to 19 (KitKat).
	* 01/07/2016: Addition of audio messages (TAPI-1.6).
	* 30/06/2016: RI: view details of messages: chat, file transfer, SMS and MMS.
	* 24/05/2016: Core: refactor provisioning tool.
	* 13/05/2016: CMS RI: replace subject by body text for MMS in TalkListView.
	* 12/05/2016: CMS RI: manage accept/reject of file transfer within talk views.
	* 03/05/2016: Manage permissions for Android Marshmallow.

Bugs fixed:

	* SQL request to push on CMS should restrict scope to XMS messages.
	* CMS: MMS are duplicated if already pushed on CMS and resolved locally.
	* FO 60 SMS correlation issue (SMS stored via the SMS-C) in the stack.
	* CMS: bad initial state for synchronized GC
	* Core: Added support for psMedia & psRTMedia
	* Core: Added ApplyBatch for resetting setting.db to default values
	* Core: Handling configuration error management in Receipt of SMS (OTP)
	* Core: Do not start Keep Alive when registration is not successful
	* Core: Core: Fix for NPE while parsing the configuration file
	* Core: User alias is not displayed for unsaved Contacts
	* Core: Excludes own number in participants list if Referred-by has it in INVITE
	* Core: Handling IllegalArgumentException while deleting 1-to-1 conversation
	* Core: GC messages are getting deleted while deleting o2o chat thread
	* Core: Initiating provisioning if registration fails with 403 response
	* Core: Status is updated as Failed even for delivered messages and FT
    * Core: Fix for crash if network type/subtype is UNKNOWN
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

	
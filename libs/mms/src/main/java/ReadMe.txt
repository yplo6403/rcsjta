/************************************************************/
/*       _____ _____ _____    __    _ _      ___   _      */
/*      |     |     |   __|  |  |  |_| |_   |_  | | |     */
/*      | | | | | | |__   |  |  |__| | . |  |  _|_| |     */
/*      |_|_|_|_|_|_|_____|  |_____|_|___|  |___|_|_|     */
/*                                                          */
/************************************************************/
/* Author: Yoann Hamon & Marc Poppleton                     */
/* Date:   July 04, 2013                                    */
/* Desc:   Send SMS and MMS easily from Android devices     */
/************************************************************/



/************************************************************/
/* Edit the manifest file and add the following permissions */
/************************************************************/

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_SMS" />
    <uses-permission android:name="android.permission.WRITE_SMS" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_APN_SETTINGS" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

/************************************************************/
/*        In the manifest add the following service         */
/************************************************************/

<service android:name="com.orange.labs.mms.priv.TransactionService"
                 android:exported="false" />

/************************************************************/
/*           To send a MMS use the following code           */
/************************************************************/

import com.orange.labs.mms.MmsManager;
import com.orange.labs.mms.MmsMessage;

// Create a new pending intent
private static final String SENT_ACTION = "com.my.app.SENT_ACTION";
PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent(SENT_ACTION), 0);

// Register a new receiver
registerReceiver(new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent){
        int resultCode = getResultCode();
        switch (resultCode){
            // The message was successfully sent, so we update its status into DB
            case Activity.RESULT_OK:
            break;
            // The message wasn't  sent, so we update its status into DB
            case Activity.RESULT_CANCELED:
            break;
        }
        // Unregister the current receiver
        unregisterReceiver(this);
    }
    }, new IntentFilter(SENT_ACTION));


// Create a new message
MmsMessage msg = new MmsMessage();
msg.addTo(number); // Add a dest
msg.setSubject(this.getResources().getString(R.string.msm_content)); // Set subject
msg.attachBitmap(bitmap); // Add a bitmap to the message
msg.setPendingIntent(sentIntent); // Add the pendingIntent used to monitor feedback

// Get the instance of MmsManager
final MmsManager mngr = MmsManager.getDefault(this);

// Send the message
mngr.sendMessage(msg);


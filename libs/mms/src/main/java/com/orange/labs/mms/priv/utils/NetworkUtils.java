/*
 *
 * MmsLib
 * 
 * Module name: com.orange.labs.mms
 * Version:     2.0
 * Created:     2013-06-06
 * Author:      vkxr8185 (Yoann Hamon)
 * 
 * Copyright (C) 2013 Orange
 *
 * This software is confidential and proprietary information of France Telecom Orange.
 * You shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 */

package com.orange.labs.mms.priv.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.util.Log;

import com.orange.labs.mms.priv.MmsApnConfigException;
import com.orange.labs.mms.priv.MmsConnectivityException;
import com.orange.labs.mms.priv.MmsException;
import com.orange.labs.mms.priv.MmsHttpException;
import com.orange.labs.mms.priv.parser.MmsDecoder;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

public final class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    /**
     * Enable the MMS connectivity
     *
     * @param context : Current application context
     * @return true if the connectivity was made, false otherwise
     */
    public static boolean startMmsConnectivity(Context context) throws MmsException {
        // Get the phone connectivity manager
        ConnectivityManager mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Enable the connectivity and check the result every 1.5s. We'll check the connectivity 20 times, then we'll stop.
        int count = 0;
        int result = beginMmsConnectivity(mConnMgr);
        try {
            if (result != 0) {
                NetworkInfo info = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                dumpNetworkInfo(info);

                while (!info.isConnected()) {
                    Thread.sleep(1500);
                    info = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                    if (Constants.DEBUG)
                        Log.d(Constants.TAG, "[CONNECTIVITY] Waiting for CONNECTED: state=" + info.getState());
                    if (count++ > 20) {
                        if (Constants.DEBUG)
                            Log.w(Constants.TAG, "[CONNECTIVITY] Failed to connect");
                        dumpNetworkInfo(info);
                        return false;
                    }
                }
                dumpNetworkInfo(info);
            }
            Thread.sleep(1500);
        } catch (InterruptedException ie){
            throw new MmsConnectivityException(ie.getMessage(),ie);
        }
        return true;
    }

    /**
     * Try to reach the given IP through the MMS connectivity
     *
     * @param context : Current application context
     * @param host    : Remote ip to be ensured
     */
    public static boolean ensureRoute(Context context, String host) {
        // Resolve the IP to an integer
        int addr = lookupHost(host);
        if (addr == -1) {
            if (Constants.DEBUG)
                Log.w(Constants.TAG, "[CONNECTIVITY] Cannot resolve host: " + host);
            return false;
        } else {
            // Try to establish the rout to the previous IP
            ConnectivityManager mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (!mConnMgr.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_MMS, addr)) {
                if (Constants.DEBUG)
                    Log.w(Constants.TAG, "[CONNECTIVITY] Cannot establish route to :" + addr);
                return false;
            } else {
                if (Constants.DEBUG)
                    Log.d(Constants.TAG, "[CONNECTIVITY] Route to MMS proxy has been ensured via MMS APN");
                return true;
            }
        }
    }

    /**
     * Send the given message to the given APN
     *
     * @param apn            : APN to be used
     * @param encodedMessage : MMS message as PDU
     * @return true if the message was correctly sent, false otherwise
     */
    public static boolean sendMessage(MmsApn apn, byte[] encodedMessage) throws MmsException
    {
        try{
            HttpURLConnection connection = getConnection(apn, encodedMessage.length);
            if(connection == null){
                connection = getConnection(apn, encodedMessage.length);
            }

            // Upload mms as bytes array
            if (Constants.DEBUG)
                Log.d(Constants.TAG, "[MMS Sender] Uploading MMS data (Size: " + encodedMessage.length + ")");
            OutputStream out = connection.getOutputStream();
            out.write(encodedMessage);
            out.flush();
            out.close();

            // Wait for the response
            if (Constants.DEBUG)
                Log.d(Constants.TAG, "[MMS Sender] Response code is " + connection.getResponseCode());

            if (connection.getResponseCode() == 200) {
                // No acknowledge fix
                if (connection.getContentLength() == 0) {
                    // Close the connection
                    if (Constants.DEBUG) Log.d(Constants.TAG, "[MMS Sender] Disconnecting ...");
                    connection.disconnect();

                    if (Constants.DEBUG)
                        Log.d(Constants.TAG, "[MMS Sender] Message has been successfully sent !");

                    // Return a message id based on timestamp
                    return true;
                } else if (connection.getContentLength() >= 0) {
                    if (Constants.DEBUG) Log.d(Constants.TAG, "[MMS Sender] Reading response ...");
                    byte[] responseArray = new byte[connection.getContentLength()];
                    DataInputStream i = new DataInputStream(connection.getInputStream());
                    int b = 0;
                    int index = 0;
                    while ((b = i.read()) != -1) {
                        responseArray[index] = (byte) b;
                        index++;
                    }
                    i.close();

                    // Close the connection
                    if (Constants.DEBUG) Log.d(Constants.TAG, "[MMS Sender] Disconnecting ...");
                    connection.disconnect();
                    // Try to decode the response code
                    MmsDecoder parser = new MmsDecoder(responseArray);
                    parser.parse();
                    if (parser.getResponseStatus() == 128) {
                        if (Constants.DEBUG)
                            Log.d(Constants.TAG, "[MMS Sender] Message has been successfully sent !");
                        return true;
                    }

                    if (Constants.DEBUG) Log.e(Constants.TAG, "Attempt failed");
                    if (Constants.DEBUG)
                        Log.e(Constants.TAG, "Response is " + parser.getResponseStatus());
                    if (Constants.DEBUG)
                        Log.e(Constants.TAG, "Response is " + parser.getResponseText());

                    return false;
                }
            }

            // Close the connection
            if (Constants.DEBUG) Log.d(Constants.TAG, "[MMS Sender] Disconnecting ...");
            connection.disconnect();

            return false;
        } catch (MalformedURLException mue) {
            throw new MmsApnConfigException("Malformed url for apn : " + apn.mmsc, mue);
        } catch (IOException ioe) {
            throw new MmsHttpException(ioe.getMessage(), ioe);
        }
    }

    private static HttpURLConnection getConnection(MmsApn apn, int contentLength) throws MalformedURLException, IOException{

        URL url = new URL(apn.mmsc);
        Properties systemProperties = System.getProperties();

        // Set-up proxy
        if (apn.isProxySet) {
            if (Constants.DEBUG)
                Log.d(Constants.TAG, "[MMS Sender] Setting up proxy (" + apn.proxyHost + ":" + apn.proxyPort + ")");

            systemProperties.setProperty("http.proxyHost", apn.proxyHost);
            systemProperties.setProperty("http.proxyPort", apn.proxyPort.equals("") ? "80" : apn.proxyPort);
        }

        systemProperties.setProperty("http.keepAlive", "false");
        systemProperties.setProperty("http.agent", "Android-Mms/2.0");

        // Open connection
        HttpURLConnection connection;

        connection = (HttpURLConnection) url.openConnection();

        // Use the URL connection for output
        connection.setUseCaches(false);
        connection.setDoOutput(true);

        // Set redirection rules
        HttpURLConnection.setFollowRedirects(true);
        connection.setInstanceFollowRedirects(true);

        // Set the timeouts
        connection.setConnectTimeout(25000);
        connection.setReadTimeout(25000);

        // Sends the request
        if (Constants.DEBUG) Log.d(Constants.TAG, "[MMS Sender] Setting up headers");
        connection.setRequestProperty("Content-Type", "application/vnd.wap.mms-message");
        connection.setRequestProperty("Content-Length", Integer.toString(contentLength));
        connection.setRequestProperty("Accept", "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic");
        connection.setRequestProperty("x-wap-profile", "http://www.google.com/oha/rdf/ua-profile-kila.xml");
        connection.setRequestProperty("Accept-Language", "fr-FR, en-US");

        // Post method
        connection.setRequestMethod("POST");

        // Connect to the MMSC
        if (Constants.DEBUG) Log.d(Constants.TAG, "[MMS Sender] Connecting to APN " + apn.mmsc);
        try{
            connection.connect();
            return connection;
        }catch(SocketException se){
            return null;
        }
    }

    /**
     * Try to stop the MMS connecitivity
     *
     * @param context : Current application context
     */
    public static void endConnectivity(Context context) {
        if (Constants.DEBUG)
            Log.d(Constants.TAG, "[CONNECTIVITY] stopUsingNetworkFeature: MOBILE, enableMMS");
        ConnectivityManager mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mConnMgr.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableMMS");
    }

    /**
     * Try to initiate the MMS connectivity
     *
     * @param connMgr : Application connectivity manager
     * @return The current status of the connectivity
     * @throws Exception Throws an exception if the MMS connectivity isn't available
     */
    private static int beginMmsConnectivity(ConnectivityManager connMgr) throws MmsException {
        if (Constants.DEBUG)
            Log.d(Constants.TAG, "[CONNECTIVITY] startUsingNetworkFeature: MOBILE, enableMMS");
        int result = connMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableMMS");

        if (Constants.DEBUG)
            Log.d(Constants.TAG, "[CONNECTIVITY] beginMmsConnectivity: result=" + result);

        switch (result) {
            case 0:
            case 1:
                return result;
        }

        throw new MmsConnectivityException("Cannot establish MMS connectivity");
    }

    /**
     * Look up a host name and return the result as an int. Works if the argument
     * is an IP address in dot notation. Obviously, this can only be used for IPv4
     * addresses.
     *
     * @param hostname as an IP address
     * @return the IP address as an Integer in network byte order
     */
    public static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24)
                | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8)
                | (addrBytes[0] & 0xff);
        return addr;
    }

    /**
     * Dumps into the debug log output the content of a NetworkInfo object
     *
     * @param info the NetworkInfo object to dump
     */
    private static void dumpNetworkInfo(NetworkInfo info) {
        if (Constants.DEBUG) {
            Log.d(Constants.TAG, "-- DUMPING NETWORKINFO --");
            Log.d(Constants.TAG, "DetailedState: " + info.getDetailedState().name());
            Log.d(Constants.TAG, "ExtraInfo: " + info.getExtraInfo());
            Log.d(Constants.TAG, "Reason: " + info.getReason());
            Log.d(Constants.TAG, "State: " + info.getState().name());
            Log.d(Constants.TAG, "Type: " + info.getType());
            Log.d(Constants.TAG, "TypeName: " + info.getTypeName());
            Log.d(Constants.TAG, "Subtype: " + info.getSubtype());
            Log.d(Constants.TAG, "SubtypeName: " + info.getSubtypeName());
            Log.d(Constants.TAG, "**-- END NETWORKINFO --**");
        }
    }
}

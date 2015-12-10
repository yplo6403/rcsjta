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
import android.database.Cursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class MmsApn {
    private static final String TAG = "MmsApn";

    public String mmsc = "";
    public String proxyHost = "";
    public String proxyPort = "";
    public String username = "";
    public String password = "";
    public boolean isProxySet = false;

    public MmsApn(String mmsc, String proxyHost, String proxyPort) {
        this.mmsc = mmsc;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;

        isProxySet = (proxyHost != null && proxyPort != null && !proxyHost.equals(""));
    }

    public MmsApn(String mmsc, String proxyHost, int proxyPort) {
        this.mmsc = mmsc;
        this.proxyHost = proxyHost;
        this.proxyPort = String.valueOf(proxyPort);

        isProxySet = (proxyHost != null && !proxyHost.equals(""));
    }

    public MmsApn(String mmsc, String proxyHost, String proxyPort, String username, String password) {
        this.mmsc = mmsc;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.username = username;
        this.password = password;

        isProxySet = (proxyHost != null && proxyPort != null && !proxyHost.equals(""));
    }

    /**
     * Get the list of the device's running APNs
     *
     * @return A list containing instances of the APNs
     */
    public static List<MmsApn> getMmsAPNs(Context context)
    {
        List<MmsApn> results = new ArrayList<MmsApn>();

        // File on device where the APN could be located
        File apns_conf = new File("/system/etc/apns-conf.xml");
        File apns_conf2 = new File("/system/etc/customer/apns-conf.xml");

        // This is an "old" version of Android (Under 4.2)
        if (android.os.Build.VERSION.SDK_INT < 17) {
            Uri apnUri = Uri.parse("content://telephony/carriers/current");
            Cursor apnCursor = context.getContentResolver().query(apnUri, null, null, null, null);
            if (apnCursor == null) {
                return Collections.emptyList();
            } else {
                while (apnCursor.moveToNext()) {
                    String type = apnCursor.getString(apnCursor.getColumnIndex("type"));
                    if (!TextUtils.isEmpty(type) && (type.equalsIgnoreCase("*") || type.contains("mms"))) {
                        String mmsc = apnCursor.getString(apnCursor.getColumnIndex("mmsc"));
                        String mmsProxy = apnCursor.getString(apnCursor.getColumnIndex("mmsproxy"));
                        String mmsPort = apnCursor.getString(apnCursor.getColumnIndex("mmsport"));
                        String username = apnCursor.getString(apnCursor.getColumnIndex("user"));
                        String password = apnCursor.getString(apnCursor.getColumnIndex("password"));
                        if (username != null && password != null)
                            results.add(new MmsApn(mmsc, mmsProxy, mmsPort, username, password));
                        else
                            results.add(new MmsApn(mmsc, mmsProxy, mmsPort));
                    }
                }

                apnCursor.close();
            }
        }

        // Check if the file exists of device (Way 1)
        else if (apns_conf.exists()) {
            results = parseFile(context, apns_conf);
        }

        // Check if the file exists of device (Way 2)
        else if (apns_conf2.exists()) {
            results = parseFile(context, apns_conf2);
        }

        // Otherwise we force download and save the file on disk
        //else
        if (results.isEmpty()) {
            // Read APNs configuration from file
            InputStream inputFile = MmsApn.class.getResourceAsStream("/com/orange/labs/mms/priv/res/apns_conf.xml");
            if (inputFile == null) {
                if (Constants.DEBUG) Log.e(Constants.TAG, "[APN] Cannot read the config file !");
                return Collections.emptyList();
            }

            // Parse file
            results = parseFile(context, inputFile);
        }

        return results;
    }

    public static List<MmsApn> parseFile(Context context, File file) {
        List<MmsApn> results = new ArrayList<MmsApn>();
        try {
            TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            String networkOperator = telephonyManager.getNetworkOperator();
            if (networkOperator == null) return Collections.emptyList();

            int carrierMcc = Integer.parseInt(networkOperator.substring(0, 3));
            int carrierMnc = Integer.parseInt(networkOperator.substring(3));

            Log.e(Constants.TAG, "Mcc: " + carrierMcc);
            Log.e(Constants.TAG, "Mnc: " + carrierMnc);

            DocumentBuilder docBuilder = null;

            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.parse(file);
            NodeList apns = doc.getElementsByTagName("apn");
            for (int i = 0; i < apns.getLength(); i++) {
                Element elem = (Element) apns.item(i);
                if (elem.hasAttribute("type") && elem.getAttribute("type").contains("mms")) {
                    int mcc = Integer.valueOf(elem.getAttribute("mcc"));
                    int mnc = Integer.valueOf(elem.getAttribute("mnc"));
                    if (mcc == carrierMcc && mnc == carrierMnc) {
                        Log.d(Constants.TAG, "[APN] Carrier that may work: " + elem.getAttribute("carrier"));
                        String mmsc = elem.getAttribute("mmsc");
                        String mmsProxy = elem.getAttribute("mmsproxy");
                        String mmsPort = elem.getAttribute("mmsport");
                        String username = elem.getAttribute("user");
                        String password = elem.getAttribute("password");
                        if (username != null && password != null)
                            results.add(new MmsApn(mmsc, mmsProxy, mmsPort, username, password));
                        else
                            results.add(new MmsApn(mmsc, mmsProxy, mmsPort));
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static List<MmsApn> parseFile(Context context, InputStream stream) {
        List<MmsApn> results = new ArrayList<MmsApn>();
        try {
            TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            String networkOperator = telephonyManager.getNetworkOperator();
            if (networkOperator == null) return Collections.emptyList();

            int carrierMcc = Integer.parseInt(networkOperator.substring(0, 3));
            int carrierMnc = Integer.parseInt(networkOperator.substring(3));

            Log.e(Constants.TAG, "Mcc: " + carrierMcc);
            Log.e(Constants.TAG, "Mnc: " + carrierMnc);

            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.parse(stream);
            NodeList apns = doc.getElementsByTagName("apn");
            for (int i = 0; i < apns.getLength(); i++) {
                Element elem = (Element) apns.item(i);
                if (elem.hasAttribute("type") && elem.getAttribute("type").contains("mms")) {
                    int mcc = Integer.valueOf(elem.getAttribute("mcc"));
                    int mnc = Integer.valueOf(elem.getAttribute("mnc"));
                    if (mcc == carrierMcc && mnc == carrierMnc) {
                        if (Constants.DEBUG)
                            Log.e(Constants.TAG, "Carrier found: " + elem.getAttribute("carrier"));
                        String mmsc = elem.getAttribute("mmsc");
                        String mmsProxy = elem.getAttribute("mmsproxy");
                        String mmsPort = elem.getAttribute("mmsport");
                        results.add(new MmsApn(mmsc, mmsProxy, mmsPort));
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MmsApn{");
        sb.append("mmsc='").append(mmsc).append('\'');
        sb.append(", proxyHost='").append(proxyHost).append('\'');
        sb.append(", proxyPort='").append(proxyPort).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", isProxySet=").append(isProxySet);
        sb.append('}');
        return sb.toString();
    }
}

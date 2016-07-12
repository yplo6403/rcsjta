/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.utils;

import com.gsma.rcs.platform.network.DatagramConnection;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.platform.network.SocketServerConnection;
import com.gsma.rcs.provider.settings.RcsSettings;

import java.io.IOException;
import java.util.Random;

/**
 * Network ressource manager
 * 
 * @author jexa7410
 */
public class NetworkRessourceManager {

    /**
     * Default SIP port max
     */
    private static final int DEFAULT_LOCAL_SIP_PORT_RANGE_MAX = 65000;

    /**
     * Generate a default free SIP port number <br>
     * This returns a random port which is free both in UDP and in TCP <br>
     * Note that it does not bind to those ports, so there still is a chance that something else
     * binds to it before you do. <br>
     * Minimize this chance by using those ports as soon as possible
     * 
     * @param rcsSettings
     * @return Local SIP port
     */
    public static synchronized int generateLocalSipPort(RcsSettings rcsSettings) {
        int defaultLocalSipPortRangeMin = rcsSettings.getSipListeningPort();
        int candidatePort = getDefaultNumber(defaultLocalSipPortRangeMin,
                DEFAULT_LOCAL_SIP_PORT_RANGE_MAX);
        while (!isLocalUdpPortFree(candidatePort) && !isLocalTcpPortFree(candidatePort)) {
            // Loop until candidate port is free in UDP and in TCP
            candidatePort = getDefaultNumber(defaultLocalSipPortRangeMin,
                    DEFAULT_LOCAL_SIP_PORT_RANGE_MAX);
        }
        return candidatePort;
    }

    /**
     * Generate a default number from a given range
     * 
     * @param minRange
     * @param maxRange
     * @return number Random number between minRange and maxRange
     */
    private static int getDefaultNumber(int minRange, int maxRange) {
        Random random = new Random();
        return (random.nextInt(maxRange - minRange + 1) + minRange);
    }

    /**
     * Generate a default free RTP port number
     * 
     * @param rcsSettings
     * @return Local RTP port
     */
    public static synchronized int generateLocalRtpPort(RcsSettings rcsSettings) {
        return generateLocalUdpPort(rcsSettings.getDefaultRtpPort());
    }

    /**
     * Generate a default free MSRP port number
     * 
     * @param rcsSettings
     * @return Local MSRP port
     */
    public static synchronized int generateLocalMsrpPort(RcsSettings rcsSettings) {
        return generateLocalTcpPort(rcsSettings.getDefaultMsrpPort());
    }

    /**
     * Generate a free UDP port number from a specific port base
     * 
     * @param portBase UDP port base
     * @return Local UDP port
     */
    private static int generateLocalUdpPort(int portBase) {
        int resp = -1;
        int port = portBase;
        while ((resp == -1) && (port < Integer.MAX_VALUE)) {
            if (isLocalUdpPortFree(port)) {
                // Free UDP port found
                resp = port;
            } else {
                // +2 needed for RTCP port
                port += 2;
            }
        }
        return resp;
    }

    /**
     * Test if the given local UDP port is really free (not used by other applications)
     * 
     * @param port Port to check
     * @return Boolean
     */
    private static boolean isLocalUdpPortFree(int port) {
        boolean res = false;
        try {
            DatagramConnection conn = NetworkFactory.getFactory().createDatagramConnection();
            conn.open(port);
            conn.close();
            res = true;
        } catch (IOException e) {
            res = false;
        }
        return res;
    }

    /**
     * Generate a free TCP port number
     * 
     * @param portBase TCP port base
     * @return Local TCP port
     */
    private static int generateLocalTcpPort(int portBase) {
        int resp = -1;
        int port = portBase;
        while (resp == -1) {
            if (isLocalTcpPortFree(port)) {
                // Free TCP port found
                resp = port;
            } else {
                port++;
            }
        }
        return resp;
    }

    /**
     * Test if the given local TCP port is really free (not used by other applications)
     * 
     * @param port Port to check
     * @return Boolean
     */
    private static boolean isLocalTcpPortFree(int port) {
        boolean res = false;
        try {
            SocketServerConnection conn = NetworkFactory.getFactory()
                    .createSocketServerConnection();
            conn.open(port);
            conn.close();
            res = true;
        } catch (IOException e) {
            res = false;
        }
        return res;
    }

    /**
     * Is a valid IP address
     * 
     * @param ipAddress IP address
     * @return Boolean
     */
    public static boolean isValidIpAddress(String ipAddress) {
        boolean result = false;
        if ((ipAddress != null) && (!ipAddress.equals("127.0.0.1"))
                && (!ipAddress.equals("localhost"))) {
            result = true;
        }
        return result;
    }

    /**
     * Convert an IP address to its integer representation
     * 
     * @param addr IP address
     * @return Integer
     */
    public static int ipToInt(String addr) {
        String[] addrArray = addr.split("\\.");
        int num = 0;
        for (int i = 0; i < addrArray.length; i++) {
            int power = 3 - i;
            num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
        }
        return num;
    }
}

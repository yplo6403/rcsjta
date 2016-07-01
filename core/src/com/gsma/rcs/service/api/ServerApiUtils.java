/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.services.rcs.RcsServiceRegistration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Server API utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServerApiUtils {
    /**
     * Test core
     */
    public static void testCore() {
        if (Core.getInstance() == null) {
            throw new ServerApiGenericException("Core is not instantiated");
        }
    }

    /**
     * Test IMS connection
     */
    public static void testIms() {
        if (!isImsConnected()) {
            throw new ServerApiServiceNotRegisteredException("Core is not connected to IMS");
        }
    }

    /**
     * Is connected to IMS
     * 
     * @return Boolean
     */
    public static boolean isImsConnected() {
        Core core = Core.getInstance();
        if (core == null) {
            return false;
        }
        ImsNetworkInterface networkInterface = core.getImsModule().getCurrentNetworkInterface();
        return networkInterface != null && networkInterface.isRegistered();
    }

    /**
     * Is mobile connection available
     *
     * @return Boolean
     */
    public static boolean isMmsConnectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        // TODO suppress deprecation
        NetworkInfo networkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
        return networkInfo != null && networkInfo.isAvailable();
    }

    /**
     * Gets the reason code for IMS service registration
     * 
     * @return reason code
     */
    public static RcsServiceRegistration.ReasonCode getServiceRegistrationReasonCode() {
        Core core = Core.getInstance();
        if (core == null) {
            return RcsServiceRegistration.ReasonCode.UNSPECIFIED;
        }
        ImsNetworkInterface networkInterface = core.getImsModule().getCurrentNetworkInterface();
        if (networkInterface == null) {
            return RcsServiceRegistration.ReasonCode.UNSPECIFIED;
        }
        return networkInterface.getRegistrationReasonCode();
    }

    /**
     * Test IMS extension
     * 
     * @param ext Extension ID
     * @throws ServerApiPermissionDeniedException
     * @throws ServerApiServiceNotRegisteredException
     */
    public static void testImsExtension(String ext) throws ServerApiPermissionDeniedException,
            ServerApiServiceNotRegisteredException {
        if (!isImsConnected()) {
            throw new ServerApiServiceNotRegisteredException("Core is not connected to IMS");
        }
        if (!Core.getInstance().getImsModule().getExtensionManager().isExtensionAuthorized(ext)) {
            throw new ServerApiPermissionDeniedException("Extension not authorized");
        }
    }
}

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

package com.gsma.rcs.core.ims.protocol.msrp;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.platform.network.SocketConnection;
import com.gsma.rcs.utils.logger.Logger;

/**
 * MSRP client connection
 * 
 * @author jexa7410
 */
public class MsrpClientConnection extends MsrpConnection {
    /**
     * Remote IP address
     */
    private String remoteAddress;

    /**
     * Remote TCP port number
     */
    private int remotePort;

    /**
     * Secured connection
     */
    private boolean secured = false;

    // Changed by Deutsche Telekom
    /**
     * Secured connection
     */
    private String announcedFingerprint = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param session MSRP session
     * @param remoteAddress Remote IP address
     * @param remotePort Remote port number
     */
    public MsrpClientConnection(MsrpSession session, String remoteAddress, int remotePort) {
        super(session);

        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
    }

    /**
     * Constructor
     * 
     * @param session MSRP session
     * @param remoteAddress Remote IP address
     * @param remotePort Remote port number
     * @param secured Secured media flag
     */
    public MsrpClientConnection(MsrpSession session, String remoteAddress, int remotePort,
            boolean secured) {
        this(session, remoteAddress, remotePort);

        this.secured = secured;
    }

    // Changed by Deutsche Telekom
    /**
     * Constructor
     * 
     * @param session MSRP session
     * @param remoteAddress Remote IP address
     * @param remotePort Remote port number
     * @param secured Secured media flag
     * @param fingerprint fingerprint announced in SDP
     */
    public MsrpClientConnection(MsrpSession session, String remoteAddress, int remotePort,
            boolean secured, String fingerprint) {
        this(session, remoteAddress, remotePort);

        this.secured = secured;
        this.announcedFingerprint = fingerprint;
    }

    /**
     * Is secured connection
     * 
     * @return Boolean
     */
    public boolean isSecured() {
        return secured;
    }

    /**
     * Returns the socket connection
     * 
     * @return Socket
     * @throws NetworkException
     * @throws PayloadException
     */
    public SocketConnection getSocketConnection() throws NetworkException, PayloadException {
        if (logger.isActivated()) {
            logger.debug("Open client socket to " + remoteAddress + ":" + remotePort);
        }
        SocketConnection socket;
        if (secured) {
            // Changed by Deutsche Telekom
            if (this.announcedFingerprint != null) {
                // follow RFC 4572
                // use self-signed certificates
                socket = NetworkFactory.getFactory().createSimpleSecureSocketClientConnection(
                        this.announcedFingerprint);
            } else {
                socket = NetworkFactory.getFactory().createSecureSocketClientConnection();
            }
        } else {
            socket = NetworkFactory.getFactory().createSocketClientConnection();
        }
        socket.open(remoteAddress, remotePort);
        if (logger.isActivated()) {
            logger.debug("Socket connected to " + remoteAddress + ":" + remotePort);
        }
        return socket;
    }
}

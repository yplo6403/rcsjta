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

package com.gsma.rcs.ri.extension.messaging;

/**
 * Messaging service utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessagingSessionUtils {
    /**
     * Service ID constant
     */
    public final static String SERVICE_ID = "ext.messaging";

    /**
     * Service content type
     */
    public final static String SERVICE_CONTENT_TYPE = "plain/text";

    /**
     * Service accept-type
     */
    public final static String[] SERVICE_ACCEPT_TYPE = {
        "plain/text"
    };

    /**
     * Service accept-wrapped-type
     */
    public final static String[] SERVICE_WRAPPED_ACCEPT_TYPE = {
    // no wrapped here
    };
}

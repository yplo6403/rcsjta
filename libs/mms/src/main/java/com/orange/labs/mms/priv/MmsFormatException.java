/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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
 */

package com.orange.labs.mms.priv;

/**
 * A generic exception that is thrown by the Mms client.
 */
public class MmsFormatException extends MmsException {
    private static final long serialVersionUID = -7323249827281485390L;

    /**
     * Creates a new MmsConnectivityException with the specified detail message.
     *
     * @param message the detail message.
     * @param cause the cause
     */
    public MmsFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

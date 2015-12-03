/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010 France Telecom S.A.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package com.gsma.services.rcs.cms;

import android.util.SparseArray;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

/**
 * XMS message
 * Created by Philippe LEMORDANT on 12/11/2015.
 */
public class XmsMessage {

    private IXmsMessage mIXmsMessage;

    public enum State {
        /**
         * The message is queued to be sent by CMS service when possible.
         */
        QUEUED(0),
        /**
         * The message is sent.
         */
        SENT(1),
        /**
         * The message has failed.
         */
        FAILED(2),
        /**
         * The message has been delivered to the remote.
         */
        DELIVERED(3),
        /**
         * The message is being displayed.
         */
        DISPLAYED(4),
        /**
         * The message is being received.
         */
        RECEIVED(5)
        ;

        private static SparseArray<State> mValueToEnum = new SparseArray<>();

        static {
            for (State entry : State.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private final int mValue;

        private State(int value) {
            mValue = value;
        }

        /**
         * Returns a State instance representing the specified integer value.
         *
         * @param value the integer value
         * @return State instance
         */
        public static State valueOf(int value) {
            State entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + State.class.getName() + "" + value + "!");
        }

        /**
         * Returns the value of this State as an integer.
         *
         * @return integer value
         */
        public final int toInt() {
            return mValue;
        }
    }

    public enum ReasonCode {
        UNSPECIFIED(0),
        FAILED_ERROR_GENERIC_FAILURE(1),
        FAILED_ERROR_RADIO_OFF(2),
        FAILED_ERROR_NULL_PDU(3),
        FAILED_ERROR_NO_SERVICE(4),
        FAILED_MMS_ERROR_UNSPECIFIED(5),
        FAILED_MMS_ERROR_INVALID_APN(6),
        FAILED_MMS_ERROR_UNABLE_CONNECT_MMS(7),
        FAILED_MMS_ERROR_HTTP_FAILURE(8),
        FAILED_MMS_ERROR_IO_ERROR(9),
        FAILED_MMS_ERROR_RETRY(10),
        FAILED_MMS_ERROR_CONFIGURATION_ERROR(11);
        private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<>();

        static {
            for (ReasonCode entry : ReasonCode.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private final int mValue;

        private ReasonCode(int value) {
            mValue = value;
        }

        /**
         * Returns a ReasonCode instance representing the specified integer value.
         *
         * @param value the integer value
         * @return ReasonCode instance
         */
        public static ReasonCode valueOf(int value) {
            ReasonCode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + ReasonCode.class.getName() + "" + value + "!");
        }

        /**
         * Returns the value of this ReasonCode as an integer.
         *
         * @return integer value
         */
        public final int toInt() {
            return mValue;
        }
    }

    /**
     * Constructor
     *
     * @param iXmsMessage XMS message interface
     * @hide
     */
    /* package private */ XmsMessage(IXmsMessage iXmsMessage) {
        mIXmsMessage = iXmsMessage;
    }

    /**
     * Gets the message ID
     *
     * @return String
     * @throws RcsGenericException
     */
    public String getMessageId() throws RcsGenericException {
        try {
            return mIXmsMessage.getMessageId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the remote contact
     *
     * @return ContactId
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ContactId getRemoteContact() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mIXmsMessage.getRemoteContact();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the mime type of the XMS message.
     *
     * @return String
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getMimeType() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mIXmsMessage.getMimeType();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the direction of the XMS message
     *
     * @return Direction
     * @see RcsService.Direction
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public RcsService.Direction getDirection() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return RcsService.Direction.valueOf(mIXmsMessage.getDirection());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the local time-stamp of when the XMS message was sent and/or queued for outgoing
     * messages or the local time-stamp of when the XMS message was received for incoming messages.
     *
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestamp() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mIXmsMessage.getTimestamp();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the local time-stamp of when the XMS message was sent and/or queued for outgoing
     * messages or the remote time-stamp of when the XMS message was sent for incoming messages.
     *
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestampSent() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mIXmsMessage.getTimestampSent();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the local timestamp of when the XMS message was delivered for outgoing messages or 0
     * for incoming messages or it was not yet delivered.
     *
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestampDelivered() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mIXmsMessage.getTimestampDelivered();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the state of the XMS message.
     *
     * @return State
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public State getState() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return State.valueOf(mIXmsMessage.getState());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the reason code of the XMS message.
     *
     * @return ReasonCode
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ReasonCode getReasonCode() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return ReasonCode.valueOf(mIXmsMessage.getReasonCode());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets true is this XMS message has been marked as read.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isRead() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mIXmsMessage.isRead();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the body text message
     *
     * @return String
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getBody() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mIXmsMessage.getBody();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Gets the chat ID
     *
     * @return String
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getChatId() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mIXmsMessage.getChatId();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}

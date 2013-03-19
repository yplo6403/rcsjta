/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.richcall;

import android.content.Context;
import java.lang.String;
import org.gsma.joyn.ClientApiException;
import org.gsma.joyn.media.IMediaPlayer;


/**
 * Class RichCallApi.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class RichCallApi extends org.gsma.joyn.ClientApi {

    /**
     * @param ctx Application context
     */
    public RichCallApi(Context ctx) {
        super ((Context) null);
    }

    public void connectApi() {

    }

    public void disconnectApi() {

    }

    /**
     * Returns the remote phone number.
     *
     * @return  The remote phone number.
     */
    public String getRemotePhoneNumber() throws ClientApiException {
        return (String) null;
    }

    /**
     *
     * @param contact
     * @param player
     * @return The i video sharing session.
     */
    public IVideoSharingSession initiateLiveVideoSharing(String contact, IMediaPlayer player) throws ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     *
     * @param contact
     * @param file
     * @param player
     * @return  The i video sharing session.
     */
    public IVideoSharingSession initiateVideoSharing(String contact, String file, IMediaPlayer player) throws ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     * Returns the video sharing session.
     *
     * @param id The session id
     * @return  The video sharing session.
     */
    public IVideoSharingSession getVideoSharingSession(String id) throws ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     *
     * @param contact
     * @param file
     * @return  The i image sharing session.
     */
    public IImageSharingSession initiateImageSharing(String contact, String file) throws ClientApiException {
        return (IImageSharingSession) null;
    }

    /**
     * Returns the image sharing session.
     *
     * @param id The session id
     * @return  The image sharing session.
     */
    public IImageSharingSession getImageSharingSession(String id) throws ClientApiException {
        return (IImageSharingSession) null;
    }

    /**
     * Sets the multi party call.
     *
     * @param state 
     */
    public void setMultiPartyCall(boolean state) throws ClientApiException {

    }

    /**
     * Sets the call hold.
     *
     * @param state
     */
    public void setCallHold(boolean state) throws ClientApiException {

    }

}

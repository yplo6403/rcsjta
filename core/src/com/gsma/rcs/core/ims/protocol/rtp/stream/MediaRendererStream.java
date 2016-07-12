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

package com.gsma.rcs.core.ims.protocol.rtp.stream;

import com.gsma.rcs.core.ims.protocol.rtp.media.MediaException;
import com.gsma.rcs.core.ims.protocol.rtp.media.MediaOutput;
import com.gsma.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.gsma.rcs.core.ims.protocol.rtp.util.Buffer;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Media renderer stream
 * 
 * @author jexa7410
 */
public class MediaRendererStream implements ProcessorOutputStream {
    /**
     * Media renderer
     */
    private MediaOutput renderer;

    /**
     * The logger
     */
    private final Logger mLogger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param renderer Media renderer
     */
    public MediaRendererStream(MediaOutput renderer) {
        this.renderer = renderer;
    }

    /**
     * Get Media renderer
     * 
     * @return renderer Media renderer
     */
    public MediaOutput getRenderer() {
        return renderer;
    }

    /**
     * Open the output stream
     * 
     * @throws MediaException
     */
    public void open() throws MediaException {
        renderer.open();
        if (mLogger.isActivated()) {
            mLogger.debug("Media renderer stream opened");
        }

    }

    /**
     * Close the output stream
     */
    public void close() {
        renderer.close();
        if (mLogger.isActivated()) {
            mLogger.debug("Media renderer stream closed");
        }
    }

    /**
     * Write to the stream without blocking
     * 
     * @param buffer Input buffer
     */
    public void write(Buffer buffer) {
        MediaSample sample = new MediaSample((byte[]) buffer.getData(), buffer.getTimestamp(),
                buffer.getSequenceNumber());
        renderer.writeSample(sample);
    }
}

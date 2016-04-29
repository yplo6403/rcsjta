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

package com.gsma.rcs.im.filetransfer;

import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;

/**
 * @author Philippe LEMORDANT
 */
public class FileTransferUtilsTest extends AndroidTestCase {

    private RcsSettings mRcsSettings;

    private static final String sXmlEncoded = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<file><file-info type=\"file\"><file-size>100000</file-size>"
            + "<file-name>gsma.jpg</file-name><content-type>image/jpeg</content-type>"
            + "<data url = \"http://www.gsma.com\"  until=\"2016-04-29T16:02:23.000Z\"/>"
            + "</file-info></file>";

    private final static Uri sUri = Uri.parse("http://www.gsma.com");
    private final static String sFilename = "gsma.jpg";
    private final static int sSize = 100000;
    private final static String sMimeType = "image/jpeg";
    private final static long sExpiration = 1461945743000L;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        LocalContentResolver localContentResolver = new LocalContentResolver(contentResolver);
        mRcsSettings = RcsSettings.getInstance(localContentResolver);
    }

    public void testCreateHttpFileTransferXml() {
        FileTransferHttpInfoDocument doc1 = new FileTransferHttpInfoDocument(mRcsSettings, sUri,
                sFilename, sSize, sMimeType, sExpiration, null);
        String xml = FileTransferUtils.createHttpFileTransferXml(doc1);
        assertEquals(sXmlEncoded, xml);
    }

    public void testParseFileTransferHttpDocument() throws PayloadException {
        FileTransferHttpInfoDocument doc2 = FileTransferUtils.parseFileTransferHttpDocument(
                sXmlEncoded.getBytes(), mRcsSettings);
        assertEquals(sExpiration, doc2.getExpiration());
        assertEquals(sFilename, doc2.getFilename());
        assertEquals(sMimeType, doc2.getMimeType());
        assertEquals(sSize, doc2.getSize());
        assertEquals(sUri, doc2.getUri());
    }
}


package com.gsma.rcs.cms.provider.xms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.test.AndroidTestCase;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.provider.xms.model.MmsPart;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;


import junit.framework.Assert;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class PartLogTest extends AndroidTestCase {

    private PartLog mPartLog;

    private String mMmsId = "myMmsId";

    private List<MmsPart> mParts = Arrays.asList(new MmsPart[]{
            new MmsPart(null, "1_nativeId", "1_contentType", "1_contentId", "1_path", "1_text", "1_thumb".getBytes()),
            new MmsPart(null, "2_nativeId", Constants.CONTENT_TYPE_TEXT, "2_contentId", "2_path", "2_text")
    });


    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        mPartLog = PartLog.getInstance(context);
        mPartLog.deleteAll();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test() {

        mPartLog.addParts(mMmsId, mParts);
        List<MmsPart> parts;
        parts = mPartLog.getParts(mMmsId, false);
        Assert.assertEquals(2, parts.size());
        Assert.assertNotNull(parts.get(0).getBaseId());
        Assert.assertEquals("1_nativeId", parts.get(0).getNativeId());
        Assert.assertEquals("1_contentType", parts.get(0).getContentType());
        Assert.assertEquals("1_contentId", parts.get(0).getContentId());
        Assert.assertEquals("1_path", parts.get(0).getPath());
        Assert.assertEquals("1_text", parts.get(0).getText());
        Assert.assertNull(parts.get(0).getThumb());

        Assert.assertNotNull(parts.get(1).getBaseId());
        Assert.assertEquals("2_nativeId", parts.get(1).getNativeId());
        Assert.assertEquals(Constants.CONTENT_TYPE_TEXT, parts.get(1).getContentType());
        Assert.assertEquals("2_contentId", parts.get(1).getContentId());
        Assert.assertEquals("2_path", parts.get(1).getPath());
        Assert.assertEquals("2_text", parts.get(1).getText());
        Assert.assertNull(parts.get(1).getThumb());


        parts = mPartLog.getParts(mMmsId, true);
        Assert.assertEquals(2, parts.size());
        Assert.assertEquals("1_nativeId", parts.get(0).getNativeId());
        Assert.assertEquals("1_contentType", parts.get(0).getContentType());
        Assert.assertEquals("1_contentId", parts.get(0).getContentId());
        Assert.assertEquals("1_path", parts.get(0).getPath());
        Assert.assertEquals("1_text", parts.get(0).getText());
        Assert.assertEquals("1_thumb", new String(parts.get(0).getThumb()));

        Assert.assertNotNull(parts.get(1).getBaseId());
        Assert.assertEquals("2_nativeId", parts.get(1).getNativeId());
        Assert.assertEquals(Constants.CONTENT_TYPE_TEXT, parts.get(1).getContentType());
        Assert.assertEquals("2_contentId", parts.get(1).getContentId());
        Assert.assertEquals("2_path", parts.get(1).getPath());
        Assert.assertEquals("2_text", parts.get(1).getText());
        Assert.assertNull(parts.get(1).getThumb());

        Assert.assertEquals("2_text", mPartLog.getTextContent(mMmsId));

        mPartLog.deleteParts(mMmsId);
        Assert.assertEquals(0, mPartLog.getParts(mMmsId, false).size());

        mPartLog.addParts(mMmsId, mParts);
        mPartLog.deleteAll();
        Assert.assertEquals(0, mPartLog.getParts(mMmsId, false).size());
    }
}

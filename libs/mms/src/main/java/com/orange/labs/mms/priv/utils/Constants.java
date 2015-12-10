/*
 *
 * MmsLib
 * 
 * Module name: com.orange.labs.mms
 * Version:     2.0
 * Created:     2013-06-06
 * Author:      vkxr8185 (Yoann Hamon)
 * 
 * Copyright (C) 2013 Orange
 *
 * This software is confidential and proprietary information of France Telecom Orange.
 * You shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 */

package com.orange.labs.mms.priv.utils;

public class Constants
{

    public static final String VERSION = "2.1.5";
    public static final String VERSION_NAME = VERSION + " Flying Deer";

	// Show log messages
	public static final boolean DEBUG = false;
	
	// Max file size
	public static final int MAX_FILE_SIZE = 200000; // International limit, do not touch this unless you want to screw up
	
	// Time to wait before retrying if no queued message was found
	public static final int DELAY_NO_MSG = 500;
    public static final String TAG = "com.orange.labs.mms";
}

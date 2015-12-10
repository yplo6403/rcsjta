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

package com.orange.labs.mms.priv.parser;

public final class MmsEncodedPart
{
	private byte[] content;
	private String contentType;
	
	public MmsEncodedPart()
	{	
	}
	
	public void setContent(byte[] content)
	{
		this.content = content;
	}
	
	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public byte[] getContent()
	{
		return content;
	}

	public String getContentType()
	{
		return contentType;
	}
}

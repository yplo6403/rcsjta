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

import java.util.ArrayList;
import java.util.List;

public final class MmsEncodedMessage
{
	private String from = null;
	private String transactionId = null;
	private long date = -1L;
	private String subject = null;
	private String contentType = null;
	private String type = null;
	
	private List<String> to = null;
	private List<MmsEncodedPart> parts = null;
	
	public MmsEncodedMessage()
	{
		this.to = new ArrayList<String>();
		this.parts = new ArrayList<MmsEncodedPart>();
	}
	
	public void setFrom(String from)
	{
		this.from = from + "/TYPE=PLMN";
	}
	
	public void setTo(List<String> dests)
	{
		for (String to : dests)
		{
			this.to.add(to + "/TYPE=PLMN");
		}
	}
	
	public void setTransactionId(String transactionId)
	{
		this.transactionId = transactionId;
	}
	
	public void setDate(long date)
	{
		this.date = date;
	}
	
	public void setSubject(String subject)
	{
		this.subject = subject;
	}
	
	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}
	
	public void setType(String type)
	{
		this.type = type;
	}
	
	public void addPart(MmsEncodedPart part)
	{
		this.parts.add(part);
	}

	public long getDate()
	{
		return date;
	}

	public List<MmsEncodedPart> getParts()
	{
		return parts;
	}

	public String getFrom()
	{
		return from;
	}

	public List<String> getTo()
	{
		return to;
	}

	public String getTransactionId()
	{
		return transactionId;
	}

	public String getSubject()
	{
		return subject;
	}

	public String getContentType()
	{
		return contentType;
	}

	public String getType()
	{
		return type;
	}
}

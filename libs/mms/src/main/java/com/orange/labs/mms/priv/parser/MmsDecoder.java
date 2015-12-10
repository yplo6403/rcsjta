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

import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.orange.labs.mms.priv.utils.Constants;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public final class MmsDecoder
{
	public static final int BCC = 0x81;
	public static final int CC = 0x82;
	public static final int CONTENT_LOCATION = 0x83;
	public static final int CONTENT_TYPE = 0x84;
	public static final int DATE = 0x85;
	public static final int DELIVERY_REPORT = 0x86;
	public static final int DELIVERY_TIME = 0x87;
	public static final int EXPIRY = 0x88;
	public static final int FROM = 0x89;
	public static final int MESSAGE_CLASS = 0x8A;
	public static final int MESSAGE_ID = 0x8B;
	public static final int MESSAGE_TYPE = 0x8C;
	public static final int MMS_VERSION = 0x8D;
	public static final int MESSAGE_SIZE = 0x8E;
	public static final int PRIORITY = 0x8F;
	public static final int READ_REPLY = 0x90;
	public static final int REPORT_ALLOWED = 0x91;
	public static final int RESPONSE_STATUS = 0x92;
	public static final int RESPONSE_TEXT = 0x93;
	public static final int SENDER_VISIBILITY = 0x94;
	public static final int STATUS = 0x95;
	public static final int SUBJECT = 0x96;
	public static final int TO = 0x97;
	public static final int TRANSACTION_ID = 0x98;
	public static final int FROM_ADDRESS_PRESENT_TOKEN = 0x80;
	public static final int FROM_INSERT_ADDRESS_TOKEN = 0x81;
	private static final String TAG = "MmsParser";
	/*--------------------------*
	* Some other useful arrays *
	*--------------------------*/
	private static final SparseIntArray MMS_YES_NO;
	private static final SparseArray<String> MMS_PRIORITY;
	private static final SparseArray<String> MMS_MESSAGE_CLASS;
	private static final SparseArray<String> MMS_CONTENT_TYPES;
	// character set (mibenum numbers by IANA, ored with 0x80)
	private static final SparseArray<String> MMS_CHARSET;
	/*--------------------------*
	* Array of header contents *
	*--------------------------*/
	private static SparseArray<String> MMS_MESSAGE_TYPES;
	
	static
	{
		MMS_MESSAGE_TYPES = new SparseArray<String>();
		MMS_MESSAGE_TYPES.put(0x80, "m-send-req");
		MMS_MESSAGE_TYPES.put(0x81, "m-send-conf");
		MMS_MESSAGE_TYPES.put(0x82, "m-notification-ind");
		MMS_MESSAGE_TYPES.put(0x83, "m-notifyresp-ind");
		MMS_MESSAGE_TYPES.put(0x84, "m-retrieve-conf");
		MMS_MESSAGE_TYPES.put(0x85, "m-acknowledge-ind");
		MMS_MESSAGE_TYPES.put(0x86, "m-delivery-ind");
	}

	static
	{
		MMS_YES_NO = new SparseIntArray();
		MMS_YES_NO.put(0x80, 1);
		MMS_YES_NO.put(0x81, 0);
	}
	
	static
	{
		MMS_PRIORITY = new SparseArray<String>();
		MMS_PRIORITY.put(0x80, "Low");
		MMS_PRIORITY.put(0x80, "Normal");
		MMS_PRIORITY.put(0x80, "High");
	}

	static
	{
		MMS_MESSAGE_CLASS = new SparseArray<String>();
		MMS_MESSAGE_CLASS.put(0x80, "Personnal");
		MMS_MESSAGE_CLASS.put(0x81, "Advertisement");
		MMS_MESSAGE_CLASS.put(0x82, "Informational");
		MMS_MESSAGE_CLASS.put(0x83, "Auto");
	}
	
	static
	{
		MMS_CONTENT_TYPES = new SparseArray<String>();
		MMS_CONTENT_TYPES.put(0x00, "*/*");
		MMS_CONTENT_TYPES.put(0x01, "text/*");
		MMS_CONTENT_TYPES.put(0x02, "text/html");
		MMS_CONTENT_TYPES.put(0x03, "text/plain");
		MMS_CONTENT_TYPES.put(0x04, "text/x-hdml");
		MMS_CONTENT_TYPES.put(0x05, "text/x-ttml");
		MMS_CONTENT_TYPES.put(0x06, "text/x-vCalendar");
		MMS_CONTENT_TYPES.put(0x07, "text/x-vCard");
		MMS_CONTENT_TYPES.put(0x08, "text/vnd.wap.wml");
		MMS_CONTENT_TYPES.put(0x09, "text/vnd.wap.wmlscript");
		MMS_CONTENT_TYPES.put(0x0A, "text/vnd.wap.wta-event");
		MMS_CONTENT_TYPES.put(0x0B, "multipart/*");
		MMS_CONTENT_TYPES.put(0x0C, "multipart/mixed");
		MMS_CONTENT_TYPES.put(0x0D, "multipart/form-data");
		MMS_CONTENT_TYPES.put(0x0E, "multipart/byterantes");
		MMS_CONTENT_TYPES.put(0x0F, "multipart/alternative");
		MMS_CONTENT_TYPES.put(0x10, "application/*");
		MMS_CONTENT_TYPES.put(0x11, "application/java-vm");
		MMS_CONTENT_TYPES.put(0x12, "application/x-www-form-urlencoded");
		MMS_CONTENT_TYPES.put(0x13, "application/x-hdmlc");
		MMS_CONTENT_TYPES.put(0x14, "application/vnd.wap.wmlc");
		MMS_CONTENT_TYPES.put(0x15, "application/vnd.wap.wmlscriptc");
		MMS_CONTENT_TYPES.put(0x16, "application/vnd.wap.wta-eventc");
		MMS_CONTENT_TYPES.put(0x17, "application/vnd.wap.uaprof");
		MMS_CONTENT_TYPES.put(0x18, "application/vnd.wap.wtls-ca-certificate");
		MMS_CONTENT_TYPES.put(0x19, "application/vnd.wap.wtls-user-certificate");
		MMS_CONTENT_TYPES.put(0x1A, "application/x-x509-ca-cert");
		MMS_CONTENT_TYPES.put(0x1B, "application/x-x509-user-cert");
		MMS_CONTENT_TYPES.put(0x1C, "image/*");
		MMS_CONTENT_TYPES.put(0x1D, "image/gif");
		MMS_CONTENT_TYPES.put(0x1E, "image/jpeg");
		MMS_CONTENT_TYPES.put(0x1F, "image/tiff");
		MMS_CONTENT_TYPES.put(0x20, "image/png");
		MMS_CONTENT_TYPES.put(0x21, "image/vnd.wap.wbmp");
		MMS_CONTENT_TYPES.put(0x22, "application/vnd.wap.multipart.*");
		MMS_CONTENT_TYPES.put(0x23, "application/vnd.wap.multipart.mixed");
		MMS_CONTENT_TYPES.put(0x24, "application/vnd.wap.multipart.form-data");
		MMS_CONTENT_TYPES.put(0x25, "application/vnd.wap.multipart.byteranges");
		MMS_CONTENT_TYPES.put(0x26, "application/vnd.wap.multipart.alternative");
		MMS_CONTENT_TYPES.put(0x27, "application/xml");
		MMS_CONTENT_TYPES.put(0x28, "text/xml");
		MMS_CONTENT_TYPES.put(0x29, "application/vnd.wap.wbxml");
		MMS_CONTENT_TYPES.put(0x2A, "application/x-x968-cross-cert");
		MMS_CONTENT_TYPES.put(0x2B, "application/x-x968-ca-cert");
		MMS_CONTENT_TYPES.put(0x2C, "application/x-x968-user-cert");
		MMS_CONTENT_TYPES.put(0x2D, "text/vnd.wap.si");
		MMS_CONTENT_TYPES.put(0x2E, "application/vnd.wap.sic");
		MMS_CONTENT_TYPES.put(0x2F, "text/vnd.wap.sl");
		MMS_CONTENT_TYPES.put(0x30, "application/vnd.wap.slc");
		MMS_CONTENT_TYPES.put(0x31, "text/vnd.wap.co");
		MMS_CONTENT_TYPES.put(0x32, "application/vnd.wap.coc");
		MMS_CONTENT_TYPES.put(0x33, "application/vnd.wap.multipart.related");
		MMS_CONTENT_TYPES.put(0x34, "application/vnd.wap.sia");
		MMS_CONTENT_TYPES.put(0x35, "text/vnd.wap.connectivity-xml");
		MMS_CONTENT_TYPES.put(0x36, "application/vnd.wap.connectivity-wbxml");
		MMS_CONTENT_TYPES.put(0x37, "application/pkcs7-mime");
		MMS_CONTENT_TYPES.put(0x38, "application/vnd.wap.hashed-certificate");
		MMS_CONTENT_TYPES.put(0x39, "application/vnd.wap.signed-certificate");
		MMS_CONTENT_TYPES.put(0x3A, "application/vnd.wap.cert-response");
		MMS_CONTENT_TYPES.put(0x3B, "application/xhtml+xml");
		MMS_CONTENT_TYPES.put(0x3C, "application/wml+xml");
		MMS_CONTENT_TYPES.put(0x3D, "text/css");
		MMS_CONTENT_TYPES.put(0x3E, "application/vnd.wap.mms-message");
		MMS_CONTENT_TYPES.put(0x3F, "application/vnd.wap.rollover-certificate");
		MMS_CONTENT_TYPES.put(0x40, "application/vnd.wap.locc+wbxml");
		MMS_CONTENT_TYPES.put(0x41, "application/vnd.wap.loc+xml");
		MMS_CONTENT_TYPES.put(0x42, "application/vnd.syncml.dm+wbxml");
		MMS_CONTENT_TYPES.put(0x43, "application/vnd.syncml.dm+xml");
		MMS_CONTENT_TYPES.put(0x44, "application/vnd.syncml.notification");
		MMS_CONTENT_TYPES.put(0x45, "application/vnd.wap.xhtml+xml");
		MMS_CONTENT_TYPES.put(0x46, "application/vnd.wv.csp.cir");
		MMS_CONTENT_TYPES.put(0x47, "application/vnd.oma.dd+xml");
		MMS_CONTENT_TYPES.put(0x48, "application/vnd.oma.drm.message");
		MMS_CONTENT_TYPES.put(0x49, "application/vnd.oma.drm.content");
		MMS_CONTENT_TYPES.put(0x4A, "application/vnd.oma.drm.rights+xml");
		MMS_CONTENT_TYPES.put(0x4B, "application/vnd.oma.drm.rights+wbxml");
	}

	static
	{
		MMS_CHARSET = new SparseArray<String>();
		MMS_CHARSET.put(0xEA, "utf-8");
		MMS_CHARSET.put(0x83, "ASCII");
		MMS_CHARSET.put(0x84, "iso-8859-1");
		MMS_CHARSET.put(0x85, "iso-8859-2");
		MMS_CHARSET.put(0x86, "iso-8859-3");
		MMS_CHARSET.put(0x87, "iso-8859-4");
	}
	
	private ArrayList<Integer> data;
	private int pos = 0;
	private ArrayList<MMSPart> parts;
	
	private String bcc;
	private String cc;
	private String content_location;
	private String content_type;
	private long date;
	private int delivery_report;
	private long expiry;
	private String from;
	private String message_class;
	private String message_id;
	private String message_type;
	private String mms_version;
	private long message_size;
	private String priority;
	private int read_reply;
	private int report_allowed;
	private int response_status;
	private String response_text;
	private int sender_visibility;
	private int status;
	private String subject;
	private List<String> to;
	private String transaction_id;
	
	private ArrayList<Integer> contenttype_params;
	
	// Constructor
	public MmsDecoder(byte[] bulkMms)
	{
		String hexMms = convertBytesToHex(bulkMms);
		
		this.data = new ArrayList<Integer>();
		
		for (int i=0; i<hexMms.length(); i+=2)
		{
			this.data.add(Integer.parseInt(String.valueOf(hexMms.charAt(i)) + String.valueOf(hexMms.charAt(i+1)), 16));
		}
		
		this.pos = 0;
		this.parts = new ArrayList<MMSPart>();
		this.to = new ArrayList<String>();
		this.contenttype_params = new ArrayList<Integer>();
	}
	
	public String getMessageType()
	{
		return this.message_type;
	}
	
	public String getTransactionId()
	{
		return this.transaction_id;
	}
	
	public String getMessageId()
	{
		return this.message_id;
	}
	
	public int getResponseStatus()
	{
		return this.response_status;
	}
	
	public String getResponseText()
	{
		return this.response_text;
	}
	
	public String getFrom()
	{
		return this.from.substring(0, this.from.indexOf("/TYPE") != -1 ? this.from.indexOf("/TYPE") : this.from.length());
	}
	
	public String getSubject()
	{
		return this.subject;
	}
	
	// This function is called when the data is to be parsed
	public boolean parse()
	{
		this.pos = 0;
		
		while (parseHeader());
		
		if ("application/vnd.wap.multipart.related".equals(this.content_type) ||"application/vnd.wap.multipart.mixed".equals(this.content_type))
		{
			while (parseParts());
		}
		else return false;
		
		return true;
	}
	
	public ArrayList<MMSPart> getParts()
	{
		return this.parts;
	}
	
	/*---------------------------------------------------*
	* This function checks what kind of field is to be *
	* parsed at the moment *
	* *
	* If true is returned, the class will go on and *
	* and continue decode the header. If false, the *
	* class will end the header decoding. *
	*---------------------------------------------------*/
	private boolean parseHeader()
	{
		if (this.pos >= this.data.size()) return false;
		
		switch (this.data.get(this.pos++))
		{
			case MESSAGE_TYPE:
				this.message_type = MMS_MESSAGE_TYPES.get(this.data.get(this.pos++));
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] MESSAGE_TYPE: " + this.message_type);
				break;
		
			case TRANSACTION_ID:
				this.transaction_id = this.parseTextString();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] TRANSACTION_ID: " + this.transaction_id);
				break;
				
			case MMS_VERSION:
				int vMaj = (this.data.get(this.pos) & (byte)0x70) >> 4;
				int vMin = (this.data.get(this.pos++) & (byte)0x0F);
				this.mms_version = vMaj + "." + vMin;
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] MMS_VERSION: " + this.mms_version);
				break;
				
			case TO:
				this.to.add(this.parseEncodedStringValue());
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] TO: " + this.to);
				break;
				
			case SUBJECT:
				this.subject = this.parseEncodedStringValue();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] SUBJECT: " + this.subject);
				break;
				
			case FROM:
				this.from = this.parseFromValue();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] FROM: " + this.from);
				break;
				
			case MESSAGE_ID:
				this.message_id = this.parseTextString();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] MESSAGE_ID: " + this.message_id);
				break;
				
			case DATE:
				this.date = this.parseLongInteger();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] DATE: " + this.date);
				break;
				
			case DELIVERY_REPORT:
				this.delivery_report = MMS_YES_NO.get(this.data.get(this.pos++));
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] DELIVERY_REPORT: " + this.delivery_report);
				break;
				
			case CONTENT_TYPE:
				if (this.data.get(this.pos) <= 31)
				{
					this.parseValueLength();
					if (this.data.get(this.pos) > 31 && this.data.get(this.pos) < 128)
					{
						this.content_type = this.parseTextString();
					}
					else
					{
						this.content_type = MMS_CONTENT_TYPES.get(this.parseIntegerValue());
					}
				}
				else if (this.data.get(this.pos) < 128)
				{
					this.content_type = this.parseTextString();
				}
				else
				{
					this.content_type = MMS_CONTENT_TYPES.get(this.parseShortInteger());
				}
				
				boolean noparams = false;
				while (!noparams)
				{
					switch (this.data.get(this.pos))
					{
						case 0x89:
							this.pos ++;
							this.parseTextString();
							break;
							
						case 0x8A:
							this.pos ++;
							if (this.data.get(this.pos) < 128)
							{
								this.pos ++;
								this.parseTextString();
							}
							else
							{
								this.contenttype_params.add(this.parseShortInteger());
							}
							break;
							
						default:
							noparams = true;
							break;
					}
				}

				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] CONTENT_TYPE: " + this.content_type);
				break;
		
			case BCC:
				this.bcc = this.parseEncodedStringValue();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] BCC: " + this.bcc);
				break;
				
			case CC:
				this.cc = this.parseEncodedStringValue();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] CC: " + this.cc);
				break;
				
			case CONTENT_LOCATION:
				this.content_location = this.parseTextString();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] CONTENT_LOCATION: " + this.content_location);
				break;
				
			case DELIVERY_TIME:
				break;
				
			case EXPIRY:
				this.expiry = this.parseLongInteger();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] EXPIRY: " + this.expiry);
				break;
				
			case MESSAGE_CLASS:
				this.message_class = this.parseMessageClassValue();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] MESSAGE_CLASS: " + this.message_class);
				break;
	
			case MESSAGE_SIZE:
				this.message_size = this.parseLongInteger();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] MESSAGE_SIZE: " + this.message_size);
				break;
				
			case PRIORITY:
				this.priority = MMS_PRIORITY.get(this.data.get(this.pos++));
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] PRIORITY: " + this.priority);
				break;
				
			case READ_REPLY:
				this.read_reply = MMS_YES_NO.get(this.data.get(this.pos++));
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] READ_REPLY: " + this.read_reply);
				break;
				
			case REPORT_ALLOWED:
				this.report_allowed = MMS_YES_NO.get(this.data.get(this.pos++));
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] REPORT_ALLOWED: " + this.report_allowed);
				break;
				
			case RESPONSE_STATUS:
				this.response_status = this.data.get(this.pos++);
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] RESPONSE_STATUS: " + this.response_status);
				break;
				
			case RESPONSE_TEXT:
				this.response_text = this.parseEncodedStringValue();
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] RESPONSE_TEXT: " + this.response_text);
				break;
				
			case SENDER_VISIBILITY:
				this.sender_visibility = MMS_YES_NO.get(this.data.get(this.pos++));
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] SENDER_VISIBILITY: " + this.sender_visibility);
				break;
				
			case STATUS:
				this.status = this.data.get(this.pos++);
				if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] STATUS: " + this.status);
				break;
				
			default:
				if (this.data.get(this.pos - 1) > 127)
				{
					if (Constants.DEBUG) Log.e(Constants.TAG, "[MMS PARSER] Unknown field (" + this.data.get(this.pos - 1) + ") for pos " + this.pos);
				}
				else
				{
					this.pos --;
					return false;
				}
				break;
		}
		
		return true;
	}
	
	/*---------------------------------------------------------------------*
	* Function called after header has been parsed. This function fetches *
	* the different parts in the MMS. Returns true until it encounter end *
	* of data. *
	*---------------------------------------------------------------------*/
	private boolean parseParts()
	{
		if (this.pos >= this.data.size()) return false;
		
		int count = this.parseUint();
		if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] Found " + count + " parts !");
		
		String ctype;
		for (int i=0; i<count; i++)
		{
			int[] header = null;
			int[] data = null;
			ctype = null;
			
			int headerlen = this.parseUint();
			int datalen = this.parseUint();
			
			header = new int[headerlen];
			data = new int[datalen];
			
			int ctypepos = this.pos;
			
			if (this.data.get(this.pos) <= 31)
			{
				this.parseValueLength();
				if (this.data.get(this.pos) > 31 && this.data.get(this.pos) < 128)
				{
					ctype = this.parseTextString();
				}
				else
				{
					ctype = MMS_CONTENT_TYPES.get(this.parseIntegerValue());
				}
			}
			else if (this.data.get(this.pos) < 128)
			{
				ctype = this.parseTextString();
			}
			else
			{
				ctype = MMS_CONTENT_TYPES.get(this.parseShortInteger());
			}
			
			this.pos = ctypepos;
			
			// Read header
			for (int j=0; j<headerlen; j++)
			{
				header[j] = this.data.get(this.pos++);
			}
			
			// read data
			for (int j=0; j<datalen; j++)
			{
				data[j] = this.data.get(this.pos++);
			}
			
			if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] Part #" + i + "; Header len " + headerlen);
			if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] Part #" + i + "; Data len " + datalen);
			if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] Part #" + i + "; Content-type " + ctype);
			
			this.parts.add(new MMSPart(ctype, header, data));
		}
		
		return false;
	}
	
	/*-------------------------------------------------------------------------------------------------*
	* Parse From-value *
	* From-value = Value-length (Address-present-token Encoded-string-value | Insert-address-token ) *
	* *
	* Address-present-token = <Octet 128> *
	* Insert-address-token = <Octet 129> *
	*-------------------------------------------------------------------------------------------------*/
	private String parseFromValue()
	{
		int len = this.parseValueLength();
		
		if (this.data.get(this.pos) == FROM_ADDRESS_PRESENT_TOKEN)
		{
			if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] Address-present-token found");
			this.pos ++;
			return this.parseEncodedStringValue();
		}
		else if (this.data.get(this.pos) == FROM_INSERT_ADDRESS_TOKEN)
		{
			if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] Insert-address-token found");
			this.pos ++;
			return "";
		}
		else
		{
			if (Constants.DEBUG) Log.v(Constants.TAG, "[MMS PARSER] No from token found, trying to skip the value field by jumping " + len + " bytes");
			this.pos += len;
		}
		
		return "";
	}
	
	/*-------------------------------------------------------------------*
	* Parse message-class *
	* message-class-value = Class-identifier | Token-text *
	* Class-idetifier = Personal | Advertisement | Informational | Auto *
	*-------------------------------------------------------------------*/
	private String parseMessageClassValue()
	{
		if (this.data.get(this.pos) > 127)
			return MMS_MESSAGE_CLASS.get(this.data.get(this.pos++));
		else
			return this.parseTextString();
	}
	
	/*----------------------------------------------------------------*
	* Parse Text-string *
	* text-string = [Quote <Octet 127>] text [End-string <Octet 00>] *
	*----------------------------------------------------------------*/
	private String parseTextString()
	{
		StringBuilder sb = new StringBuilder();
		if (this.data.get(this.pos) == (byte)0x7F) this.pos ++;
		
		char c;
		while (this.data.get(this.pos) != 0)
		{
			c = (char) this.data.get(this.pos++).intValue();
			sb.append(c);
		}
		this.pos ++;
		
		return sb.toString();
	}
	
	/*------------------------------------------------------------------------*
	* Parse Encoded-string-value *
	* *
	* Encoded-string-value = Text-string | Value-length Char-set Text-string *
	* *
	*------------------------------------------------------------------------*/
	private String parseEncodedStringValue()
	{
		if (this.data.get(this.pos) <= 31)
		{
			this.parseValueLength();
			int mibnum = this.data.get(this.pos++);
			
			String charset;
			if (MMS_CHARSET.get(mibnum) != null) charset = MMS_CHARSET.get(mibnum);
			else charset = "";
				
			String raw = this.parseTextString();
			if (charset.equals("utf-8")) try {
				raw = new String(raw.getBytes(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			return raw;
		}
		
		return this.parseTextString();
	}
	
	/*--------------------------------------------------------------------------------*
	* Parse Value-length *
	* Value-length = Short-length<Octet 0-30> | Length-quote<Octet 31> Length<Uint> *
	* *
	* A list of content-types of a MMS message can be found here: *
	* http://www.wapforum.org/wina/wsp-content-type.htm *
	*--------------------------------------------------------------------------------*/
	private int parseValueLength()
	{
		if (this.data.get(this.pos) < 31)
		{
			return this.data.get(this.pos++);
		}
		else if (this.data.get(this.pos) == 31)
		{
			this.pos ++;
			return this.parseUint();
		}
		else
		{
			if (Constants.DEBUG) Log.e(Constants.TAG, "[MMS PARSER] Short-length-octet (" + this.data.get(this.pos) + ") > 31 in Value-length at offset " + this.pos + "!");
			return 0;
		}
	}
	
	/*--------------------------------------------------------------------------*
	* Parse Long-integer *
	* Long-integer = Short-length<Octet 0-30> Multi-octet-integer<1*30 Octets> *
	*--------------------------------------------------------------------------*/
	private long parseLongInteger()
	{
		int octetcount = this.data.get(this.pos++);
		
		if (octetcount > 30)
		{
			if (Constants.DEBUG) Log.e(Constants.TAG, "[MMS PARSER] Short-length-octet (" + this.data.get(this.pos - 1) + ") > 30 in Value-length at offset " + (this.pos - 1) + "!");
			return 0;
		}
		
		long longint = 0L;
		for (int i=0; i<octetcount; i++)
		{
			longint = longint << 8;
			longint += this.data.get(this.pos++);
		}
		
		return longint;
	}
	
	/*------------------------------------------------------------------------*
	* Parse Short-integer *
	* Short-integer = OCTET *
	* Integers in range 0-127 shall be encoded as a one octet value with the *
	* most significant bit set to one, and the value in the remaining 7 bits *
	*------------------------------------------------------------------------*/
	private int parseShortInteger()
	{
		return this.data.get(this.pos++) & (byte)0x7F;
	}
	
	/*-------------------------------------------------------------*
	* Parse Integer-value *
	* Integer-value = short-integer | long-integer *
	* *
	* This function checks the value of the current byte and then *
	* calls either parseLongInt() or parseShortInt() depending on *
	* what value the current byte has *
	*-------------------------------------------------------------*/
	private int parseIntegerValue()
	{
		if (this.data.get(this.pos) < 31)
		{
			return (int) this.parseLongInteger();
		}
		else if (this.data.get(this.pos) > 127)
		{
			return this.parseShortInteger();
		}
		else
		{
			if (Constants.DEBUG) Log.e(Constants.TAG, "[MMS PARSER] Not a IntegerValue field at pos " + this.pos);
			this.pos ++;
			return 0;
		}
	}
	
	/*------------------------------------------------------------------*
	* Parse Unsigned-integer *
	* *
	* The value is stored in the 7 last bits. If the first bit is set, *
	* then the value continues into the next byte. *
	* *
	* http://www.nowsms.com/discus/messages/12/522.html *
	*------------------------------------------------------------------*/
	private int parseUint()
	{
		int uint = 0;
		
		while ((this.data.get(this.pos) & (byte)0x80) != 0)
		{
			uint = uint << 7;
			uint |= this.data.get(this.pos++) & (byte)0x7F;
		}
		
		uint = uint << 7;
		uint |= this.data.get(this.pos++) & (byte)0x7F;
		
		return uint;
	}
	
	
	public String convertBytesToHex(byte[] array)
	{
		String result = "";
        for (int i=0; i<array.length; i++)
        {
            result += Integer.toString( ( array[i] & 0xff ) + 0x100, 16).substring(1);
        }
        return result;
	}
	
	/*---------------------------------------------------------------------*
	* The MMS part class *
	* An instance of this class contains one part of a MMS message. *
	* *
	* The multipart type is formed as: *
	* number |part1|part2|....|partN *
	* where part# is formed by headerlen|datalen|contenttype|headers|data *
	*---------------------------------------------------------------------*/
	public class MMSPart
	{
		private int[] header;
		private String content_type;
		private int[] data;
		
		public MMSPart(String content_type, int[] header, int[] data)
		{
			this.content_type = content_type;
			this.header = header;
			this.data = data;
		}
		
		public String getContentType()
		{
			return this.content_type;
		}
		
		public byte[] getHeader()
		{
			byte[] headerBytes = new byte[header.length];
			for (int i=0; i<header.length; i++)
			{
				headerBytes[i] = (byte) header[i];
			}
			return headerBytes;
		}
		
		public byte[] getContent()
		{
			byte[] dataBytes = new byte[data.length];
			for (int i=0; i<data.length; i++)
			{
				dataBytes[i] = (byte) data[i];
			}
			return dataBytes;
		}
	}
}

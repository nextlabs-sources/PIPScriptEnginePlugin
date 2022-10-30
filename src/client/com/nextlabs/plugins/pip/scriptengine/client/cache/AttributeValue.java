/*
 * Copyright 2017 by Nextlabs Inc.
 *
 * All rights reserved worldwide.
 * Created on 11 Aug 2017
 * 
 * MLI - amilasilva88@gmail.com
 * 
 */
package com.nextlabs.plugins.pip.scriptengine.client.cache;

import static java.lang.System.currentTimeMillis;

import java.io.Serializable;

/**
 * Attribute cache value with time to live properties
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public class AttributeValue implements Serializable {

	private static final long serialVersionUID = -6254306246809761411L;

	private String value;
	private long ttlInMins;
	private long createdTime;

	public AttributeValue() {

	}

	/**
	 * Create AttributeValue instance
	 * 
	 * @param value
	 *            attribute value
	 * @param ttlInMins
	 *            time to live in minutes
	 * @param createdTime
	 *            created time
	 * @return {@link AttributeValue}
	 */
	public static AttributeValue create(String value, long ttlInMins, long createdTime) {
		AttributeValue attrValue = new AttributeValue();
		attrValue.value = value;
		attrValue.ttlInMins = ttlInMins;
		attrValue.createdTime = createdTime;
		return attrValue;
	}

	public String getValue() {
		return value;
	}

	public long getTtlInMins() {
		return ttlInMins;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	/**
	 * Check value is expired according to given time to live configuration
	 * 
	 * @return true if expired otherwise false
	 */
	public boolean isExpired() {
		return (this.createdTime + (ttlInMins * 60 * 1000)) <= currentTimeMillis();
	}

	@Override
	public String toString() {
		return String.format("AttributeValue [value=%s, ttlInMins=%s, createdTime=%s]", value, ttlInMins, createdTime);
	}

}

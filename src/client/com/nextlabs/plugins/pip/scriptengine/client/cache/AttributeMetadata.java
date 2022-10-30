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

import java.io.Serializable;

/**
 * Attribute cache key
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public class AttributeMetadata implements Serializable {

	private static final long serialVersionUID = -4749641314841197386L;
	private Long tenantId;
	private String attributeName;
	private Long ttl;

	public AttributeMetadata() {
	}

	/**
	 * Create attribute key
	 * 
	 * @param attributeName
	 *            attribute name
	 * @param ttl
	 *            time to live
	 * @return {@link AttributeMetadata}
	 */
	public static AttributeMetadata create(String attributeName, Long ttl) {
		AttributeMetadata key = new AttributeMetadata();
		key.attributeName = attributeName;
		key.ttl = ttl;
		return key;
	}

	public Long getTenantId() {
		return tenantId;
	}

	public void setTenantId(Long tenantId) {
		this.tenantId = tenantId;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public Long getTtl() {
		return ttl;
	}

	@Override
	public int hashCode() {
		final int prime = 61;
		int result = 1;
		result = prime * result + ((attributeName == null) ? 0 : attributeName.hashCode());
		result = prime * result + ((ttl == null) ? 0 : ttl.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeMetadata other = (AttributeMetadata) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		if (ttl == null) {
			if (other.ttl != null)
				return false;
		} else if (!ttl.equals(other.ttl))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("AttributeMetadata [tenantId=%s, attributeName=%s, ttl=%s]", tenantId, attributeName, ttl);
	}

}

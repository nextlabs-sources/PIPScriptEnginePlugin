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
public class AttributeKey implements Serializable {

	private static final long serialVersionUID = -4749641314841197386L;
	private Long tenantId;
	private String entityId;
	private String entityType;
	private String modelType;
	private String attributeName;

	public AttributeKey() {
	}

	/**
	 * Create attribute key
	 * 
	 * @param entityId
	 *            entity id
	 * @param entityType
	 *            entity type
	 * @param modelType
	 *            policy model type
	 * @param attributeName
	 *            attribute name
	 * 
	 * @return {@link AttributeKey}
	 */
	public static AttributeKey create(String entityId, String entityType, String modelType, String attributeName) {
		AttributeKey key = new AttributeKey();
		key.entityId = entityId;
		key.entityType = entityType;
		key.modelType = modelType;
		key.attributeName = attributeName;
		return key;
	}

	public Long getTenantId() {
		return tenantId;
	}

	public String getEntityId() {
		return entityId;
	}

	public String getEntityType() {
		return entityType;
	}

	public String getModelType() {
		return modelType;
	}

	public String getAttributeName() {
		return attributeName;
	}

	@Override
	public int hashCode() {
		final int prime = 41;
		int result = 1;
		result = prime * result + ((attributeName == null) ? 0 : attributeName.hashCode());
		result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
		result = prime * result + ((modelType == null) ? 0 : modelType.hashCode());
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
		AttributeKey other = (AttributeKey) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (entityType == null) {
			if (other.entityType != null)
				return false;
		} else if (!entityType.equals(other.entityType))
			return false;
		if (modelType == null) {
			if (other.modelType != null)
				return false;
		} else if (!modelType.equals(other.modelType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("AttributeKey [tenantId=%s, entityId=%s, entityType=%s, modelType=%s, attributeName=%s]",
				tenantId, entityId, entityType, modelType, attributeName);
	}

}

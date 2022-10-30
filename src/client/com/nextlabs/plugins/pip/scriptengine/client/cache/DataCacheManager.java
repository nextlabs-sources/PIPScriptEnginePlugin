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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

/**
 * Data cache manager to manage data caches.
 * 
 * This will manage two caches - script data cache - attribute cache with
 * specific ttl
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public final class DataCacheManager {

	private static final Log log = LogFactory.getLog(DataCacheManager.class);

	private static final DataCacheManager instance = new DataCacheManager();

	private static final String META_DATA_CACHE = "scriptMetaDataCache";
	private static final String ATTR_DATA_CACHE = "attributeDataCache";

	private CacheManager scriptMetaDataCacheMgr = null;
	private CacheManager attributeCacheMgr = null;

	private DataCacheManager() {

	}

	/**
	 * Get Data Cache manager singleton instance
	 * 
	 * @return {@link DataCacheManager}
	 */
	public static DataCacheManager getInstance() {
		return instance;
	}

	public Cache<AttributeKey, AttributeMetadata> getScriptMetaDataCache() {
		if (scriptMetaDataCacheMgr == null) {
			synchronized (scriptMetaDataCacheMgr) {
				scriptMetaDataCacheMgr = CacheManagerBuilder.newCacheManagerBuilder()
						.withCache(META_DATA_CACHE,
								CacheConfigurationBuilder
										.newCacheConfigurationBuilder(AttributeKey.class, AttributeMetadata.class,
												ResourcePoolsBuilder.newResourcePoolsBuilder()
														.heap(4000, EntryUnit.ENTRIES).offheap(20, MemoryUnit.MB)))
						.build();
				scriptMetaDataCacheMgr.init();
				log.info("Script meta-data cache initialized successfully");
			}
		}
		Cache<AttributeKey, AttributeMetadata> cache = scriptMetaDataCacheMgr.getCache(META_DATA_CACHE,
				AttributeKey.class, AttributeMetadata.class);

		return cache;
	}

	/**
	 * Add attribute meta-data tocache
	 * 
	 * @param entityId
	 *            entity id
	 * @param entityType
	 *            entity type
	 * @param modelType
	 *            policy model type
	 * @param attributeName
	 *            attribute name
	 * @param attributeValue
	 *            value of the attribute
	 * @param ttlInMins
	 *            time to live in minutes
	 */
	public void addMetadata(String entityId, String entityType, String modelType, String attributeName,
			long ttlInMins) {
		AttributeKey key = AttributeKey.create(entityId, entityType, modelType, attributeName);
		AttributeMetadata metadata = AttributeMetadata.create(attributeName, ttlInMins);

		getScriptMetaDataCache().put(key, metadata);

		if (log.isDebugEnabled()) {
			log.debug("Attribute metadata added to metadata cache: [ meta-data: " + key + "]");
		}
	}

	/**
	 * Get attribute meta-data by given key
	 * 
	 * @param key
	 *            {@link AttributeKey}
	 * @return {@link AttributeMetadata}
	 */
	public AttributeMetadata getMetadata(AttributeKey key) {
		AttributeMetadata metadata = getScriptMetaDataCache().get(key);
		return metadata;
	}

	public Cache<AttributeKey, AttributeValue> getAttributeCache() {
		if (attributeCacheMgr == null) {
			synchronized (attributeCacheMgr) {
				attributeCacheMgr = CacheManagerBuilder.newCacheManagerBuilder()
						.withCache(ATTR_DATA_CACHE,
								CacheConfigurationBuilder
										.newCacheConfigurationBuilder(AttributeKey.class, AttributeValue.class,
												ResourcePoolsBuilder.newResourcePoolsBuilder()
														.heap(4000, EntryUnit.ENTRIES).offheap(20, MemoryUnit.MB))
										.withExpiry(Expirations.timeToLiveExpiration(Duration.of(4, TimeUnit.HOURS))))
						.build();
				attributeCacheMgr.init();
				log.info("Attribute data cache initialized successfully");
			}
		}
		Cache<AttributeKey, AttributeValue> cache = attributeCacheMgr.getCache(ATTR_DATA_CACHE, AttributeKey.class,
				AttributeValue.class);

		return cache;
	}

	/**
	 * Add attribute value to attribute cache
	 * 
	 * @param entityId
	 *            entity id
	 * @param entityType
	 *            entity type
	 * @param modelType
	 *            policy model type
	 * @param attributeName
	 *            attribute name
	 * @param attributeValue
	 *            value of the attribute
	 * @param ttlInMins
	 *            time to live in minutes
	 */
	public void addAttribute(String entityId, String entityType, String modelType, String attributeName,
			String attributeValue, long ttlInMins) {
		AttributeKey key = AttributeKey.create(entityId, entityType, modelType, attributeName);

		if (ttlInMins <= 0) {
			AttributeMetadata metadata = getMetadata(key);
			ttlInMins = (metadata != null) ? metadata.getTtl() : ttlInMins;
		}
		AttributeValue value = AttributeValue.create(attributeValue, ttlInMins, System.currentTimeMillis());

		getAttributeCache().put(key, value);

		if (log.isDebugEnabled()) {
			log.debug("Value added to attribute cache: [ key: " + key + ", value: " + attributeValue + "]");
		}
	}

	/**
	 * Get attribute value by given key
	 * 
	 * @param key
	 *            {@link AttributeKey}
	 * @return value
	 */
	public String getAttributeValue(AttributeKey key) {
		AttributeValue attrValue = getAttributeCache().get(key);
		if (attrValue != null) {
			return attrValue.isExpired() ? null : attrValue.getValue();
		} else {
			if (log.isInfoEnabled()) {
				log.info("value not found in attribute cache: [ key: " + key + "]");
			}
		}
		return null;
	}

	/**
	 * Shutdown the data cache manager
	 * 
	 */
	public void shutdown() {
		if (scriptMetaDataCacheMgr != null) {
			scriptMetaDataCacheMgr.close();
		}

		if (attributeCacheMgr != null) {
			attributeCacheMgr.close();
		}
	}
}

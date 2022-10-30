/*
 * Copyright 2017 by Nextlabs Inc.
 *
 * All rights reserved worldwide.
 * Created on 8 Aug 2017
 * 
 * MLI - amilasilva88@gmail.com
 */
package com.nextlabs.plugins.pip.scriptengine;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.nextlabs.destiny.pip.scriptengine.dto.ScriptData;
import com.nextlabs.destiny.pip.scriptengine.dto.ScriptMetadata;
import com.nextlabs.destiny.pip.scriptengine.engine.ScriptedEngine;
import com.nextlabs.destiny.pip.scriptengine.engine.ScriptedEngineFactory;
import com.nextlabs.destiny.pip.scriptengine.exceptions.ScriptEngineException;
import com.nextlabs.plugins.pip.scriptengine.client.PIPScriptEngineClient;
import com.nextlabs.plugins.pip.scriptengine.client.cache.AttributeKey;
import com.nextlabs.plugins.pip.scriptengine.client.cache.DataCacheManager;
import com.nextlabs.plugins.pip.scriptengine.dto.ScriptEngineHBResponse;
import com.nextlabs.plugins.pip.scriptengine.exceptions.PIPPluginException;

/**
 * Script Attribute data provider, this class provides the subject and resource
 * attribute value to PDP during policy evaluation
 * 
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public class ScriptDataProvider {

	private static final Log log = LogFactory.getLog(ScriptDataProvider.class);

	private static ScriptDataProvider instance = null;
	private boolean enableCache;
	private String scriptLoadingMode;
	private String scriptFolder;
	private Properties properties;

	private DataCacheManager cacheManager;

	private ScriptDataProvider() {

	}

	/**
	 * Get Script data provider instance
	 * 
	 * @return {@link ScriptDataProvider}
	 */
	public static ScriptDataProvider getInstance(Properties properties) {
		if (instance == null) {
			synchronized (ScriptDataProvider.class) {
				if (instance == null) {
					instance = new ScriptDataProvider();
					instance.setProperties(properties);
				}
			}
		}

		return instance;
	}

	/**
	 * Initialize Data provider
	 * 
	 * @throws PIPPluginException
	 *             throws at any error
	 */
	public void init() throws PIPPluginException {
		this.enableCache = Boolean.valueOf(properties.getProperty(PIPScriptEngineClient.enable_cache));
		this.scriptLoadingMode = properties.getProperty(PIPScriptEngineClient.script_mode);
		this.scriptFolder = properties.getProperty(PIPScriptEngineClient.script_folder);
		this.cacheManager = DataCacheManager.getInstance();

		switch (scriptLoadingMode) {
		case "local":
			handleLocalFiles();
			break;

		default:
			handleLocalFiles();
			break;
		}
		log.info("Data provider initialization successfull.");
	}

	public String getAttribute(String entityId, String entityType, String modelType, String attributeName) {
		if (enableCache) {
			String cachedAttribute = cacheManager
					.getAttributeValue(AttributeKey.create(entityId, entityType, modelType, attributeName));
			if (cachedAttribute != null) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("cached attribute found for: [%s : %s : %s : %s]", entityId, entityType,
							modelType, attributeName));
				}
				return cachedAttribute;
			}
		}

		ScriptMetadata metadata = ScriptMetadata.create(entityType, modelType, attributeName);
		ScriptedEngine engine = ScriptedEngineFactory.getEngine(metadata);
		if (engine == null) {
			log.info("No scripted engine found for key: " + metadata);
			return null;
		}
		long startTime = System.nanoTime();
		JSONObject attributeResponse = engine.getAttribute(entityId, entityType, modelType, attributeName);
		String attributeValue = (String) attributeResponse.get("attributeValue");
		refreshCache(entityId, entityType, modelType, attributeResponse);

		if (log.isDebugEnabled()) {
			log.debug("Retrive attribute from script took: [" + (System.nanoTime() - startTime) + "ns]");
		}
		return attributeValue;
	}

	private void refreshCache(String entityId, String entityType, String modelType, JSONObject attributeResponse) {

		if (!enableCache) {
			return;
		}
		String attrName = (String) attributeResponse.get("attributeId");
		String attrValue = (String) attributeResponse.get("attributeValue");
		cacheManager.addAttribute(entityId, entityType, modelType, attrName, attrValue, -1);
		JSONArray extraAttributes = attributeResponse.containsKey("extraAttributes")
				? (JSONArray) attributeResponse.get("extraAttributes") : null;
		if (extraAttributes != null && extraAttributes.size() > 0) {
			for (int i = 0; i < extraAttributes.size(); i++) {
				JSONObject attrData = (JSONObject) extraAttributes.get(i);
				attrName = (String) attrData.get("attributeId");
				attrValue = (String) attrData.get("attributeValue");
				cacheManager.addAttribute(entityId, entityType, modelType, attrName, attrValue, -1);
			}
		}
	}

	private void handleLocalFiles() {
		final File scriptDir = new File(this.scriptFolder);
		log.info("Plugin is running in local mode, This will load the scripts from :" + scriptDir.getAbsolutePath());

		for (File file : scriptDir.listFiles()) {
			try {
				ScriptedEngineFactory.loadScriptedEngine(file);
			} catch (ScriptEngineException e) {
				log.error("Error occurred in loading scripted engine,", e);
			}
		}

		// start file change listener
		Thread fileWatcher = new Thread() {
			public void run() {
				try {
					WatchService watcher = FileSystems.getDefault().newWatchService();
					scriptDir.toPath().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
					log.info("watching file change in directory " + scriptDir);
					while (true) {
						try {
							WatchKey key = watcher.take(); // blocking
							try {
								Thread.sleep(100);
								List<WatchEvent<?>> events = key.pollEvents();
								handleFileChangeEvent(scriptDir, events);
							} catch (Exception e) {
								log.error("Exception while watching file", e);
							}
							key.reset();
						} catch (InterruptedException ex) {
							log.error("File watcher interrupted", ex);
						}
					}
				} catch (Exception e) {
					log.error("File watcher exception", e);
				}
			}

			private void handleFileChangeEvent(final File scriptDir, List<WatchEvent<?>> events)
					throws IOException, ScriptEngineException {
				for (WatchEvent<?> e : events) {
					Kind<?> kind = e.kind();
					String path = ((Path) e.context()).toString();
					log.info("File change detected, File " + path + ", Change type is " + kind);

					File scriptFile = new File(scriptDir, path);
					if (ENTRY_DELETE.equals(kind)) {
						ScriptedEngineFactory.removeEngine(scriptFile.getCanonicalPath());

					} else if (ENTRY_CREATE.equals(kind)) {
						ScriptedEngineFactory.loadScriptedEngine(new File(scriptDir, path));

					} else if (ENTRY_MODIFY.equals(kind)) {
						ScriptedEngineFactory.removeEngine(scriptFile.getCanonicalPath());
						ScriptedEngineFactory.loadScriptedEngine(scriptFile);
					}
				}
			};
		};
		fileWatcher.setName("AS-script-file-watcher");
		fileWatcher.start();
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Reload the script data
	 * 
	 * @param response
	 *            {@link ScriptEngineHBResponse}
	 * @throws PIPPluginException
	 *             throws at any error
	 */
	public void reloadScripts(ScriptEngineHBResponse response) throws PIPPluginException {

		if (log.isDebugEnabled()) {
			log.debug(String.format("Request came to reload the scripts [ new/updates: %s, deleted: %s ]",
					response.getScriptSize(), response.getDeletedScriptSize()));
		}

		// remove deleted files
		for (String scriptName : response.getDeletedScriptNames()) {
			try {
				Files.deleteIfExists(Paths.get(scriptFolder, scriptName));
			} catch (Exception e) {
				log.error("Error encountered in delete file: [ script: " + scriptName + "]", e);
			}
		}

		for (ScriptData scriptData : response.getScriptDataList()) {
			Path filePath = Paths.get(scriptFolder, scriptData.getFilename());
			try (OutputStream os = new BufferedOutputStream(
					Files.newOutputStream(filePath, CREATE, WRITE, TRUNCATE_EXISTING))) {
				os.write(scriptData.getScriptText().getBytes(), 0, scriptData.getScriptText().length());
				os.flush();
			} catch (Exception e) {
				log.error("Error encountered in create/update file: [ script: " + scriptData.getFilename() + "]", e);
			}
		}
	}

}

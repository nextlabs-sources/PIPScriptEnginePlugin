/*
 * Copyright 2017 by Nextlabs Inc.
 *
 * All rights reserved worldwide.
 * Created on 10 Aug 2017
 * 
 * MLI - amilasilva88@gmail.com
 * 
 */
package com.nextlabs.plugins.pip.scriptengine.client;

import static com.bluejungle.framework.expressions.ValueType.STRING;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.utils.SerializationUtils;
import com.bluejungle.pf.domain.destiny.serviceprovider.IHeartbeatServiceProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.IResourceAttributeProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.ISubjectAttributeProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.ServiceProviderException;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.epicenter.resource.IResource;
import com.nextlabs.pf.domain.destiny.serviceprovider.IConfigurableServiceProvider;
import com.nextlabs.plugins.pip.scriptengine.ScriptDataProvider;
import com.nextlabs.plugins.pip.scriptengine.dto.ScriptEngineHBRequest;
import com.nextlabs.plugins.pip.scriptengine.dto.ScriptEngineHBResponse;
import com.nextlabs.plugins.pip.scriptengine.exceptions.PIPPluginException;

/**
 * PIP Scripting Engine client plug-in main invoking point
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public class PIPScriptEngineClient implements IHeartbeatServiceProvider, IConfigurableServiceProvider,
		IResourceAttributeProvider, ISubjectAttributeProvider {

	private static final Log log = LogFactory.getLog(PIPScriptEngineClient.class);

	private ClassLoader classLoader = getClass().getClassLoader();

	public Properties properties;

	public static final String client_id = "client.id";
	public static final String enable_cache = "enable.cache";
	public static final String script_mode = "script.mode";
	public static final String script_folder = "script.folder";

	private static Long lastRequestTime = -1L;

	private ScriptDataProvider dataProvider;

	public PIPScriptEngineClient() {
	}

	@Override
	public Serializable prepareRequest(String id) {
		if (ScriptEngineHBRequest.PLUGIN.equals(id)) {
			String clientId = properties.getProperty(client_id);
			ScriptEngineHBRequest request = new ScriptEngineHBRequest(clientId, lastRequestTime);
			return request;
		} else {
			log.warn(String.format("Plugin id: %s doesn't match: %s", id, ScriptEngineHBRequest.PLUGIN));
		}
		return null;
	}

	@Override
	public void processResponse(String id, String data) {
		if (ScriptEngineHBRequest.PLUGIN.equals(id)) {

			Serializable responseObject = SerializationUtils.unwrapSerialized(data, classLoader);
			if (responseObject instanceof ScriptEngineHBResponse) {
				try {
					ScriptEngineHBResponse response = (ScriptEngineHBResponse) responseObject;
					dataProvider.reloadScripts(response);
					lastRequestTime = System.currentTimeMillis();
					if (log.isDebugEnabled()) {
						log.debug("Updated jwt secrets for Jwt Filter");
					}
				} catch (PIPPluginException e) {
					log.error("Error encountered in processing the Script engine HB response,", e);
				}
			} else {
				log.warn(String.format(
						"Skip process heartbeat response because recevied data type: %s is not of type: %s",
						responseObject.getClass().getName(), ScriptEngineHBResponse.class.getName()));
			}

		}
	}

	@Override
	public void init() throws Exception {
		log.info("PIP Script engine -client --> initializing...");

		dataProvider = ScriptDataProvider.getInstance(properties);
		dataProvider.init();

		log.info("PIP Script engine -client --> started");
	}

	@Override
	public void setProperties(Properties paramProperties) {
		this.properties = paramProperties;
	}

	@Override
	public IEvalValue getAttribute(IDSubject subject, String paramString) throws ServiceProviderException {
		return getAttribute(subject.getUid(), "SUBJECT", "user", paramString);
	}

	@Override
	public IEvalValue getAttribute(IResource resource, String paramString) throws ServiceProviderException {
		String policyModel = null;
		for (Entry<String, IEvalValue> entry : resource.getEntrySet()) {
			if ("ce::destinytype".equals(entry.getKey()) && STRING.equals(entry.getValue().getType())) {
				policyModel = (String) entry.getValue().getValue();
			}
		}
		if (policyModel == null) {
			log.error("policy model is not available");
			return null;
		}
		return getAttribute(String.valueOf(resource.getIdentifier()), "RESOURCE", policyModel, paramString);
	}

	private IEvalValue getAttribute(String entityId, String entityType, String modelType, String attributeName) {
		if (log.isDebugEnabled()) {
			log.debug("***************************************************************");
			log.debug(String.format("attribute: %s for entity %s of type %s, model type: %s", attributeName, entityId,
					entityType, modelType));
		}
		long startTime = System.nanoTime();
		String attributeVal = dataProvider.getAttribute(entityId, entityType, modelType, attributeName);
		if (log.isDebugEnabled() && attributeVal == null) {
			log.debug("attribute not found");
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("getAttribute took %s ms", ((System.nanoTime() - startTime) / 1000000)));
			log.debug("***************************************************************");
		}
		return EvalValue.build(attributeVal);
	}

}

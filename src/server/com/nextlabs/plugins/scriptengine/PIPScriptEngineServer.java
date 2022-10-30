/*
 * Copyright 2017 by Nextlabs Inc.
 *
 * All rights reserved worldwide.
 * Created on 10 Aug 2017
 * 
 * MLI - amilasilva88@gmail.com
 */
package com.nextlabs.plugins.scriptengine;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.destiny.container.dcc.plugin.IDCCHeartbeatServerPlugin;
import com.bluejungle.destiny.server.shared.registration.IRegisteredDCCComponent;
import com.bluejungle.framework.comp.ComponentInfo;
import com.bluejungle.framework.comp.ComponentManagerFactory;
import com.bluejungle.framework.comp.IComponentManager;
import com.bluejungle.framework.comp.LifestyleType;
import com.bluejungle.framework.configuration.DestinyRepository;
import com.bluejungle.framework.datastore.hibernate.HibernateUtils;
import com.bluejungle.framework.datastore.hibernate.IHibernateRepository;
import com.bluejungle.framework.heartbeat.IServerHeartbeatManager;
import com.bluejungle.framework.heartbeat.ServerHeartbeatManagerImpl;
import com.bluejungle.framework.utils.SerializationUtils;
import com.nextlabs.destiny.pip.scriptengine.dto.ScriptData;
import com.nextlabs.plugins.pip.scriptengine.dto.ScriptEngineHBRequest;
import com.nextlabs.plugins.pip.scriptengine.dto.ScriptEngineHBResponse;
import com.nextlabs.plugins.pip.scriptengine.exceptions.PIPPluginException;

import net.sf.hibernate.Session;

/**
 * PIP Scripting Engine server plug-in main invoking point
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public class PIPScriptEngineServer implements IDCCHeartbeatServerPlugin {

	private static final Log log = LogFactory.getLog(PIPScriptEngineServer.class);

	private ClassLoader classLoader = getClass().getClassLoader();

	@Override
	public void init(IRegisteredDCCComponent component) {
		// Get heartbeat manager
		ComponentInfo<ServerHeartbeatManagerImpl> heartbeatMgrCompInfo = new ComponentInfo<ServerHeartbeatManagerImpl>(
				IServerHeartbeatManager.COMP_NAME, ServerHeartbeatManagerImpl.class, IServerHeartbeatManager.class,
				LifestyleType.SINGLETON_TYPE);

		IServerHeartbeatManager heartbeatMgr = ComponentManagerFactory.getComponentManager()
				.getComponent(heartbeatMgrCompInfo);
		heartbeatMgr.register(ScriptEngineHBRequest.PLUGIN, this);
		log.info("Registed ScriptEngine request with component " + component.getComponentName());

	}

	@Override
	public Serializable serviceHeartbeatRequest(String name, String data) {
		if (ScriptEngineHBRequest.PLUGIN.equals(name)) {

			Serializable requestObject = SerializationUtils.unwrapSerialized(data, classLoader);
			if (requestObject instanceof ScriptEngineHBRequest) {
				ScriptEngineHBRequest request = (ScriptEngineHBRequest) requestObject;
				if (log.isDebugEnabled()) {
					log.debug("Recevied ScriptEngineRequest from client : " + request.getClientId());
				}
				ScriptEngineHBResponse response = null;
				try {
					response = getScriptData(request);
				} catch (Exception e) {
					log.error("Error in retrieving script engine response data:", e);
					// return empty response
					response = new ScriptEngineHBResponse();
				}
				return response;
			} else {
				log.warn(String.format(
						"Skip service heartbeat request because recevied data type: %s is not of type: %s",
						requestObject.getClass().getName(), ScriptEngineHBRequest.class.getName()));
			}

		}
		return null;
	}

	private static final String FIND_SCRIPT_SQL = "SELECT * FROM pip_script_data pip "
			+ "WHERE pip.last_updated >= ? AND (pip.status = ? OR  pip.status = ? ";

	private ScriptEngineHBResponse getScriptData(ScriptEngineHBRequest request) throws Exception {
		IComponentManager componentManager = ComponentManagerFactory.getComponentManager();
		IHibernateRepository dataSource = (IHibernateRepository) componentManager
				.getComponent(DestinyRepository.MANAGEMENT_REPOSITORY.getName());

		if (dataSource == null) {
			throw new IllegalStateException("Required datasource not initialized for getting data from database");
		}

		ScriptEngineHBResponse response = new ScriptEngineHBResponse();

		Session session = null;
		PreparedStatement stmt = null;
		try {
			session = dataSource.getCountedSession();
			Connection conn = session.connection();
			stmt = conn.prepareStatement(FIND_SCRIPT_SQL);
			stmt.setLong(0, request.getLastRequestTime());
			stmt.setString(1, "ACTIVE");
			stmt.setString(2, "DELETED");

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("id");
				String scriptName = rs.getString("script_name");
				String description = rs.getString("description");
				String content = rs.getString("content");
				String status = rs.getString("status");

				scriptName = id + "_" + scriptName;

				if ("ACTIVE".equalsIgnoreCase(status)) {
					ScriptData script = new ScriptData();
					script.setName(scriptName);
					script.setFilename(scriptName);
					script.setDescription(description);
					script.setScriptText(content);
					response.getScriptDataList().add(script);
				} else {
					response.getDeletedScriptNames().add(scriptName);
				}

			}
			rs.close();
			conn.close();
		} catch (Exception e) {
			throw new PIPPluginException(e);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
			HibernateUtils.closeSession(session, log);
		}
		return response;
	}

}

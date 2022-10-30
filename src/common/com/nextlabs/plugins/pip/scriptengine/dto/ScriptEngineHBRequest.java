/*
 * Copyright 2017 by Nextlabs Inc.
 *
 * All rights reserved worldwide.
 * Created on 10 Aug 2017
 * 
 * MLI - amilasilva88@gmail.com
 */
package com.nextlabs.plugins.pip.scriptengine.dto;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Script engine heart-beat request to control center to fetch scripts modifications or new
 * scripts
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public class ScriptEngineHBRequest implements Externalizable {

	public static final String PLUGIN = "PIPScriptEnginePlugin";

	private String clientId;
	private Long lastRequestTime;

	public ScriptEngineHBRequest() {

	}

	public ScriptEngineHBRequest(String clientId, Long lastRequestTime) {
		this.clientId = clientId;
		this.lastRequestTime = lastRequestTime;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(clientId);
		out.writeLong(lastRequestTime);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.clientId = in.readUTF();
		this.lastRequestTime = in.readLong();
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Long getLastRequestTime() {
		return lastRequestTime;
	}

	public void setLastRequestTime(Long lastRequestTime) {
		this.lastRequestTime = lastRequestTime;
	}

}

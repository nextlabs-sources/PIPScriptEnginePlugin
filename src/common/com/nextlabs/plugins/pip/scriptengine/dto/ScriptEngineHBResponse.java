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
import java.util.ArrayList;
import java.util.List;

import com.nextlabs.destiny.pip.scriptengine.dto.ScriptData;

/**
 * Script engine heart-beat response to send details on updated or new scripts
 * with meta data
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public class ScriptEngineHBResponse implements Externalizable {

	private int scriptSize;
	private int deletedScriptSize;
	private List<ScriptData> scriptDataList = new ArrayList<>();
	private List<String> deletedScriptNames = new ArrayList<>();

	public int getScriptSize() {
		return scriptSize;
	}

	public void setScriptSize(int scriptSize) {
		this.scriptSize = scriptSize;
	}

	public int getDeletedScriptSize() {
		return deletedScriptSize;
	}

	public void setDeletedScriptSize(int deletedScriptSize) {
		this.deletedScriptSize = deletedScriptSize;
	}

	public List<ScriptData> getScriptDataList() {
		return scriptDataList;
	}

	public void setScriptDataList(List<ScriptData> scriptDataList) {
		this.scriptDataList = scriptDataList;
	}

	public List<String> getDeletedScriptNames() {
		return deletedScriptNames;
	}

	public void setDeletedScriptNames(List<String> deletedScriptNames) {
		this.deletedScriptNames = deletedScriptNames;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		this.scriptSize = scriptDataList.size();
		this.deletedScriptSize = deletedScriptNames.size();

		out.writeInt(scriptSize);
		out.writeInt(deletedScriptSize);

		for (ScriptData scriptData : scriptDataList) {
			out.writeObject(scriptData);
		}

		for (String scriptName : deletedScriptNames) {
			out.writeUTF(scriptName);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		scriptSize = in.readInt();
		deletedScriptSize = in.readInt();

		for (int i = 0; i < scriptSize; i++) {
			scriptDataList.add((ScriptData) in.readObject());
		}

		for (int i = 0; i < deletedScriptSize; i++) {
			deletedScriptNames.add(in.readUTF());
		}
	}

}

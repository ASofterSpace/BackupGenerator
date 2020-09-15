/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.toolbox.utils.Record;

import java.util.List;


public class Target {

	// name is e.g. "hdd_13_1", which can then be searched for on each drive to identify such a drive
	private String name;

	private List<String> actions;


	public Target(Record rec) {

		this.name = rec.getString("name");

		this.actions = rec.getArrayAsStringList("actions");
	}

	public Record toRecord() {

		Record result = Record.emptyObject();

		result.set("name", name);

		result.set("actions", actions);

		return result;
	}

}

/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.target;

import com.asofterspace.toolbox.utils.Record;

import java.util.List;


public class TargetDrive {

	// name is e.g. "hdd_13_1", which can then be searched for on each drive to identify such a drive
	protected String name;

	protected List<String> actions;


	public TargetDrive(Record rec) {

		this.name = rec.getString("name");

		this.actions = rec.getArrayAsStringList("actions");
	}

	public TargetDrive(TargetDrive other) {

		this.name = other.name;

		this.actions = other.actions;
	}

	public Record toRecord() {

		Record result = Record.emptyObject();

		result.set("name", name);

		result.set("actions", actions);

		return result;
	}

	public String getName() {
		return name;
	}

}

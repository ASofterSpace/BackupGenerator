/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.target;

import com.asofterspace.backupGenerator.actions.Action;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.Record;

import java.util.ArrayList;
import java.util.List;


public class TargetDrive {

	// name is e.g. "hdd_13_1", which can then be searched for on each drive to identify such a drive
	protected String name;

	protected List<Action> actions;

	protected TextFile logfile;


	public TargetDrive(Record rec) {

		this.name = rec.getString("name");

		List<Record> actionRecs = rec.getArray("actions");
		this.actions = new ArrayList<>();
		for (Record actionRec : actionRecs) {
			this.actions.add(new Action(actionRec));
		}

		String logfileStr = rec.getString("logfile");
		if (logfileStr != null) {
			this.logfile = new TextFile(logfileStr);
		}
	}

	public TargetDrive(TargetDrive other) {

		this.name = other.name;

		this.actions = other.actions;

		this.logfile = other.logfile;
	}

	public Record toRecord() {

		Record result = Record.emptyObject();

		result.set("name", name);

		List<Record> actionRecs = new ArrayList<>();
		for (Action action : actions) {
			actionRecs.add(action.toRecord());
		}
		result.set("actions", actionRecs);

		if (logfile == null) {
			result.set("logfile", null);
		} else {
			result.set("logfile", logfile.getAbsoluteFilename());
		}

		return result;
	}

	public String getName() {
		return name;
	}

	public List<Action> getActions() {
		return actions;
	}

}

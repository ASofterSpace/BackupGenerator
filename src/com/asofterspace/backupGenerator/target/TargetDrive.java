/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.target;

import com.asofterspace.backupGenerator.actions.Action;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.ArrayList;
import java.util.List;


public class TargetDrive {

	private static final String USE_AS_TEMPLATE_FOR = "useAsTemplateFor";
	private static final String REPLACENAME = "%NAME%";
	private static final String ACTIONS = "actions";
	private static final String LOGFILE = "logfile";
	private static final String NAME = "name";

	// name is e.g. "hdd_13_1", which can then be searched for on each drive to identify such a drive
	protected String name;

	protected List<String> useAsTemplateFor;

	protected List<Action> actions;

	protected TextFile logfile;


	public TargetDrive(Record rec) {

		this.name = rec.getString(NAME);

		this.useAsTemplateFor = rec.getArrayAsStringList(USE_AS_TEMPLATE_FOR);

		List<Record> actionRecs = rec.getArray(ACTIONS);
		this.actions = new ArrayList<>();
		for (Record actionRec : actionRecs) {
			this.actions.add(new Action(actionRec));
		}

		String logfileStr = rec.getString(LOGFILE);
		if (logfileStr != null) {
			this.logfile = new TextFile(logfileStr);
		}
	}

	public TargetDrive(TargetDrive other) {

		this.name = other.name;

		this.useAsTemplateFor = other.useAsTemplateFor;

		this.actions = other.actions;

		this.logfile = other.logfile;
	}

	public static List<TargetDrive> createFromTemplate(Record rec) {
		List<TargetDrive> result = new ArrayList<>();
		List<String> useAsTemplateFor = rec.getArrayAsStringList(USE_AS_TEMPLATE_FOR);
		if ((useAsTemplateFor == null) || (useAsTemplateFor.size() < 1)) {
			result.add(new TargetDrive(rec));
		} else {
			for (String templateName : useAsTemplateFor) {
				Record curRec = rec.createDeepCopy();
				curRec.setString(NAME, StrUtils.replaceAll(curRec.getString(NAME), REPLACENAME, templateName));
				curRec.setString(LOGFILE, StrUtils.replaceAll(curRec.getString(LOGFILE), REPLACENAME, templateName));
				curRec.remove(USE_AS_TEMPLATE_FOR);
				result.add(new TargetDrive(curRec));
			}
		}
		return result;
	}

	public Record toRecord() {

		Record result = Record.emptyObject();

		result.set(NAME, name);

		List<Record> actionRecs = new ArrayList<>();
		for (Action action : actions) {
			actionRecs.add(action.toRecord());
		}
		result.set(ACTIONS, actionRecs);

		if (logfile == null) {
			result.set(LOGFILE, null);
		} else {
			result.set(LOGFILE, logfile.getAbsoluteFilename());
		}

		if ((useAsTemplateFor != null) && (useAsTemplateFor.size() > 0)) {
			result.set(USE_AS_TEMPLATE_FOR, useAsTemplateFor);
		}

		return result;
	}

	public String getName() {
		return name;
	}

	public List<Action> getActions() {
		return actions;
	}

	public TextFile getLogfile() {
		return logfile;
	}

}

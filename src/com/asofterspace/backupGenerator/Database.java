/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.backupGenerator.target.TargetDrive;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.utils.Record;

import java.util.ArrayList;
import java.util.List;


public class Database {

	private JsonFile dbFile;

	private JSON root;

	private List<TargetDrive> targets;


	public Database() {

		this.dbFile = new JsonFile("config/database.json");
		this.dbFile.createParentDirectory();
		try {
			this.root = dbFile.getAllContents();
		} catch (JsonParseException e) {
			System.err.println("Oh no!");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		List<Record> targetRecs = root.getArray("targets");

		this.targets = new ArrayList<>();

		for (Record rec : targetRecs) {
			targets.add(new TargetDrive(rec));
		}
	}

	public Record getRoot() {
		return root;
	}

	public void save() {

		root.makeObject();

		List<Record> targetRecs = new ArrayList<>();

		for (TargetDrive obj : targets) {
			targetRecs.add(obj.toRecord());
		}

		root.set("targets", targetRecs);

		dbFile.setAllContents(root);
		dbFile.save();
	}

	public List<TargetDrive> getTargets() {
		return targets;
	}

}

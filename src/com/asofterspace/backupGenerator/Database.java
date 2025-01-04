/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.backupGenerator.target.TargetDrive;
import com.asofterspace.toolbox.io.Directory;
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

	private List<String> mountpoints;


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

		mountpoints = root.getArrayAsStringList("mountpoints");
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

	public List<Directory> getDriveMountPoints() {
		List<Directory> result = new ArrayList<>();
		if (mountpoints != null) {
			for (String mountpoint : mountpoints) {
				result.add(new Directory(mountpoint));
			}
		}
		if (result.size() < 1) {
			for (char driveLetter = 'A'; driveLetter < 'Z'; driveLetter++) {
				result.add(new Directory("" + driveLetter + ":\\"));
			}
		}
		return result;
	}
}

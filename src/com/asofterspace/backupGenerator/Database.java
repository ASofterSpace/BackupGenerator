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

	private static final String TARGETS = "targets";
	private static final String MOUNTPOINTS = "mountpoints";
	private static final String MOUNTPOINT_ANY = "%ANY%";
	private static final String INTEGRITY_CHECK_DIRS = "integrityCheckDirs";

	private JsonFile dbFile;

	private JSON root;

	private List<TargetDrive> targetsFromFile;

	private List<TargetDrive> templatedTargets;

	private List<String> mountpoints;

	private List<String> integrityCheckDirs;


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

		List<Record> targetRecs = root.getArray(TARGETS);

		this.targetsFromFile = new ArrayList<>();
		this.templatedTargets = new ArrayList<>();

		for (Record rec : targetRecs) {
			this.targetsFromFile.add(new TargetDrive(rec));
			this.templatedTargets.addAll(TargetDrive.createFromTemplate(rec));
		}

		mountpoints = root.getArrayAsStringList(MOUNTPOINTS);

		integrityCheckDirs = root.getArrayAsStringList(INTEGRITY_CHECK_DIRS);
	}

	public Record getRoot() {
		return root;
	}

	public void save() {

		root.makeObject();

		List<Record> targetRecs = new ArrayList<>();
		for (TargetDrive obj : targetsFromFile) {
			targetRecs.add(obj.toRecord());
		}
		root.set(TARGETS, targetRecs);

		root.set(MOUNTPOINTS, mountpoints);

		root.set(INTEGRITY_CHECK_DIRS, integrityCheckDirs);

		dbFile.setAllContents(root);
		dbFile.save();
	}

	public List<TargetDrive> getTemplatedTargets() {

		return templatedTargets;
	}

	public List<Directory> getDriveMountPoints() {
		List<Directory> result = new ArrayList<>();
		if (mountpoints != null) {
			for (String mountpoint : mountpoints) {
				if (mountpoint.endsWith(MOUNTPOINT_ANY) || mountpoint.endsWith(MOUNTPOINT_ANY + "/") || mountpoint.endsWith(MOUNTPOINT_ANY + "\\")) {
					// always do +1 as that will copy to ending with a trailing (back)slash if it ends with one or to ending without one
					// but both is fine
					mountpoint = mountpoint.substring(0, mountpoint.length() - (MOUNTPOINT_ANY.length() + 1));
					Directory anyMountParentDir = new Directory(mountpoint);
					boolean recursively = false;
					List<Directory> subMountpoints = anyMountParentDir.getAllDirectories(recursively);
					for (Directory subMountpoint : subMountpoints) {
						result.add(subMountpoint);
					}
				} else {
					result.add(new Directory(mountpoint));
				}
			}
		}
		if (result.size() < 1) {
			for (char driveLetter = 'A'; driveLetter < 'Z'; driveLetter++) {
				result.add(new Directory("" + driveLetter + ":\\"));
			}
		}
		return result;
	}

	public List<String> getIntegrityCheckDirs() {
		return integrityCheckDirs;
	}

}

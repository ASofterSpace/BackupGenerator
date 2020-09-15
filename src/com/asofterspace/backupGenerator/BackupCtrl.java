/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.backupGenerator.target.IdentifiedTarget;
import com.asofterspace.backupGenerator.target.TargetDrive;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;

import java.util.ArrayList;
import java.util.List;


public class BackupCtrl {

	private Database database;


	public BackupCtrl(Database database) {
		this.database = database;
	}

	public void start() {

		List<IdentifiedTarget> targets = identifyTargets();

		for (IdentifiedTarget target : targets) {
			System.out.println("Backing up to " + target + "...");
		}
	}

	private List<IdentifiedTarget> identifyTargets() {

		List<TargetDrive> possibleTargets = database.getTargets();

		List<IdentifiedTarget> result = new ArrayList<>();

		for (TargetDrive possibleTarget : possibleTargets) {
			for (char driveLetter = 'A'; driveLetter < 'Z'; driveLetter++) {
				Directory targetDir = new Directory("" + driveLetter + ":\\");
				File targetIdFile = new File(targetDir, possibleTarget.getName() + ".txt");
				if (targetIdFile.exists()) {
					result.add(new IdentifiedTarget(possibleTarget, targetDir));
				}
			}
		}

		return result;
	}

}

/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.backupGenerator.actions.Action;
import com.asofterspace.backupGenerator.target.IdentifiedTarget;
import com.asofterspace.backupGenerator.target.TargetDrive;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;

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
			List<Action> actions = target.getActions();

			StringBuilder logLines = new StringBuilder();
			logLines.append(target.getName() + "\n\n");
			logLines.append("Last backup on: " + DateUtils.serializeDateTime(DateUtils.now()) + "\n");
			logLines.append("Then mounted on: " + target.getTargetDir().getAbsoluteDirname() + "\n");
			logLines.append("\nBacked up folders:");

			for (Action action : actions) {
				Directory destination = new Directory(action.getDestinationName());
				List<String> sourcePaths = action.getSourcePaths();
				int replicationFactor = action.getReplicationFactor();
				for (String sourcePath : sourcePaths) {
					logLines.append("\n");
					Directory source = new Directory(sourcePath);
					String actionLog = startAction(action, destination, source, replicationFactor);
					logLines.append(actionLog);
				}
			}

			TextFile log = target.getLogfile();
			if (log != null) {
				log.saveContent(logLines);
			}
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

	private String startAction(Action action, Directory destination, Directory source, int replicationFactor) {

		System.out.println("  Starting " + action.getKind() + " from " + source.getAbsoluteDirname() + " to " +
			destination.getAbsoluteDirname() + " with replication factor " + replicationFactor + "...");

		// TODO

		Directory datedDestinationToday = new Directory(destination.getAbsoluteDirname() +
			" (" + StrUtils.replaceAll(DateUtils.serializeDate(DateUtils.now()), "-", " ") + ")");

		return datedDestinationToday.getLocalDirname();
	}


}

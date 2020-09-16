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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
				Directory destinationParent = target.getTargetDir();
				List<String> sourcePaths = action.getSourcePaths();
				int replicationFactor = action.getReplicationFactor();
				String actionLog = startAction(action, sourcePaths, destinationParent, replicationFactor);
				logLines.append("\n");
				logLines.append(actionLog);
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

	private String startAction(Action action, List<String> sourcePaths, Directory destinationParent, int replicationFactor) {

		Directory source = new Directory(sourcePaths.get(0));
		List<Directory> sources = new ArrayList<>();
		String fromStr = "";
		String sep = "";
		for (String sourcePath : sourcePaths) {
			sources.add(new Directory(sourcePath));
			fromStr += sep + sourcePath;
			sep = " and ";
		}

		Directory datedDestinationToday = new Directory(destinationParent, source.getLocalDirname() +
			" (" + StrUtils.replaceAll(DateUtils.serializeDate(DateUtils.now()), "-", " ") + ")");

		System.out.println("  Starting " + action.getKind() + " from " + fromStr + " to " +
			datedDestinationToday.getAbsoluteDirname() + " with replication factor " + replicationFactor + "...");

		boolean recursively = false;
		List<Directory> destDirs = destinationParent.getAllDirectories(recursively);
		List<Directory> existingDestDirs = new ArrayList<>();
		for (Directory destDir : destDirs) {
			if (destDir.getLocalDirname().startsWith(action.getDestinationName() + " (")) {
				existingDestDirs.add(destDir);
			}
		}

		Collections.sort(existingDestDirs, new Comparator<Directory>() {
			public int compare(Directory a, Directory b) {
				String dateAStr = a.getDirname().substring(a.getDirname().lastIndexOf(" (") + 2);
				dateAStr = dateAStr.substring(0, dateAStr.length() - 1);
				Date dateA = DateUtils.parseDate(dateAStr);

				String dateBStr = b.getDirname().substring(b.getDirname().lastIndexOf(" (") + 2);
				dateBStr = dateBStr.substring(0, dateBStr.length() - 1);
				Date dateB = DateUtils.parseDate(dateBStr);

				return dateA.compareTo(dateB);
			}
		});

		System.out.println("  Found " + existingDestDirs.size() + " existing backups, expecting " +
			replicationFactor + "...");

		// removing oldest backups if there are too many
		while (existingDestDirs.size() > replicationFactor) {
			System.out.println("  Removing " + existingDestDirs.get(0) + "...");
			existingDestDirs.get(0).delete();
			existingDestDirs.remove(0);
		}

		Directory actualDestination = null;

		if (existingDestDirs.size() == replicationFactor) {
			// using oldest backup if there are exactly as many as wanted
			actualDestination = existingDestDirs.get(0);

			// however, if today's backup destination dir already exists...
			if (datedDestinationToday.exists()) {
				// ... then use that one instead, as we don't want to copy the older one into the newer one afterwards
				actualDestination = datedDestinationToday;
			}
		} else {
			// creating new backup set to today if there are fewer than wanted
			actualDestination = datedDestinationToday;
		}

		switch (action.getKind()) {
			// allowed action kinds
			case "sync":
			case "writeonly":
				break;
			default:
				System.out.println("  UNKNOWN ACTION KIND " + action.getKind() + "!");
				return destinationParent.getAbsoluteDirname() + "/" + source.getLocalDirname() +
					" (" + action.getKind() + " unknown!)";
		}

		performAction(action.getKind(), sources, actualDestination, "");

		// once all is done, rename the folder we are backuping into to the current date
		actualDestination.rename(datedDestinationToday.getLocalDirname());

		return datedDestinationToday.getLocalDirname();
	}

	private void performAction(String kind, List<Directory> sources, Directory destination, String curRelPath) {

		boolean recursively = false;

		List<Directory> curSources = new ArrayList<>();
		for (Directory source : sources) {
			curSources.add(new Directory(source, curRelPath));
		}
		Directory curDestination = new Directory(destination, curRelPath);

		curDestination.create();

		List<File> childFiles = new ArrayList<>();
		for (Directory curSource : curSources) {
			childFiles.addAll(curSource.getAllFiles(recursively));
		}
		for (File sourceFile : childFiles) {
			File destFile = new File(curDestination, sourceFile.getLocalFilename());
			// the backed up file already exists...
			if (destFile.exists()) {
				Date sourceChangeDate = sourceFile.getChangeDate();
				Date destChangeDate = destFile.getChangeDate();
				// ... and the last update times are the same...
				if ((sourceChangeDate != null) && (destChangeDate != null) &&
					DateUtils.isSameDay(sourceChangeDate, destChangeDate)) {
					Long sourceSize = sourceFile.getSize();
					Long destSize = destFile.getSize();
					// ... and the reported sizes are the same...
					if ((sourceSize != null) && (destSize != null) && ((long) sourceSize == (long) destSize)) {
						// ... skip backing up this file!
						continue;
					}
				}
			}
			// actually backup this file
			sourceFile.copyToDisk(destFile);
		}

		if ("sync".equals(kind)) {
			// in case of sync, delete files in the destination which are not in the source
			List<File> destChildren = curDestination.getAllFiles(recursively);
			for (File destChild : destChildren) {
				boolean deletedInSource = true;
				for (File sourceFile : childFiles) {
					if (sourceFile.getLocalFilename().equals(destChild.getLocalFilename())) {
						deletedInSource = false;
						break;
					}
				}
				if (deletedInSource) {
					destChild.delete();
				}
			}
		}

		List<Directory> childDirs = new ArrayList<>();
		for (Directory curSource : curSources) {
			childDirs.addAll(curSource.getAllDirectories(recursively));
		}
		for (Directory childDir : childDirs) {
			performAction(kind, sources, destination, curRelPath + childDir.getLocalDirname() + "/");
		}

		if ("sync".equals(kind)) {
			// in case of sync, delete child directories in the destination which are not in the source
			List<Directory> destChildren = curDestination.getAllDirectories(recursively);
			for (Directory destChild : destChildren) {
				boolean deletedInSource = true;
				for (Directory sourceDir : childDirs) {
					if (sourceDir.getLocalDirname().equals(destChild.getLocalDirname())) {
						deletedInSource = false;
						break;
					}
				}
				if (deletedInSource) {
					destChild.delete();
				}
			}
		}
	}


}

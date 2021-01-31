/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.backupGenerator.actions.Action;
import com.asofterspace.backupGenerator.output.OutputUtils;
import com.asofterspace.backupGenerator.target.IdentifiedTarget;
import com.asofterspace.backupGenerator.target.TargetDrive;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class BackupCtrl {

	private Database database;

	private int checkCounter = 0;
	private int copyCounter = 0;

	private volatile boolean paused = false;
	private volatile boolean cancelled = false;


	public BackupCtrl(Database database) {
		this.database = database;
	}

	public void start() {

		cancelled = false;
		paused = false;

		List<IdentifiedTarget> targets = identifyTargets();

		for (IdentifiedTarget target : targets) {

			if (cancelled) {
				return;
			}

			OutputUtils.setTarget(target.getName() + " (mounted as " + target.getTargetDir().getDirname() + ")");

			OutputUtils.println("Backing up to " + target + "...");
			List<Action> actions = target.getActions();

			Date backupStartTime = DateUtils.now();

			StringBuilder logLines = new StringBuilder();
			TextFile log = target.getLogfile();
			if (log != null) {
				logLines.append(target.getName() + "\n\n");
				logLines.append("Last backup on: " + DateUtils.serializeDateTime(backupStartTime) + " (still ongoing)\n");
				logLines.append("Then mounted on: " + target.getTargetDir().getAbsoluteDirname() + "\n");

				log.saveContent(logLines);

				logLines.append("\nBacked up folders:");
			}

			List<String> actionLogs = new ArrayList<>();

			for (Action action : actions) {
				Directory destinationParent = target.getTargetDir();
				List<String> sourcePaths = action.getSourcePaths();
				int replicationFactor = action.getReplicationFactor();
				String actionLog = startAction(action, sourcePaths, destinationParent, replicationFactor, targets);

				if (cancelled) {
					return;
				}

				if (log != null) {
					logLines.append("\n");
					logLines.append(actionLog);
					log.saveContent(logLines);
					actionLogs.add(actionLog);
				}
			}

			if (log != null) {
				logLines = new StringBuilder();
				logLines.append(target.getName() + "\n\n");
				logLines.append("Last backup on: " + DateUtils.serializeDateTime(backupStartTime) + " - " +
					DateUtils.serializeDateTime(DateUtils.now()) + "\n");
				logLines.append("Then mounted on: " + target.getTargetDir().getAbsoluteDirname() + "\n");
				logLines.append("\nBacked up folders:");
				for (String actionLogLine : actionLogs) {
					logLines.append("\n");
					logLines.append(actionLogLine);
				}
				log.saveContent(logLines);
			}
		}

		OutputUtils.message("Backup run done! :)");
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

	private String startAction(Action action, List<String> sourcePaths, Directory destinationParent, int replicationFactor,
		List<IdentifiedTarget> targets) {

		if (cancelled) {
			return "cancelled";
		}

		List<Directory> sources = new ArrayList<>();
		String fromStr = "";
		String sep = "";
		for (String sourcePath : sourcePaths) {

			// we are listing the sources for this sync or write action, and for each source, we check
			// if the name is like [hdd_12_1]\\blubb (actual), so starting with [target], in which case
			// we try to resolve that target to an actual directory path
			if (sourcePath.startsWith("[")) {
				sourcePath = sourcePath.substring(1);
				if (!sourcePath.contains("]")) {
					OutputUtils.println("  SOURCE '" + sourcePath + "' MISSING THE ']'!");
					return destinationParent.getAbsoluteDirname() + " (source '" + sourcePath + "' missing the ']'!)";
				}
				String sourceName = sourcePath.substring(0, sourcePath.indexOf("]"));
				sourcePath = sourcePath.substring(sourcePath.indexOf("]") + 1);
				boolean foundTargetAsSource = false;
				for (IdentifiedTarget target : targets) {
					if (sourceName.equals(target.getName())) {
						sourcePath = target.getTargetDir().getAbsoluteDirname() + sourcePath;
						foundTargetAsSource = true;
						break;
					}
				}
				if (!foundTargetAsSource) {
					OutputUtils.println("  SOURCE NOT FOUND: [" + sourceName + "]!");
					return destinationParent.getAbsoluteDirname() + " (source [" + sourceName + "] not found!)";
				}
			}

			// actually add the source to the list of sources that we will use
			sources.add(new Directory(sourcePath));
			fromStr += sep + sourcePath;
			sep = " and ";
		}
		Directory source = sources.get(0);


		switch (action.getKind()) {
			// allowed action kinds
			case "sync":
			case "writeonly":
				break;
			default:
				OutputUtils.println("  UNKNOWN ACTION KIND " + action.getKind() + "!");
				return destinationParent.getAbsoluteDirname() + "/" + source.getLocalDirname() +
					" (" + action.getKind() + " unknown!)";
		}


		Directory destination = null;
		Directory renameDestination = null;

		Directory destinationWithActualQualifier = new Directory(destinationParent, action.getDestinationName() +
			" (actual)");

		if (destinationWithActualQualifier.exists()) {

			destination = destinationWithActualQualifier;

			OutputUtils.println("  Starting " + action.getKind() + " from " + fromStr + " to " +
				destination.getAbsoluteDirname() + " (ignoring replication factor for actual destionations)...");

		} else {

			Directory datedDestinationToday = new Directory(destinationParent, action.getDestinationName() +
				" (" + StrUtils.replaceAll(DateUtils.serializeDate(DateUtils.now()), "-", " ") + ")");

			OutputUtils.println("  Starting " + action.getKind() + " from " + fromStr + " to " +
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

			OutputUtils.println("  Found " + existingDestDirs.size() + " existing backups, expecting " +
				replicationFactor + "...");

			// removing oldest backups if there are too many
			while (existingDestDirs.size() > replicationFactor) {
				OutputUtils.println("  Removing " + existingDestDirs.get(0).getAbsoluteDirname() + "...");
				existingDestDirs.get(0).delete();
				existingDestDirs.remove(0);
			}

			if (existingDestDirs.size() == replicationFactor) {

				if (!datedDestinationToday.exists()) {

					// using oldest backup if there are exactly as many as wanted
					destination = existingDestDirs.get(0);

					renameDestination = datedDestinationToday;

				} else {

					// however, if today's backup destination dir already exists, then use that one instead,
					// as we don't want to copy the older one into the newer one afterwards
					// (which fully guards against any problems with re-running backupper several times a day!)
					destination = datedDestinationToday;
				}
			} else {
				// creating new backup set to today if there are fewer than wanted
				destination = datedDestinationToday;
			}
		}

		performAction(action.getKind(), sources, destination, "");

		if (cancelled) {
			return "cancelled";
		}

		// once all is done, rename the folder we are backuping into to the current date
		if (renameDestination != null) {
			destination.rename(renameDestination.getLocalDirname());
		}

		return destination.getLocalDirname();
	}

	private void performAction(String kind, List<Directory> sources, Directory destination, String curRelPath) {

		OutputUtils.printDir("Checking " + curRelPath + "...");

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
			while (paused) {
				Utils.sleep(1000);
			}

			if (cancelled) {
				return;
			}

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
						checkCounter++;
						if (checkCounter > 2048) {
							checkCounter = 0;
							OutputUtils.println("    checking " + sourceFile.getAbsoluteFilename() +
								"... identical to " + destFile.getAbsoluteFilename());
						}
						continue;
					}
				}
			}

			// actually backup this file
			java.io.File destinationFile = destFile.getJavaFile();

			// create parent directories
			if (destinationFile.getParentFile() != null) {
				destinationFile.getParentFile().mkdirs();
			}

			try {
				Files.copy(sourceFile.getJavaPath(), destFile.getJavaPath(), StandardCopyOption.REPLACE_EXISTING);

			} catch (IOException e) {
				OutputUtils.printerrln("The file " + sourceFile.getAbsoluteFilename() + " could not be copied to " +
					destFile.getAbsoluteFilename() + "!");
			}

			copyCounter++;
			if (copyCounter > 1024) {
				copyCounter = 0;
				OutputUtils.println("    copying " + sourceFile.getAbsoluteFilename() + " to " +
					destFile.getAbsoluteFilename());
			}
		}

		if ("sync".equals(kind)) {
			// in case of sync, delete files in the destination which are not in the source
			List<File> destChildren = curDestination.getAllFiles(recursively);
			for (File destChild : destChildren) {

				while (paused) {
					Utils.sleep(1000);
				}

				if (cancelled) {
					return;
				}

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

				while (paused) {
					Utils.sleep(1000);
				}

				if (cancelled) {
					return;
				}

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

	public void cancel() {
		cancelled = true;
		paused = false;
	}

	public void pause() {
		paused = true;
	}

	public void resume() {
		paused = false;
	}


}

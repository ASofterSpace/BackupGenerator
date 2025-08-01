/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.backupGenerator.actions.Action;
import com.asofterspace.backupGenerator.output.OutputUtils;
import com.asofterspace.backupGenerator.target.IdentifiedTarget;
import com.asofterspace.backupGenerator.target.TargetDrive;
import com.asofterspace.toolbox.io.BinaryFile;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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

	private boolean reportAllActions = false;
	private boolean reportChangingActions = true;


	public BackupCtrl(Database database) {
		this.database = database;
	}

	public void start() {

		String finalRunStr = "cancelled";
		cancelled = false;
		paused = false;

		List<IdentifiedTarget> targets = identifyTargets();

		for (IdentifiedTarget target : targets) {

			if (cancelled) {
				finalizeRun(finalRunStr);
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
				String actionLog = "error";
				try {
					actionLog = startAction(action, sourcePaths, destinationParent, replicationFactor, targets);
					// System.out.println("DEBUG after startAction");
				} catch (Throwable t) {
					StringWriter strWr = new StringWriter();
					PrintWriter prWr = new PrintWriter(strWr);
					t.printStackTrace(prWr);
					actionLog = strWr.toString() + "\nCancelling the rest of the backup run...";
					cancelled = true;
					finalRunStr += " due to exception";
				}

				if (log != null) {
					logLines.append("\n");
					logLines.append(actionLog);
					log.saveContent(logLines);
					actionLogs.add(actionLog);
				}

				// System.out.println("DEBUG couldfinalizeRun " + cancelled);

				if (cancelled) {
					finalizeRun(finalRunStr);
					return;
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

		finalizeRun("done successfully");
	}

	private void finalizeRun(String finalRunStr) {

		// System.out.println("DEBUG callingFinalizeRun()");

		String logFileName = StrUtils.replaceAll(DateUtils.serializeDate(DateUtils.now()), "-", " ") + ".log";
		TextFile logFile = new TextFile(logFileName);
		logFile.setContent(OutputUtils.getErrorLogContent());
		logFile.save();

		if (BackupGenerator.BACKUP_RUN_FILE != null) {
			BackupGenerator.BACKUP_RUN_FILE.delete();
		}

		String smiley = "._.";
		if (finalRunStr.startsWith("done")) {
			smiley = ":)";
		}

		OutputUtils.message("Backup run " + finalRunStr + "! " + smiley);
	}

	private List<IdentifiedTarget> identifyTargets() {

		List<TargetDrive> possibleTargets = database.getTargets();
		List<Directory> driveMountPoints = database.getDriveMountPoints();

		System.out.println("");
		System.out.println("Looking for all of these targets:");
		for (TargetDrive possibleTarget : possibleTargets) {
			System.out.println(possibleTarget.getName() + ".txt");
		}
		System.out.println("");
		System.out.println("In all of these drive mount points:");
		for (Directory driveMountPoint : driveMountPoints) {
			System.out.println(driveMountPoint.getAbsoluteDirname());
		}
		System.out.println("");

		List<IdentifiedTarget> result = new ArrayList<>();

		for (TargetDrive possibleTarget : possibleTargets) {
			for (Directory driveMountPoint : driveMountPoints) {
				File targetIdFile = new File(driveMountPoint, possibleTarget.getName() + ".txt");
				if (targetIdFile.exists()) {
					result.add(new IdentifiedTarget(possibleTarget, driveMountPoint));
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
			case "writeonly-noreport":
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
				destination.getAbsoluteDirname() + " (ignoring replication factor for \"(actual)\" destionations)...");

		} else {

			OutputUtils.println("  Starting " + action.getKind() + " from " + fromStr + " to " +
				destinationParent.getCanonicalDirname() + " > " + action.getDestinationName() +
				" with replication factor " + replicationFactor + "...");

			boolean recursively = false;
			List<Directory> destDirs = destinationParent.getAllDirectories(recursively);
			List<Directory> existingDestDirs = new ArrayList<>();
			for (Directory destDir : destDirs) {
				if (destDir.getLocalDirname().startsWith(action.getDestinationName() + " (")) {
					OutputUtils.println("Considering whether " + destDir.getCanonicalDirname() + " is a destination directory...");
					// ignore non-dated directories (e.g. movies (just in) should be ignored)
					if (getDateFromDirectory(destDir) != null) {
						existingDestDirs.add(destDir);
						OutputUtils.println("Yes, because it has a date attached!");
					} else {
						OutputUtils.println("No, because it has no date attached!");
					}
				}
			}

			Collections.sort(existingDestDirs, new Comparator<Directory>() {
				public int compare(Directory a, Directory b) {
					Date dateA = getDateFromDirectory(a);
					Date dateB = getDateFromDirectory(b);

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

			Directory datedDestinationToday = new Directory(destinationParent, action.getDestinationName() +
				" (" + StrUtils.replaceAll(DateUtils.serializeDate(DateUtils.now()), "-", " ") + ")");

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

		performAction(action.getKind(), sources, destination, "", action.getIndexRemoteFiles(), action.getIgnore(), 0);

		if (cancelled) {
			return "cancelled";
		}

		// once all is done, rename the folder we are backuping into to the current date
		if (renameDestination != null) {
			destination.rename(renameDestination.getLocalDirname());
		}

		return destination.getLocalDirname();
	}

	private long performAction(String kind, List<Directory> sources, Directory destination,
		String curRelPath, boolean indexRemoteFiles, List<String> ignore, long fileCounter) {

		OutputUtils.printDir(kind + "ing " + curRelPath + "...");

		List<Directory> curSources = new ArrayList<>();
		for (Directory source : sources) {
			curSources.add(new Directory(source, curRelPath));
		}
		Directory curDestination = new Directory(destination, curRelPath);

		if (curDestination.getDirname().endsWith(" ")) {
			OutputUtils.printerrln("Encountered a directory whose name ends with a space: '" +
				curDestination.getAbsoluteDirname() + "' - ignoring this directory completely " +
				"to prevent problems...", true);
			return fileCounter;
		}

		if (OutputUtils.getPrintDirectories()) {
			OutputUtils.printDir(kind + "ing " + curDestination.getAbsoluteDirname() + "...");
		}

		curDestination.create();

		boolean recursively = true;

		recursively = false;

		List<File> childFiles = new ArrayList<>();
		for (Directory curSource : curSources) {
			if ((ignore == null) || (ignore.size() < 1)) {
				// System.out.println("DEBUBfiles1");
				childFiles.addAll(curSource.getAllFiles(recursively));
			} else {
				// System.out.println("DEBUGfiles2");
				for (File childFile : curSource.getAllFiles(recursively)) {
					if (!ignore.contains(childFile.getLocalFilename())) {
						childFiles.add(childFile);
					}
				}
			}
		}
		// System.out.println("DEBUG childFiles: [" + StrUtils.join(",", childFiles) + "]");

		for (File sourceFile : childFiles) {
			fileCounter++;
			boolean didPause = false;
			while (paused) {
				didPause = true;
				OutputUtils.println("    [paused]");
				Utils.sleep(1000);
			}
			if (didPause) {
				OutputUtils.println("    [resumed]");
			}

			if (cancelled) {
				OutputUtils.println("    [cancelled]");
				return fileCounter;
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
						if (reportAllActions || (checkCounter > 2048)) {
							checkCounter = 0;
							OutputUtils.println("    checking " + getCounterDisplayStr(fileCounter) + sourceFile.getAbsoluteFilename() +
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

			copyCounter++;
			if (reportAllActions || reportChangingActions || (copyCounter > 1024)) {
				copyCounter = 0;

				OutputUtils.println("    copying " + getCounterDisplayStr(fileCounter) + sourceFile.getAbsoluteFilename() + " to " +
					destFile.getAbsoluteFilename());
			}

			try {
				// attempt to copy the file...
				Files.copy(sourceFile.getJavaPath(), destFile.getJavaPath(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

			} catch (IOException e1) {

				if (reportAllActions) {
					OutputUtils.println("    COPY EXCEPTION 1 :: " + e1);
				}

				// ... and in case of problems...
				try {
					// ... attempt to delete the destination file...
					Files.delete(destFile.getJavaPath());
				} catch (IOException eIgnore) {
					// ... but for now do nothing if this fails...

					if (reportAllActions) {
						OutputUtils.println("    DELETE EXCEPTION :: " + eIgnore);
					}
				}
				try {
					// ... then re-attempt to copy...
					Files.copy(sourceFile.getJavaPath(), destFile.getJavaPath(),
						StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

				} catch (IOException e2) {

					if (reportAllActions) {
						OutputUtils.println("    COPY EXCEPTION 2 :: " + e2);
					}

					boolean allFine = false;

					// ... and if we are told we have no access, check if the content of the file
					// is already perfectly dandy - if so, no need to be unhappy about anything!
					if (e2 instanceof AccessDeniedException) {
						if (reportAllActions) {
							OutputUtils.println("    size comparing " + sourceFile.getAbsoluteFilename() + " to " +
								destFile.getAbsoluteFilename());
						}

						Long sourceSize = sourceFile.getSize();
						Long destSize = destFile.getSize();
						if ((sourceSize != null) && (destSize != null)) {
							if (sourceSize == destSize) {
								allFine = true;

								// for files smaller than 256 kB, try to explicitly check the sizes...
								if (sourceSize < 256 * 1024) {
									if (reportAllActions) {
										OutputUtils.println("    binary comparing " + sourceFile.getAbsoluteFilename() +
											" to " + destFile.getAbsoluteFilename());
									}

									BinaryFile sourceBinary = new BinaryFile(sourceFile);
									byte[] sourceData = sourceBinary.loadContent();
									BinaryFile destBinary = new BinaryFile(destFile);
									byte[] destData = destBinary.loadContent();
									if (!Arrays.equals(sourceData, destData)) {
										allFine = false;
									}

									if (reportAllActions) {
										OutputUtils.println("    END OF binary comparing " + sourceFile.getAbsoluteFilename() +
											" to " + destFile.getAbsoluteFilename());
									}
								}
							}
						}
					}

					if (!allFine) {
						// ... and in case of another failure, actually complain about it!
						OutputUtils.printerrln("The file " + sourceFile.getAbsoluteFilename() + " could not be copied to " +
							destFile.getAbsoluteFilename() + " due to first " + toOneLine(e1) + " and then " + toOneLine(e2) +
							" in the second attempt!", true);
					}
				}
			}
		}

		// we actually sync
		boolean sync = "sync".equals(kind);

		// we don't sync, but we log output to see what would be done if we would sync
		// (and for writeonly-noreport we do not output this at all so the whole block can be skipped)
		boolean outputAsIfSync = "writeonly".equals(kind);

		if (sync || outputAsIfSync) {

			if (reportAllActions) {
				if (sync) {
					OutputUtils.println("    syncing files...");
				} else {
					OutputUtils.println("    output-as-if-syncing files...");
				}
			}

			// in case of sync, delete files in the destination which are not in the source
			List<File> destChildren = curDestination.getAllFiles(recursively);

			List<String> sourceLocalFilenames = new ArrayList<>();

			for (File sourceFile : childFiles) {
				sourceLocalFilenames.add(sourceFile.getLocalFilename());
			}

			for (File destChild : destChildren) {

				boolean didPause = false;
				while (paused) {
					didPause = true;
					OutputUtils.println("    [paused]");
					Utils.sleep(1000);
				}
				if (didPause) {
					OutputUtils.println("    [resumed]");
				}

				if (cancelled) {
					OutputUtils.println("    [cancelled]");
					return fileCounter;
				}

				boolean deletedInSource = true;

				String destLocalFilename = destChild.getLocalFilename();

				for (String sourceLocalFilename : sourceLocalFilenames) {
					if (destLocalFilename.equals(sourceLocalFilename)) {
						deletedInSource = false;
						break;
					}
				}
				if (deletedInSource) {
					if (sync) {
						destChild.delete();
					} else {
						OutputUtils.printerrln("Would delete " + destChild.getAbsoluteFilename() + " if we were not in " + kind + " mode!", false);
					}
				}
			}
		}

		List<Directory> childDirs = new ArrayList<>();
		for (Directory curSource : curSources) {
			if ((ignore == null) || (ignore.size() < 1)) {
				// System.out.println("DEBUGdirs1");
				childDirs.addAll(curSource.getAllDirectories(recursively));
			} else {
				// System.out.println("DEBUGdirs2");
				for (Directory childDir : curSource.getAllDirectories(recursively)) {
					if (!ignore.contains(childDir.getLocalDirname())) {
						childDirs.add(childDir);
					}
				}
			}
		}
		// System.out.println("DEBUG childDirs: [" + StrUtils.join(",", childDirs) + "]");

		for (Directory childDir : childDirs) {
			// set ignore to null as we only want to ignore top-level directories
			fileCounter += performAction(kind, sources, destination, curRelPath + childDir.getLocalDirname() + "/", false, null, fileCounter);

			boolean didPause = false;
			while (paused) {
				didPause = true;
				OutputUtils.println("    [paused]");
				Utils.sleep(1000);
			}
			if (didPause) {
				OutputUtils.println("    [resumed]");
			}

			if (cancelled) {
				OutputUtils.println("    [cancelled]");
				return fileCounter;
			}
		}

		if (sync || outputAsIfSync) {
			// in case of sync, delete child directories in the destination which are not in the source
			List<Directory> destChildren = curDestination.getAllDirectories(recursively);

			if (reportAllActions) {
				if (sync) {
					OutputUtils.println("    syncing directories...");
				} else {
					OutputUtils.println("    output-as-if-syncing directories...");
				}
			}

			List<String> sourceLocalDirnames = new ArrayList<>();

			for (Directory sourceDir : childDirs) {
				sourceLocalDirnames.add(sourceDir.getLocalDirname());
			}

			for (Directory destChild : destChildren) {

				boolean didPause = false;
				while (paused) {
					didPause = true;
					OutputUtils.println("    [paused]");
					Utils.sleep(1000);
				}
				if (didPause) {
					OutputUtils.println("    [resumed]");
				}

				if (cancelled) {
					OutputUtils.println("    [cancelled]");
					return fileCounter;
				}

				boolean deletedInSource = true;
				String destLocalDirname = destChild.getLocalDirname();
				for (String sourceLocalDirname : sourceLocalDirnames) {
					if (destLocalDirname.equals(sourceLocalDirname)) {
						deletedInSource = false;
						break;
					}
				}
				if (deletedInSource) {
					if (sync) {
						destChild.delete();
					} else {
						OutputUtils.printerrln("Would delete " + destChild.getAbsoluteDirname() + " if we were not in " + kind + " mode!", false);
					}
				}
			}
		}

		if (indexRemoteFiles) {
			recursively = false;
			List<Directory> destDirs = curDestination.getAllDirectories(recursively);

			recursively = true;
			String INDEX_FILE_NAME = "remote_index.txt";

			for (Directory destDir : destDirs) {
				List<File> destFiles = destDir.getAllFiles(recursively);
				StringBuilder indexContent = new StringBuilder();
				String dirName = destDir.getCanonicalDirname();
				for (File destFile : destFiles) {
					if (INDEX_FILE_NAME.equals(destFile.getLocalFilename())) {
						continue;
					}
					String fileName = destFile.getCanonicalFilename();
					if (fileName.startsWith(dirName)) {
						fileName = fileName.substring(dirName.length());
					}
					if (fileName.startsWith("\\")) {
						fileName = fileName.substring(1);
					}
					if (fileName.startsWith("/")) {
						fileName = fileName.substring(1);
					}
					fileName = StrUtils.replaceLast(fileName, "\\", " > ");
					fileName = StrUtils.replaceLast(fileName, "/", " > ");
					indexContent.append(fileName);
					indexContent.append("\n");
				}
				String indexStr = indexContent.toString();

				for (Directory source : sources) {
					TextFile sourceIndexFile = new TextFile(new Directory(source, destDir.getLocalDirname()), INDEX_FILE_NAME);
					sourceIndexFile.saveContent(indexStr);
				}
				TextFile destIndexFile = new TextFile(destDir, INDEX_FILE_NAME);
				destIndexFile.saveContent(indexStr);
			}
		}

		return fileCounter;
	}

	private String toOneLine(IOException e) {
		String result = e.toString();
		result = StrUtils.replaceAll(result, "\r", "");
		result = StrUtils.replaceAll(result, "\n", "");
		return result;
	}

	public boolean getReportAllActions() {
		return reportAllActions;
	}

	public void setReportAllActions(boolean reportAllActions) {
		this.reportAllActions = reportAllActions;
	}

	public boolean getReportChangingActions() {
		return reportChangingActions;
	}

	public void setReportChangingActions(boolean reportChangingActions) {
		this.reportChangingActions = reportChangingActions;
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

	private Date getDateFromDirectory(Directory dir) {

		String dirname = dir.getDirname();

		if (dirname.lastIndexOf(" (") < 0) {
			return null;
		}

		String dateStr = dirname.substring(dirname.lastIndexOf(" (") + 2);

		if (!dirname.endsWith(")")) {
			return null;
		}

		dateStr = dateStr.substring(0, dateStr.length() - 1);

		return DateUtils.parseDate(dateStr);
	}

	private static String getCounterDisplayStr(long fileCounter) {
		return fileCounter + ": ";
	}

}

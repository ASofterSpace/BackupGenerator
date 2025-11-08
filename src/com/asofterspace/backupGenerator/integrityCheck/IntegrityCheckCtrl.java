/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.integrityCheck;

import com.asofterspace.backupGenerator.Database;
import com.asofterspace.backupGenerator.output.OutputUtils;
import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;


public class IntegrityCheckCtrl {

	private Database database;
	private ConfigFile config;
	private String[] videoFileCheckCommandAndArgs = {null, null};

	private volatile boolean paused = false;
	private volatile boolean cancelled = false;
	private boolean foundProblems = false;
	private int videoAmount = 0;
	private int audioAmount = 0;

	private final String CANCEL_STR = "Integrity checking cancelled!";


	public IntegrityCheckCtrl(Database database, ConfigFile config) {
		this.database = database;
		this.config = config;

		this.videoFileCheckCommandAndArgs[0] = config.getValue("ffprobePath");
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

	public void start() {

		List<String> directoryStrs = database.getIntegrityCheckDirs();

		if ((directoryStrs != null) && (directoryStrs.size() > 0)) {
			videoAmount = 0;
			audioAmount = 0;

			for (String dirStr : directoryStrs) {
				foundProblems = false;
				Directory curDir = new Directory(dirStr);
				OutputUtils.printerrln("", false);
				OutputUtils.printerrln("Integrity checks starting for directory:", false);
				OutputUtils.printerrln(curDir.getCanonicalDirname(), false);
				checkDirectory(curDir.getJavaFile());
				if (!foundProblems) {
					OutputUtils.printerrln("All shiny! ^-^", false);
				}
			}

			if (cancelled) {
				OutputUtils.printerrln(CANCEL_STR, false);
				return;
			}

			OutputUtils.printerrln("", false);
			OutputUtils.printerrln("Integrity checks done!", false);
		} else {
			OutputUtils.printerrln("", false);
			OutputUtils.printerrln("Skipping integrity checks, as none are defined!", false);
		}
	}

	private void checkDirectory(java.io.File entryPoint) {

		if (entryPoint.isDirectory()) {
			java.io.File[] children = entryPoint.listFiles();
			if (children != null) {
				for (java.io.File curChild : children) {
					if (curChild.isDirectory()) {
						checkDirectory(curChild);
					} else {
						boolean checkedFile = false;
						boolean curFoundProblems = false;
						String lowpath = curChild.getPath().toLowerCase();
						if (lowpath.endsWith(".mp4") || lowpath.endsWith(".avi") || lowpath.endsWith(".mkv") ||
							lowpath.endsWith(".wmv") || lowpath.endsWith(".mov") || lowpath.endsWith(".mpeg")) {
							checkedFile = true;
							videoAmount++;
							if (!checkIndividualVideoFile(curChild)) {
								curFoundProblems = true;
							}
						}
						// ffprobe seems to not work well for midi files, so we are not testing those
						if (lowpath.endsWith(".mp3") || lowpath.endsWith(".wav") || lowpath.endsWith(".ogg") ||
							lowpath.endsWith(".wma") || lowpath.endsWith(".aac")) {
							checkedFile = true;
							audioAmount++;
							if (!checkIndividualAudioFile(curChild)) {
								curFoundProblems = true;
							}
						}
						// in the future, we could also add other files whose integrity we want to check

						if (curFoundProblems) {
							OutputUtils.printerrln(curChild.getAbsolutePath() + " is broken!", true);
							foundProblems = true;
						}
						if (checkedFile) {
							if ((videoAmount + audioAmount) % 16 == 0) {
								OutputUtils.println("Integrity checking of " + videoAmount +
									" video files and " + audioAmount + " audio files done...");
							}
						}
					}

					if (cancelled) {
						return;
					}
				}
			}
		}
	}

	// returns true if the file is checked successfully, returns false if the file is broken
	private boolean checkIndividualAudioFile(java.io.File audioFile) {
		return checkIndividualVideoFile(audioFile);
	}

	// returns true if the file is checked successfully, returns false if the file is broken
	private boolean checkIndividualVideoFile(java.io.File videoFile) {

		videoFileCheckCommandAndArgs[1] = videoFile.getAbsolutePath();

		try {
			Process process = Runtime.getRuntime().exec(videoFileCheckCommandAndArgs);
			// BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			try {
				int result = process.waitFor();
				/*
				String lastLine = null;
				String nextLine = "";
				while (nextLine != null) {
					lastLine = nextLine;
					nextLine = errorReader.readLine();
				}
				System.out.println("file: " + videoFile + " got result: " + result + " err: " + lastLine);
				*/
				if (result == 0) {
					return true;
				}
			} catch (InterruptedException ie) {
				// well, stop waiting...
			}
		} catch (IOException ioe) {
		}

		return false;
	}
}

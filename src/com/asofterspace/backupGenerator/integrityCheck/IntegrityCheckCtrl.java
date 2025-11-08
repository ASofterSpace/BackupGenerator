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
import java.util.ArrayList;
import java.util.List;


public class IntegrityCheckCtrl {

	private Database database;
	private ConfigFile config;
	private String[] videoFileCheckCommandAndArgs = {null, null};

	private List<File> videoFiles = new ArrayList<>();
	private List<File> audioFiles = new ArrayList<>();

	private volatile boolean paused = false;
	private volatile boolean cancelled = false;

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

		OutputUtils.printerrln("", false);

		List<String> directoryStrs = database.getIntegrityCheckDirs();

		if ((directoryStrs != null) && (directoryStrs.size() > 0)) {
			OutputUtils.printerrln("Integrity checks starting for directories:", false);
			List<Directory> directories = new ArrayList<>();
			for (String dirStr : directoryStrs) {
				Directory curdir = new Directory(dirStr);
				directories.add(curdir);
				OutputUtils.printerrln(curdir.getCanonicalDirname(), false);
			}
			OutputUtils.printerrln("", false);
			OutputUtils.printerrln("Results:", false);

			videoFiles = new ArrayList<>();
			audioFiles = new ArrayList<>();

			OutputUtils.println("Gathering files for integrity checking...");
			if (cancelled) {
				OutputUtils.printerrln(CANCEL_STR, false);
				return;
			}

			gatherFiles(directories);

			int i = 0;
			int j = 0;
			OutputUtils.println("Integrity checking of " + videoFiles.size() + " videos (" +
				i + " done) and " + audioFiles.size() + " audios (" + j + " done)...");

			if (cancelled) {
				OutputUtils.printerrln(CANCEL_STR, false);
				return;
			}

			boolean foundProblems = false;

			for (File videoFile : videoFiles) {
				i++;
				if (i % 64 == 0) {
					OutputUtils.println("Integrity checking of " + videoFiles.size() + " videos (" +
						i + " done) and " + audioFiles.size() + " audios (" + j + " done)...");
				}
				if (!checkIndividualVideoFile(videoFile)) {
					foundProblems = true;
					OutputUtils.printerrln(videoFile.getAbsoluteFilename() + " is broken!", true);
				}
				if (cancelled) {
					OutputUtils.printerrln(CANCEL_STR, false);
					return;
				}
			}
			for (File audioFile : audioFiles) {
				j++;
				if (j % 64 == 0) {
					OutputUtils.println("Integrity checking of " + videoFiles.size() + " videos (" +
						i + " done) and " + audioFiles.size() + " audios (" + j + " done)...");
				}
				if (!checkIndividualAudioFile(audioFile)) {
					foundProblems = true;
					OutputUtils.printerrln(audioFile.getAbsoluteFilename() + " is broken!", true);
				}
				if (cancelled) {
					OutputUtils.printerrln(CANCEL_STR, false);
					return;
				}
			}
			if (!foundProblems) {
				OutputUtils.printerrln("All shiny! ^-^", false);
			}
			OutputUtils.printerrln("", false);
			OutputUtils.printerrln("Integrity checks done!", false);
		} else {
			OutputUtils.printerrln("Skipping integrity checks, as none are defined!", false);
		}
	}

	private void gatherFiles(List<Directory> directories) {
		boolean recursively = true;
		for (Directory curdir : directories) {
			for (File curfile : curdir.getAllFiles(recursively)) {
				String contentType = curfile.getContentType();
				if (contentType.startsWith("video/")) {
					this.videoFiles.add(curfile);
				}
				if (contentType.startsWith("audio/")) {
					this.audioFiles.add(curfile);
				}
				// in the future, we could also add other files whose integrity we want to check
			}
		}
	}

	// returns true if the file is checked successfully, returns false if the file is broken
	private boolean checkIndividualAudioFile(File audioFile) {
		return checkIndividualVideoFile(audioFile);
	}

	// returns true if the file is checked successfully, returns false if the file is broken
	private boolean checkIndividualVideoFile(File videoFile) {

		videoFileCheckCommandAndArgs[1] = videoFile.getAbsoluteFilename();

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

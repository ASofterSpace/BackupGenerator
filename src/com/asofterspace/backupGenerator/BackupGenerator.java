/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.backupGenerator.gui.GUI;
import com.asofterspace.backupGenerator.output.OutputUtils;
import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.Utils;

import javax.swing.SwingUtilities;


public class BackupGenerator {

	public final static String PROGRAM_TITLE = "BackupGenerator";
	public final static String VERSION_NUMBER = "0.0.1.8(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "15. September 2020 - 1. August 2025";

	public final static String USE_STATUS_FILE = "useStatusFile";
	public final static String STATUS_FILE_PATH = "statusFilePath";

	private static ConfigFile config;

	private static volatile boolean guiVisible = false;

	// synchronize with browser
	public static TextFile BACKUP_RUN_FILE;


	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		if (args.length > 0) {
			if (args[0].equals("--version")) {
				OutputUtils.println(Utils.getFullProgramIdentifierWithDate());
				return;
			}

			if (args[0].equals("--version_for_zip")) {
				OutputUtils.println("version " + Utils.getVersionNumber());
				return;
			}
		}

		try {
			// load config
			config = new ConfigFile("settings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON("{}"));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the settings failed:");
			System.err.println(e);
			System.exit(1);
		}

		final Boolean useStatusFile = config.getBoolean(USE_STATUS_FILE, true);

		if (useStatusFile) {
			String statusFilePath = config.getValue(STATUS_FILE_PATH);
			if (statusFilePath == null) {
				statusFilePath = "BACKUP.TXT";
			}
			BACKUP_RUN_FILE = new TextFile(statusFilePath);

			if (BACKUP_RUN_FILE.exists()) {
				GuiUtils.notify("Refusing to start as previous backup run is not yet finished!\n" +
					"(" + BACKUP_RUN_FILE.getCanonicalFilename() + " exists)");
				return;
			}

			OutputUtils.println("Saving status file " + statusFilePath + "...");

			BACKUP_RUN_FILE.saveContent("Started backup run at " + DateUtils.serializeDateTime(DateUtils.now()));
		}

		OutputUtils.println("Loading database...");

		Database database = new Database();

		OutputUtils.println("Saving database...");

		database.save();

		OutputUtils.println("Creating backup ctrl...");

		BackupCtrl backupCtrl = new BackupCtrl(database);

		OutputUtils.println("Starting GUI...");

		SwingUtilities.invokeLater(new GUI(backupCtrl, config));

		while (!guiVisible) {
			Utils.sleep(1000);
		}

		OutputUtils.println("Starting backup ctrl...");

		backupCtrl.start();
	}

	public static void setGuiVisible(boolean guiVisibleArg) {
		guiVisible = guiVisibleArg;
	}

}

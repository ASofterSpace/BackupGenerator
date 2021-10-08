/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.backupGenerator.gui.GUI;
import com.asofterspace.backupGenerator.output.OutputUtils;
import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.Utils;

import javax.swing.SwingUtilities;


public class BackupGenerator {

	public final static String PROGRAM_TITLE = "BackupGenerator";
	public final static String VERSION_NUMBER = "0.0.0.8(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "15. September 2020 - 8. October 2021";

	private static ConfigFile config;

	private static volatile boolean guiVisible = false;


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

		OutputUtils.println("Loading database...");

		Database database = new Database();

		OutputUtils.println("Saving database...");

		database.save();

		OutputUtils.println("Creating backup ctrl...");

		BackupCtrl backupCtrl = new BackupCtrl(database);

		OutputUtils.println("Starting GUI...");

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

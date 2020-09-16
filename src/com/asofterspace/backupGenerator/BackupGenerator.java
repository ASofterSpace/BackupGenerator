/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator;

import com.asofterspace.toolbox.Utils;


/*
TODO:
>> remove all files which no longer exist
>> copy over all files which have different size or different date
>> ensure that also hidden files are copied
>> also copy Downloads folder into the main of that date
>> including movies into movie folder etc.
>> allow picking up dropped copying from last time this ran (without needing to write out anything, just do new list / compare)
>> AFTER all else is done, rename the folder to current day
>> and then - update the meta-info in the /inf/ about the contents of this drive
   (read from data on drive about which one it is, e.g. G:\hdd_14_1.txt)
>> oh, and make this configurable so Bene can also use it :)
*/
public class BackupGenerator {

	public final static String PROGRAM_TITLE = "BackupGenerator";
	public final static String VERSION_NUMBER = "0.0.0.1(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "15. Sep 2020 - 15. Sep 2020";

	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		if (args.length > 0) {
			if (args[0].equals("--version")) {
				System.out.println(Utils.getFullProgramIdentifierWithDate());
				return;
			}

			if (args[0].equals("--version_for_zip")) {
				System.out.println("version " + Utils.getVersionNumber());
				return;
			}
		}

		System.out.println("Loading database...");

		Database database = new Database();

		System.out.println("Saving database...");

		database.save();

		System.out.println("Starting backup ctrl...");

		BackupCtrl backupCtrl = new BackupCtrl(database);

		backupCtrl.start();

		System.out.println("Done! Have a nice day! :)");
	}

}

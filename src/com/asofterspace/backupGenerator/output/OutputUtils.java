/**
 * Unlicensed code created by A Softer Space, 2021
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.output;

import javax.swing.JLabel;


public class OutputUtils {

	private static JLabel outputLabel;

	private static JLabel currentDirectoryLabel;

	private static boolean printDirectories = false;


	public static void println(String line) {
		if (outputLabel == null) {
			System.out.println(line);
		} else {
			outputLabel.setText(line);
		}
	}

	public static void printDir(String line) {
		if (printDirectories && (currentDirectoryLabel != null)) {
			currentDirectoryLabel.setText(line);
		}
	}

	public static void setOutputLabel(JLabel outputLabelArg) {
		outputLabel = outputLabelArg;
	}

	public static void setCurrentDirectoryLabel(JLabel currentDirectoryLabelArg) {
		currentDirectoryLabel = currentDirectoryLabelArg;
	}

	public static boolean getPrintDirectories() {
		return printDirectories;
	}

	public static void setPrintDirectories(boolean printDirectoriesArg) {
		printDirectories = printDirectoriesArg;
	}

}

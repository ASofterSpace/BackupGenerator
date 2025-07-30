/**
 * Unlicensed code created by A Softer Space, 2021
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.output;

import com.asofterspace.toolbox.gui.GuiUtils;

import javax.swing.JLabel;
import javax.swing.JTextArea;


public class OutputUtils {

	private static JLabel outputLabel;

	private static JTextArea errorMemo;

	private static JLabel targetLabel;

	private static JLabel currentDirectoryLabel;

	private static String target;

	private static boolean printDirectories = false;

	private static StringBuilder errorText = new StringBuilder();

	private static boolean debugMode = false;


	public static void println(String line) {
		if (debugMode || (outputLabel == null)) {
			System.out.println(line);
		}
		if (outputLabel != null) {
			outputLabel.setText(line);
		}
	}

	public static void message(String line) {
		if (debugMode || (outputLabel == null)) {
			System.out.println(line);
		}
		if (outputLabel != null) {
			GuiUtils.notify(line);
			outputLabel.setText(line);
		}
	}

	public static void printerrln(String line, boolean isError) {
		if (isError) {
			line = "[ERROR] " + line;
		} else {
			line = "[INFO] " + line;
		}

		if (errorMemo == null) {
			System.err.println(line);
		} else {
			if (debugMode) {
				System.out.println(line);
			}
			errorText.append(line);
			errorText.append("\n");
			errorMemo.setText(errorText.toString());
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

	public static void setErrorMemo(JTextArea errorMemoArg) {
		errorMemo = errorMemoArg;
	}

	public static String getErrorLogContent() {
		if (errorMemo == null) {
			return "";
		}
		return errorMemo.getText();
	}

	public static void setTarget(String targetArg) {
		target = targetArg;
		updateTargetLabel();
	}

	public static void setTargetLabel(JLabel targetLabelArg) {
		targetLabel = targetLabelArg;
		updateTargetLabel();
	}

	private static void updateTargetLabel() {
		if ((targetLabel != null) && (target != null)) {
			targetLabel.setText(target);
		}
	}

	public static boolean getDebugMode() {
		return debugMode;
	}

	public static void setDebugMode(boolean debugModeArg) {
		debugMode = debugModeArg;
	}

}

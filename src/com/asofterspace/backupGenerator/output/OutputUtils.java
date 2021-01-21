/**
 * Unlicensed code created by A Softer Space, 2021
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.output;

import javax.swing.JLabel;


public class OutputUtils {

	private static JLabel outputLabel;


	public static void println(String line) {
		if (outputLabel == null) {
			System.out.println(line);
		} else {
			outputLabel.setText(line);
		}
	}

	public static void setOutputLabel(JLabel outputLabelArg) {
		outputLabel = outputLabelArg;
	}

}

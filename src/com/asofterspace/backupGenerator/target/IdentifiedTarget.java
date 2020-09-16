/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.target;

import com.asofterspace.toolbox.io.Directory;


/**
 * This is a target drive which has actually been found currently connected
 * to this computer and can be used as backup target
 */
public class IdentifiedTarget extends TargetDrive {

	// the directory which contains this target, e.g. E:\
	private Directory targetDir;


	public IdentifiedTarget(TargetDrive other, Directory targetDir) {

		super(other);

		this.targetDir = targetDir;
	}

	@Override
	public String toString() {
		return name + " at " + targetDir.getAbsoluteDirname();
	}

	public Directory getTargetDir() {
		return targetDir;
	}

}

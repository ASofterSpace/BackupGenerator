/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.actions;

import com.asofterspace.toolbox.utils.Record;

import java.util.List;


public class Action {

	// "sync" (write files to destination and delete files from destination which are in no source) or
	// "writeonly" (write files to destination but do not delete anything from destination) are allowed
	private String kind;

	private String destinationName;

	private List<String> sourcePaths;

	// 0 .. do not write a backup at all
	// 1 .. write one backup
	// 2 .. write two backups (the older one is being updated while the newer one is ignored)
	private Integer replicationFactor;


	public Action(Record rec) {

		this.kind = rec.getString("kind");

		this.destinationName = rec.getString("destinationName");

		this.sourcePaths = rec.getArrayAsStringList("sourcePaths");

		this.replicationFactor = rec.getInteger("replicationFactor");
	}

	public Record toRecord() {

		Record result = Record.emptyObject();

		result.set("kind", kind);

		result.set("destinationName", destinationName);

		result.set("sourcePaths", sourcePaths);

		result.set("replicationFactor", replicationFactor);

		return result;
	}

	public String getKind() {
		return kind;
	}

	public String getDestinationName() {
		return destinationName;
	}

	public List<String> getSourcePaths() {
		return sourcePaths;
	}

	public int getReplicationFactor() {
		if (replicationFactor == null) {
			return 1;
		}
		return replicationFactor;
	}

}

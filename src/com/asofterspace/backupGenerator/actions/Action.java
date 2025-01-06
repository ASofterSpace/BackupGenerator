/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.backupGenerator.actions;

import com.asofterspace.toolbox.utils.Record;

import java.util.List;


public class Action {

	private static final String DESTINATION_NAME = "destinationName";
	private static final String INDEX_REMOTE_FILES = "indexRemoteFiles";
	private static final String KIND = "kind";
	private static final String REPLICATION_FACTOR = "replicationFactor";
	private static final String IGNORE = "ignore";
	private static final String SOURCE_PATHS = "sourcePaths";

	// "sync" (write files to destination and delete files from destination which are in no source) or
	// "writeonly" (write files to destination but do not delete anything from destination) are allowed
	private String kind;

	private String destinationName;

	private List<String> ignore;

	private List<String> sourcePaths;

	// 0 .. do not write a backup at all
	// 1 .. write one backup
	// 2 .. write two backups (the older one is being updated while the newer one is ignored)
	private Integer replicationFactor;

	private Boolean indexRemoteFiles;


	public Action(Record rec) {

		this.kind = rec.getString(KIND);

		this.destinationName = rec.getString(DESTINATION_NAME);

		this.ignore = rec.getArrayAsStringList(IGNORE);

		this.sourcePaths = rec.getArrayAsStringList(SOURCE_PATHS);

		this.replicationFactor = rec.getInteger(REPLICATION_FACTOR);

		this.indexRemoteFiles = rec.getBoolean(INDEX_REMOTE_FILES, false);
	}

	public Record toRecord() {

		Record result = Record.emptyObject();

		result.set(KIND, kind);

		result.set(DESTINATION_NAME, destinationName);

		result.set(IGNORE, ignore);

		result.set(SOURCE_PATHS, sourcePaths);

		result.set(REPLICATION_FACTOR, replicationFactor);

		result.set(INDEX_REMOTE_FILES, indexRemoteFiles);

		return result;
	}

	public String getKind() {
		return kind;
	}

	public String getDestinationName() {
		return destinationName;
	}

	public List<String> getIgnore() {
		return ignore;
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

	public boolean getIndexRemoteFiles() {
		if (indexRemoteFiles == null) {
			return false;
		}
		return indexRemoteFiles;
	}

}

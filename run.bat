@echo off

cd /D %~dp0

start "Backupper" javaw -classpath "%~dp0\bin" -Xms16m -Xmx1024m com.asofterspace.backupGenerator.BackupGenerator %*

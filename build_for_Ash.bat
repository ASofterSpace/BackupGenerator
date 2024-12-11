DEL BackupGenerator.zip

SET PATH=C:\home\system\java\jdk1.8.0_91\bin

CALL build.bat

C:\home\system\zip\zip.exe -r BackupGenerator.zip bin server res debug.bat debug.sh run.bat run.sh run_as_admin.bat UNLICENSE

PAUSE

@echo off
setlocal
cd /d %~dp0
set "CONFIG_FILE=application.properties"
java -jar upcam-client-1.0-jar-with-dependencies.jar "%CONFIG_FILE%" "log4j2.xml" %*

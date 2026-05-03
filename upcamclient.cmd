@echo off
setlocal
cd /d %~dp0
set "CONFIG_FILE=application.local.properties"
if not exist "%CONFIG_FILE%" set "CONFIG_FILE=application.properties"
if not exist "%CONFIG_FILE%" if exist "upcamclient.local.properties" set "CONFIG_FILE=upcamclient.local.properties"
if not exist "%CONFIG_FILE%" if exist "upcamclient.properties" set "CONFIG_FILE=upcamclient.properties"
java -jar upcam-client-1.0-jar-with-dependencies.jar "%CONFIG_FILE%" "log4j2.xml" %*

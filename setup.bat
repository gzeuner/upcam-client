@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "DEST_DIR=%USERPROFILE%\upcam"

echo [1/4] Building project (mvn clean package)...
call mvn clean package
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

echo [2/4] Preparing runtime directories in %DEST_DIR%...
mkdir "%DEST_DIR%\images\received" 2>nul
mkdir "%DEST_DIR%\images\sent" 2>nul
mkdir "%DEST_DIR%\images\noise" 2>nul
mkdir "%DEST_DIR%\logs" 2>nul
mkdir "%DEST_DIR%\.state" 2>nul
mkdir "%DEST_DIR%\.lock" 2>nul

echo [3/4] Copying runtime files...
copy /Y ".\target\upcam-client-1.0-jar-with-dependencies.jar" "%DEST_DIR%\" >nul
copy /Y ".\src\main\resources\application.properties" "%DEST_DIR%\" >nul
copy /Y ".\src\main\resources\application.local.properties.example" "%DEST_DIR%\" >nul
copy /Y ".\src\main\resources\upcamclient.properties" "%DEST_DIR%\" >nul
copy /Y ".\src\main\resources\log4j2.xml" "%DEST_DIR%\" >nul
copy /Y ".\upcamclient.cmd" "%DEST_DIR%\" >nul
copy /Y ".\upcamclient.sh" "%DEST_DIR%\" >nul

if not exist "%DEST_DIR%\application.local.properties" (
  copy /Y "%DEST_DIR%\application.local.properties.example" "%DEST_DIR%\application.local.properties" >nul
)

echo [4/4] Done.
echo Runtime folder: %DEST_DIR%
echo Edit %DEST_DIR%\application.local.properties and set camera credentials.

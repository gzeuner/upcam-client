@echo off

:: Verzeichnisse erstellen
mkdir "%userprofile%\upcam\images\received"
mkdir "%userprofile%\upcam\images\sent"
mkdir "%userprofile%\upcam\logs"

:: Maven Build
mvn clean install

:: Warte 3 Sekunden
timeout /t 10

:: Überprüfen, ob die JAR-Datei vorhanden ist
if not exist ".\target\upcam-client-1.0-jar-with-dependencies.jar" (
  echo JAR-Datei wurde nicht gefunden!
  exit /b 1
)

:: Kopieren von upcamclient.cmd
copy path\to\upcamclient.cmd "%userprofile%\upcam\" || echo Kopieren von upcamclient.cmd fehlgeschlagen

:: Kopieren der erzeugten JAR-Datei
copy .\target\upcam-client-1.0-jar-with-dependencies.jar "%userprofile%\upcam\" || echo Kopieren der JAR-Datei fehlgeschlagen

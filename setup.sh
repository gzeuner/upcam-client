#!/bin/bash

# Verzeichnisse erstellen
mkdir -p ~/upcam/images/received
mkdir -p ~/upcam/images/sent
mkdir -p ~/upcam/logs

# Maven Build
mvn clean install

# Warte 3 Sekunden
sleep 10

# Überprüfen, ob die JAR-Datei vorhanden ist
if [ ! -f ./target/upcam-client-1.0-jar-with-dependencies.jar ]; then
  echo "JAR-Datei wurde nicht gefunden!"
  exit 1
fi

# Kopieren von upcamclient.sh
cp path/to/upcamclient.sh ~/upcam/

# Ausführbare Berechtigungen für upcamclient.sh setzen
chmod +x ~/upcam/upcamclient.sh

# Kopieren der erzeugten JAR-Datei
cp ./target/upcam-client-1.0-jar-with-dependencies.jar ~/upcam/ || echo "Kopieren der JAR-Datei fehlgeschlagen"

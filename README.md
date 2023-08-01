### Deutsch

# UpCam Client

UpCam Client ist ein Tool, das Bilder von einer IP-Kamera herunterlädt und bereitstellt. Es kann mit der upCam Tornado HD PRO und anderen kompatiblen Modellen verwendet werden.
Ergänzend: [SnapShotter](https://github.com/gzeuner/SnapShotter) um Bilder per WhatsApp zu empfangen.

## Voraussetzungen

- Java 11 oder höher
- Maven

## Build

Sie können das Projekt mit Maven bauen. Navigieren Sie im Terminal oder der Befehlszeile zu dem Verzeichnis, in dem sich die `pom.xml` befindet, und führen Sie den folgenden Befehl aus:

```bash
mvn clean package
```

Das Build-System erstellt die notwendigen Verzeichnisse, kopiert die erforderlichen Dateien und kompiliert das JAR.

## Installation

Nachdem das Projekt erfolgreich gebaut wurde, finden Sie die notwendigen Dateien im `upcam`-Ordner in Ihrem Home-Verzeichnis.

## Konfiguration

Passen Sie die `upcamclient.properties`-Datei im `upcam`-Ordner an Ihre Bedürfnisse an. Hier können Sie die Kamera-URL, Intervalle und andere spezifische Einstellungen konfigurieren.

## Ausführung

Unter Linux können Sie das mitgelieferte Skript verwenden:

```bash
~/upcam/upcamclient.sh
```

Unter Windows verwenden Sie:

```cmd
%userprofile%\upcam\upcamclient.cmd
```

## Lizenz

[MIT](LICENSE)

# Besuchen Sie

[tiny-tool.de](https://tiny-tool.de/).

# Bilder per WhatsApp empfangen
[SnapShotter](https://github.com/gzeuner/SnapShotter)

### Englisch

# UpCam Client

UpCam Client is a tool that downloads and provides images from an IP camera. It can be used with the upCam Tornado HD PRO and other compatible models.
Supplementary: [SnapShotter](https://github.com/gzeuner/SnapShotter) to receive pictures via WhatsApp.

## Requirements

- Java 11 or higher
- Maven

## Build

You can build the project with Maven. Navigate to the directory containing the `pom.xml` in your terminal or command line, and execute the following command:

```bash
mvn clean package
```

The build system will create the necessary directories, copy the required files, and compile the JAR.

## Installation

Once the project has been successfully built, you will find the necessary files in the `upcam` folder in your home directory.

## Configuration

Modify the `upcamclient.properties` file in the `upcam` folder to suit your needs. Here, you can configure the camera URL, intervals, and other specific settings.

## Execution

On Linux, you can use the provided script:

```bash
~/upcam/upcamclient.sh
```

On Windows, use:

```cmd
%userprofile%\upcam\upcamclient.cmd
```

## License

[MIT](LICENSE)

# Visit  

[tiny-tool.de](https://tiny-tool.de/).

# Receive images via WhatsApp
[SnapShotter](https://github.com/gzeuner/SnapShotter)
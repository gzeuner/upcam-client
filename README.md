# UpCam + SnapShotter

This repository contains two runtimes:

1. `UpCam` (Java): camera ingest and local frame persistence
2. `SnapShotter` (Node.js): motion evaluation and WhatsApp delivery

Supported camera sources in Java:

- `UPCAM`
- `REOLINK`

## Repository Setup

Requirements:

- Java 21+
- Maven
- Node.js (for `SnapShotter`)

Clone including submodule:

```bash
git clone --recurse-submodules git@github.com:gzeuner/upcam-client.git
```

If already cloned:

```bash
git submodule update --init --recursive
```

## Java Runtime (UpCam)

### One-command setup

Linux/macOS:

```bash
./setup.sh
```

Windows:

```cmd
setup.bat
```

The setup scripts build and prepare `${HOME}/upcam` (Windows: `%USERPROFILE%\upcam`) with runtime files.

### Start

Linux:

```bash
~/upcam/upcamclient.sh
```

Windows:

```cmd
%USERPROFILE%\upcam\upcamclient.cmd
```

Single ingest cycle:

```bash
java -jar upcam-client-1.0-jar-with-dependencies.jar --once
```

### Configuration model

- `application.properties`: tracked defaults (safe placeholders only)
- `upcamclient.properties`: tracked legacy-compatible defaults
- `application.local.properties`: local overrides with real credentials/hosts (not tracked)
- `application.local.properties.example`: template for local file creation

Resolution order:

1. `application.local.properties`
2. `application.properties`
3. `upcamclient.local.properties`
4. `upcamclient.properties`

Rule: put real secrets only in `application.local.properties`.

### Minimal local config

UPCAM:

```properties
camera.type=UPCAM
base.url=http://upcam.local
image.daily.root.resource=/sd/${day}
upcam.user.name=admin
upcam.user.pwd=change_me
```

REOLINK:

```properties
camera.type=REOLINK
reolink.host=reolink.local
reolink.httpPort=80
reolink.username=admin
reolink.password=change_me
reolink.snapshotPath=/cgi-bin/api.cgi?cmd=Snap&channel=0&rs={timestamp}
```

## Node Runtime (SnapShotter)

Source lives in submodule `SnapShotter/`.

Start:

```bash
cd SnapShotter
node src/SnapShotter.js
```

Tests:

```bash
cd SnapShotter
npm test
```

Detailed Node documentation:

- `SnapShotter/README.md`

## Deployment Bundle

Build production bundle:

```powershell
./package-prod.ps1
```

Output: `deploy/*.zip` with Java + Node runtime files and launchers.
Not bundled: runtime image content (`images/*`) and analysis/sample scripts.

## Security and Commit Hygiene

Do not commit:

- `application.local.properties`
- runtime folders (`images/`, `logs/`, `.state/`, `.lock/`, `dataset/`, `sent/`)
- Node auth/session folders (for example `SnapShotter/.wwebjs_auth/`)

Optional staged pre-commit scan:

```bash
git diff --cached --name-only
git diff --cached | rg -n --pcre2 "(?i)(password|secret|token|api[_-]?key|authorization|bearer|BEGIN [A-Z ]*PRIVATE KEY|\b(10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[0-1])\.)\b)"
```

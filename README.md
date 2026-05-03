# UpCam Client

UpCam Client downloads snapshots from an IP camera and stores them locally for further processing.
Supported camera sources:

- `UPCAM`
- `REOLINK`

The repository also contains `SnapShotter` for WhatsApp-based forwarding.

## Quick Start

### Requirements

- Java 21+
- Maven
- Node.js (only for `SnapShotter`)

### One-command setup

Linux/macOS:

```bash
./setup.sh
```

Windows:

```cmd
setup.bat
```

The setup scripts:

1. build the project
2. create `%USERPROFILE%\upcam` (or `${HOME}/upcam`)
3. copy runtime files
4. create `application.local.properties` from template (if missing)

### Run

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

## Configuration Files

- `application.properties`: tracked default config (no real secrets, placeholders only)
- `upcamclient.properties`: tracked legacy-compatible config (no real secrets)
- `application.local.properties`: local runtime overrides with real credentials/hosts, not tracked
- `application.local.properties.example`: tracked template to create local config

Practical rule:

1. keep committed defaults in `application.properties` / `upcamclient.properties` generic and safe
2. put real usernames/passwords/host addresses only into `application.local.properties`

Resolution order for auto-start:

1. `application.local.properties`
2. `application.properties`
3. `upcamclient.local.properties`
4. `upcamclient.properties`

## Minimal Config Examples

Put these values into `application.local.properties` on the target host (not into git-tracked files).

### UPCAM

```properties
camera.type=UPCAM
base.url=http://upcam.local
image.daily.root.resource=/sd/${day}
image.html.pattern=a[href*=images]
upcam.user.name=admin
upcam.user.pwd=change_me
```

### REOLINK

```properties
camera.type=REOLINK
reolink.host=reolink.local
reolink.httpPort=80
reolink.username=admin
reolink.password=change_me
reolink.snapshotPath=/cgi-bin/api.cgi?cmd=Snap&channel=0&rs={timestamp}
```

Template variables supported in `reolink.snapshotPath` / `reolink.snapshotUrl`:

- `{host}`
- `{port}`
- `{username}`
- `{password}`
- `{usernameEncoded}`
- `{passwordEncoded}`
- `{timestamp}`

## Deployment Bundle

Use:

```powershell
./package-prod.ps1
```

It creates a deploy zip under `deploy/` with Java + Node runtime files and launchers.
No runtime image content (`images/*`) and no analysis/test sample scripts (for example `analyzeSamples.js`) are bundled.

## Security / Commit Hygiene

Never commit:

- `application.local.properties`
- runtime folders (`images/`, `logs/`, `.state/`, `.lock/`, `dataset/`, `sent/`)
- Node auth/session folders (for example `SnapShotter/.wwebjs_auth/`)

These paths are ignored via `.gitignore`.

### Pre-Commit Check (Staged Files Only)

Use this before commit:

```bash
git diff --cached --name-only
git diff --cached | rg -n --pcre2 "(?i)(password|secret|token|api[_-]?key|authorization|bearer|BEGIN [A-Z ]*PRIVATE KEY|\b(10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[0-1])\.)\b)"
```

## SnapShotter

Node runtime is in:

- `SnapShotter/src/SnapShotter.js`
- `SnapShotter/src/config.js`

Run tests:

```bash
cd SnapShotter
npm test
```

## License

MIT

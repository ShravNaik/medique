# Medique

Medique is a JavaFX desktop medicine reminder with local SQLite storage. It supports prescription schedules, dose reminders, adherence history, caregiver contacts, and optional backend synchronization.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer on `PATH`

If `JAVA_HOME` is set, the build scripts use that JDK.

## Run and test

```bat
run.bat
build.bat test
build.bat clean package
```

The shaded application JAR is created under `target`. To create a Windows app image with the project icon, run from the project root:

```bat
jpackage --type app-image --name Medique --input target --main-jar medique-1.0.0.jar --main-class com.medique.Launcher --dest dist --icon src\main\resources\MediqueIcon.ico --app-version 1.0.0
```

The generated `target` and `dist` folders are build outputs and are not tracked in Git.

## Local data and configuration

The SQLite database is normally stored at `%USERPROFILE%\.medique\medique.db`. Database files are excluded from Git.

Cloud synchronization and SMTP caregiver alerts require real service configuration. The source defaults are placeholders and do not provide a production backend or mail credentials.

## Security note

Accounts must sign in; the app does not auto-login or prefill credentials. Passwords currently use salted SHA-256, which is not a production-grade password-storage algorithm. Do not use this prototype with real patient data until password storage and other security controls have been reviewed and upgraded.
